package yesweburp.api

class APIException(msg: String?) : Exception(msg)

data class Pagination(val nb_pages: Int)
data class Page<T>(val items: List<T>, val pagination: Pagination)

data class BusinessUnit(val currency: String, val name: String? = null)
data class Scope(
    val scope: String,
    val scope_type: String,
    val asset_value: String,
    val scope_type_name: String? = null
)
data class Image(val url: String? = null)
data class ProgramStats(
    val total_reports: Int? = null,
    val total_reports_last24_hours: Int? = null,
    val total_reports_last7_days: Int? = null
)
data class ShortProgram(val title: String, val slug: String, val public: Boolean)
data class RewardGrid(
    val bounty_low: Int? = null,
    val bounty_medium: Int? = null,
    val bounty_high: Int? = null,
    val bounty_critical: Int? = null
)

data class Program(
    val title: String,
    val slug: String,
    val rules_html: String,
    val scopes: List<Scope>,
    val out_of_scope: List<String>,
    val public: Boolean = false,
    val qualifying_vulnerability: List<String> = emptyList(),
    val non_qualifying_vulnerability: List<String> = emptyList(),
    val business_unit: BusinessUnit,
    val vpn_active: Boolean = false,
    val user_agent: String? = null,
    val reward_grid_default: RewardGrid? = null,
    val reward_grid_very_low: RewardGrid? = null,
    val reward_grid_low: RewardGrid? = null,
    val reward_grid_medium: RewardGrid? = null,
    val reward_grid_high: RewardGrid? = null,
    val reward_grid_critical: RewardGrid? = null,
    val account_access_html: String? = null,
    val type: String? = null,
    val bounty: Boolean? = null,
    val bounty_reward_min: Int? = null,
    val bounty_reward_max: Int? = null,
    val reports_count: Int? = null,
    val scopes_count: Int? = null,
    val stats: ProgramStats? = null,
    val last_update_at: String? = null,
    val supported_languages: List<String>? = null,
    val thumbnail: Image? = null,
)

data class Login(val email: String, val password: String)
data class LoginOTP(val token: String, val code: String)
data class LoginResponse(val token: String?, val totp_token: String?)
data class LoginResponseTOTP(val totp_token: String)
data class APIError(val code: Int? = null, val message: String? = null)


enum class AuthMethod(val label: String) {
    ANONYMOUS("Anonymous"),
    EMAIL_PASSWORD("Email / Password"),
    EMAIL_PASSWORD_OTP("Email / Password + OTP"),
    TOKEN("Access token");

    val requirePassword get() = this == EMAIL_PASSWORD || this == EMAIL_PASSWORD_OTP
    val requireEmail get() = requirePassword
    val requireOTP get() = this == EMAIL_PASSWORD_OTP
    val requireToken get() = this == TOKEN

    companion object {
        fun labels() = values().map { it.label }.toTypedArray()

        private val map = values().associateBy(AuthMethod::label)
        fun fromValue(type: String) = map[type] ?: throw RuntimeException("Unreachable")
    }
}