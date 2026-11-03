package yesweburp.config

import burp.IBurpExtenderCallbacks
import burp.IHttpListener
import burp.IHttpRequestResponse
import yesweburp.helpers
import yesweburp.stderr

object UserAgentListener : IHttpListener {
    private const val RULES_TTL_MS = 5000
    @Volatile
    private var rules: List<Pair<Regex, String>> = emptyList()
    @Volatile
    private var loadedAt = 0L

    private fun currentRules(): List<Pair<Regex, String>> {
        val now = System.currentTimeMillis()
        if (now - loadedAt > RULES_TTL_MS) {
            loadedAt = now
            rules = try {
                BurpConfig.yesWeBurpHeaderRules().map { Regex(it.string_match) to it.string_replace }
            } catch (e: Exception) {
                stderr.println("YesWeBurp: cannot load match/replace rules: ${e.message}")
                rules
            }
        }
        return rules
    }

    fun applyRules(headers: List<String>, rules: List<Pair<Regex, String>>): List<String> =
        headers.mapIndexed { i, line ->
            if (i == 0) return@mapIndexed line
            rules.fold(line) { acc, (regex, replace) ->
                val appended = replace.substringAfterLast("$1", "")
                if (appended.isNotEmpty() && acc.endsWith(appended)) acc else regex.replace(acc, replace)
            }
        }

    override fun processHttpMessage(toolFlag: Int, messageIsRequest: Boolean, messageInfo: IHttpRequestResponse) {
        if (!messageIsRequest || toolFlag == IBurpExtenderCallbacks.TOOL_PROXY) return
        val rules = currentRules()
        if (rules.isEmpty()) return

        val request = messageInfo.request
        val info = helpers.analyzeRequest(request)
        val headers = applyRules(info.headers, rules)
        if (headers != info.headers) {
            messageInfo.request = helpers.buildHttpMessage(headers, request.copyOfRange(info.bodyOffset, request.size))
        }
    }
}
