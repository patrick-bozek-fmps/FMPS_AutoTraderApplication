package com.fmps.autotrader.desktop.i18n

import javafx.beans.property.SimpleObjectProperty
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle

object Localization {
    private val bundleProperty = SimpleObjectProperty(loadBundle(Locale.getDefault()))

    fun string(key: String, defaultValue: String = key): String =
        try {
            bundleProperty.get().getString(key)
        } catch (ex: MissingResourceException) {
            defaultValue
        }

    fun setLocale(locale: Locale) {
        bundleProperty.set(loadBundle(locale))
    }

    private fun loadBundle(locale: Locale): ResourceBundle =
        runCatching { ResourceBundle.getBundle("i18n.messages", locale) }
            .getOrElse { ResourceBundle.getBundle("i18n.messages", Locale.ENGLISH) }
}


