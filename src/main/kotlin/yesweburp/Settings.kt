package yesweburp

import yesweburp.api.AuthMethod
import kotlin.reflect.KProperty


class Persistent<T>(private val key: String, private val default: T, private val parse: (String) -> T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val saved = callbacks.loadExtensionSetting(key)
        return if (saved.isNullOrEmpty()) default else parse(saved)
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        callbacks.saveExtensionSetting(key, value.toString())
    }
}

object Settings {
    var userEmail by Persistent("USER_EMAIL", "") { it }
    var userPassword by Persistent("USER_PASSWORD", "") { it }
    var rememberPassword by Persistent("REMEMBER_PASSWORD", false) { it == "true" }
    var authMethod by Persistent("AUTH_METHOD", AuthMethod.ANONYMOUS.label) { it }
    var favoriteTab by Persistent("FAVORITE_TAB", 0) { it.toInt() }
}
