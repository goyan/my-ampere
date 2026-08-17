package dev.frx.myampere.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/** DataStore unique de l'app — un seul delegate pour ce nom de fichier, partagé
 *  entre le widget (dernière valeur) et le service (toggle utilisateur persisté). */
val Context.appStore by preferencesDataStore(name = "widget")

object Prefs {
    private val KEY_LAST_MA = intPreferencesKey("last_ma")
    private val KEY_USER_ENABLED = booleanPreferencesKey("user_enabled")

    suspend fun lastMa(context: Context): Int? = context.appStore.data.first()[KEY_LAST_MA]
    suspend fun setLastMa(context: Context, ma: Int) { context.appStore.edit { it[KEY_LAST_MA] = ma } }

    suspend fun userEnabled(context: Context): Boolean =
        context.appStore.data.first()[KEY_USER_ENABLED] ?: true
    suspend fun setUserEnabled(context: Context, enabled: Boolean) {
        context.appStore.edit { it[KEY_USER_ENABLED] = enabled }
    }
}
