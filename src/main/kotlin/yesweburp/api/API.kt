package yesweburp.api


import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import yesweburp.Events.programsLoaded
import yesweburp.VERSION
import yesweburp.callbacks
import yesweburp.helpers
import yesweburp.stderr
import java.net.URL
import java.util.stream.Collectors
import javax.swing.SwingUtilities


object API {
    private var token: String? = null
    private val jsonMapper = with(jacksonObjectMapper()) {
        configure(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false
        )
    }
    private const val host = "api.yeswehack.com"
    private const val port = 443

    private fun sendRequest(request: ByteArray): ByteArray {
        val service = helpers.buildHttpService(host, port, port == 443)
        val response = callbacks.makeHttpRequest(service, request).response
        val responseInfo = helpers.analyzeResponse(response)
        val responseBody = response.slice(responseInfo.bodyOffset until response.size).toByteArray()
        if (responseInfo.statusCode.toInt() != 200) {
            val isJson = responseInfo.headers.any { it.lowercase().startsWith("content-type: application/json") }
            val message = if (isJson) runCatching { jsonMapper.readValue<APIError>(responseBody).message }.getOrNull() else null
            throw APIException(message ?: "API Error ${responseInfo.statusCode}")
        }
        return responseBody
    }

    private fun request(method: String, path: String, body: ByteArray? = null): ByteArray {
        val headers = listOfNotNull(
            "$method $path HTTP/1.1",
            "Host: $host",
            body?.let { "Content-Type: application/json" },
            "User-Agent: YesWeBurp $VERSION",
            token?.let { "Authorization: Bearer $it" }
        )
        return sendRequest(helpers.buildHttpMessage(headers, body ?: byteArrayOf()))
    }

    private inline fun <reified T> get(path: String): T = jsonMapper.readValue(request("GET", path))

    private inline fun <reified TResp> post(path: String, form: Any): TResp =
        jsonMapper.readValue(request("POST", path, jsonMapper.writeValueAsBytes(form)))

    fun auth() {
        token = null
    }

    fun auth(accessToken: String) {
        token = null
        val t = accessToken.trim().removePrefix("Bearer ").trim()
        if (t.isEmpty() || t.any { it.isWhitespace() || it.isISOControl() }) {
            throw APIException("Invalid access token")
        }
        token = t
    }

    fun auth(email: String, password: String) {
        token = null
        val response: LoginResponse = post("/login", Login(email, password))
        if (response.totp_token is String) {
            throw APIException("OTP Required")
        }
        token = response.token
    }

    fun auth(email: String, password: String, otp: String) {
        token = null
        val otpResponse: LoginResponseTOTP = post("/login", Login(email, password))
        val response: LoginResponse = post("/account/totp", LoginOTP(otpResponse.totp_token, otp))

        token = response.token
    }

    fun fetchPrograms() {
        val data = mutableListOf<Program>()
        var page = 1
        do {
            val response: Page<ShortProgram> = get("/programs?page=$page")
            data += response.items.parallelStream()
                .map { fetchProgram(it) }
                .collect(Collectors.toList())
                .filterNotNull()
        } while (page++ < response.pagination.nb_pages)
        val programs = data.sortedBy(Program::title)
        SwingUtilities.invokeLater { programsLoaded.fire(programs) }
    }

    private fun fetchProgram(short: ShortProgram): Program? = try {
        get<Program>("/programs/${short.slug}").copy(public = short.public)
    } catch (e: Exception) {
        stderr.println("YesWeBurp: skipped program ${short.slug}: ${e.message}")
        null
    }

    fun fetchImage(url: String): ByteArray? {
        val u = URL(url)
        val service = helpers.buildHttpService(u.host, if (u.port == -1) u.defaultPort else u.port, u.protocol == "https")
        val response = callbacks.makeHttpRequest(service, helpers.buildHttpRequest(u)).response ?: return null
        val info = helpers.analyzeResponse(response)
        if (info.statusCode.toInt() != 200) return null
        return response.copyOfRange(info.bodyOffset, response.size)
    }
}
