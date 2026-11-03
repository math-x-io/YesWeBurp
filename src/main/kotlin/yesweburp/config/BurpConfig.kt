package yesweburp.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import yesweburp.callbacks

data class BurpScopeRule(
    var enabled: Boolean = true,
    var protocol: String = "any",
    var host: String = "",
    var port: String = "",
    var file: String = ""
)

data class BurpReplaceRule(
    var is_simple_match: Boolean = false,
    var enabled: Boolean = true,
    var rule_type: String = "",
    var string_match: String = "",
    var string_replace: String ="",
    var comment: String = ""
)

data class BurpScopeConfig(
    var advanced_mode: Boolean,
    var include: List<BurpScopeRule>,
    var exclude: List<BurpScopeRule> = emptyList()
)

data class BurpProxyConfig(var match_replace_rules: List<BurpReplaceRule>)

data class BurpTargetConfig(val scope: BurpScopeConfig)

data class BurpConfigResponse(
    var target: BurpTargetConfig? = null,
    var proxy: BurpProxyConfig? = null
)


object BurpConfig {
    const val RULE_COMMENT_SUFFIX = " - YesWeBurp"

    val mapper: ObjectMapper = with(jacksonObjectMapper()) {
        configure(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false
        )
    }

    private inline fun <reified  T> getConfigAsJson(path: String): T{
        return mapper.readValue(callbacks.saveConfigAsJson(path))
    }
    private inline fun <reified  T> setConfigAsJson(config: T){
        callbacks.loadConfigFromJson(mapper.writeValueAsString(config))
    }

    private fun updateTargetScope(update: BurpScopeConfig.() -> Unit){
        val config: BurpConfigResponse = getConfigAsJson("target.scope")
        config.target!!.scope.advanced_mode = true
        config.target!!.scope.update()
        setConfigAsJson(config)
    }

    fun addTargetScope(scopes: List<BurpScopeRule>) = updateTargetScope { include = (include + scopes).distinct() }

    fun addTargetExclude(scopes: List<BurpScopeRule>) = updateTargetScope { exclude = (exclude + scopes).distinct() }

    fun addMatchReplace(rule: BurpReplaceRule){
        val config: BurpConfigResponse = getConfigAsJson("proxy.match_replace_rules")
        if (!config.proxy!!.match_replace_rules.contains(rule)){
            config.proxy!!.match_replace_rules += arrayOf(rule)
        }
        setConfigAsJson(config)
    }

    fun yesWeBurpHeaderRules(): List<BurpReplaceRule> {
        val config: BurpConfigResponse = getConfigAsJson("proxy.match_replace_rules")
        return config.proxy!!.match_replace_rules.filter {
            it.enabled && it.rule_type == "request_header" && it.comment.endsWith(RULE_COMMENT_SUFFIX)
        }
    }
}