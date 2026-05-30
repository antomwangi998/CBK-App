package com.helasacco.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.helasacco.app.domain.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hela_session")

data class SessionData(
    val userId: String,
    val username: String,
    val fullName: String,
    val role: UserRole,
    val branchId: String?,
    val memberId: String?,
    val sessionToken: String,
)

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val FULL_NAME = stringPreferencesKey("full_name")
        val ROLE = stringPreferencesKey("role")
        val BRANCH_ID = stringPreferencesKey("branch_id")
        val MEMBER_ID = stringPreferencesKey("member_id")
        val SESSION_TOKEN = stringPreferencesKey("session_token")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val LAST_ACTIVE = longPreferencesKey("last_active")
        val THEME = stringPreferencesKey("theme")      // "light" | "dark" | "system"
    }

    val session: Flow<SessionData?> = context.dataStore.data.map { prefs ->
        val userId = prefs[Keys.USER_ID] ?: return@map null
        val token = prefs[Keys.SESSION_TOKEN] ?: return@map null
        SessionData(
            userId = userId,
            username = prefs[Keys.USERNAME] ?: "",
            fullName = prefs[Keys.FULL_NAME] ?: "",
            role = UserRole.from(prefs[Keys.ROLE] ?: "member"),
            branchId = prefs[Keys.BRANCH_ID],
            memberId = prefs[Keys.MEMBER_ID],
            sessionToken = token,
        )
    }

    val isLoggedIn: Flow<Boolean> = session.map { it != null }

    val theme: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME] ?: "system"
    }

    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.BIOMETRIC_ENABLED] ?: false
    }

    suspend fun saveSession(data: SessionData) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = data.userId
            prefs[Keys.USERNAME] = data.username
            prefs[Keys.FULL_NAME] = data.fullName
            prefs[Keys.ROLE] = data.role.value
            data.branchId?.let { prefs[Keys.BRANCH_ID] = it }
            data.memberId?.let { prefs[Keys.MEMBER_ID] = it }
            prefs[Keys.SESSION_TOKEN] = data.sessionToken
            prefs[Keys.LAST_ACTIVE] = System.currentTimeMillis()
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USERNAME)
            prefs.remove(Keys.FULL_NAME)
            prefs.remove(Keys.ROLE)
            prefs.remove(Keys.BRANCH_ID)
            prefs.remove(Keys.MEMBER_ID)
            prefs.remove(Keys.SESSION_TOKEN)
        }
    }

    suspend fun savePinHash(hash: String) {
        context.dataStore.edit { it[Keys.PIN_HASH] = hash }
    }

    suspend fun setBiometric(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[Keys.THEME] = theme }
    }

    suspend fun updateLastActive() {
        context.dataStore.edit { it[Keys.LAST_ACTIVE] = System.currentTimeMillis() }
    }
}
