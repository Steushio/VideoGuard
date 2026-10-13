package com.videoguard.app.data

import android.content.Context
import android.content.SharedPreferences
import com.videoguard.app.model.UserAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

object AccountRepository {

    private const val PREFS_NAME = "videoguard_account_prefs"
    private const val KEY_ACCOUNT = "current_account_json"
    private var prefs: SharedPreferences? = null

    private val _currentAccount = MutableStateFlow<UserAccount?>(null)
    val currentAccount: StateFlow<UserAccount?> = _currentAccount.asStateFlow()

    private val _isIncognito = MutableStateFlow(false)
    val isIncognito: StateFlow<Boolean> = _isIncognito.asStateFlow()

    val isSignedIn: Boolean
        get() = _currentAccount.value != null

    fun getDeviceAccounts(context: Context): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        try {
            val am = android.accounts.AccountManager.get(context)
            val accounts = am.getAccountsByType("com.google")
            for (acc in accounts) {
                val email = acc.name
                if (email.isNotBlank()) {
                    val name = formatAccountName(email)
                    results.add(Pair(name, email))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }

    fun formatAccountName(email: String): String {
        val prefix = email.substringBefore("@")
        return prefix.replace(".", " ").replace("_", " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    fun init(context: Context) {
        if (prefs != null) return
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp

        val jsonStr = sp.getString(KEY_ACCOUNT, null)

        if (!jsonStr.isNullOrBlank()) {
            try {
                val obj = JSONObject(jsonStr)
                val savedName = obj.optString("name", "")
                val savedEmail = obj.optString("email", "")
                val savedHandle = obj.optString("handle", "@user")
                val savedAvatar = if (obj.isNull("avatarUrl")) null else obj.optString("avatarUrl").takeIf { it.isNotBlank() }

                if (savedName.isNotBlank() && savedName != "YouTube User" && savedName != "Google Account") {
                    _currentAccount.value = UserAccount(
                        id = obj.optString("id", "google_user"),
                        name = savedName,
                        email = savedEmail,
                        handle = savedHandle,
                        initial = obj.optString("initial", savedName.take(1).uppercase()),
                        avatarUrl = savedAvatar,
                        avatarBgColorHex = obj.optLong("avatarBgColorHex", 0xFF1976D2L),
                        isCurrent = true,
                        isGoogleAuthenticated = obj.optBoolean("isGoogleAuthenticated", true)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // If authenticated YouTube cookies exist, sync the real channel profile, watch history, and playlists
        if (YouTubeSyncEngine.hasAuthCookies()) {
            if (_currentAccount.value == null) {
                // Provisional account while background sync completes
                _currentAccount.value = UserAccount(
                    id = "google_user",
                    name = "YouTube Account",
                    email = "",
                    handle = "@user",
                    initial = "Y",
                    avatarUrl = null,
                    avatarBgColorHex = 0xFF1976D2L,
                    isCurrent = true,
                    isGoogleAuthenticated = true
                )
            }
            CoroutineScope(Dispatchers.IO).launch {
                YouTubeSyncEngine.syncAllUserData()
            }
        }
    }

    fun setGoogleAccount(name: String, email: String, avatarUrl: String?, handle: String) {
        val initialLetter = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "Y"
        _isIncognito.value = false
        val account = UserAccount(
            id = "google_user",
            name = name.ifBlank { "YouTube Account" },
            email = email,
            handle = if (handle.startsWith("@")) handle else "@$handle",
            initial = initialLetter,
            avatarUrl = avatarUrl,
            avatarBgColorHex = 0xFF1976D2L,
            isCurrent = true,
            isGoogleAuthenticated = true
        )
        _currentAccount.value = account
        saveAccount(account)
    }

    fun signOut() {
        _isIncognito.value = false
        _currentAccount.value = null
        prefs?.edit()?.remove(KEY_ACCOUNT)?.apply()
        UserDataRepository.clearHistory()
        try {
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.CookieManager.getInstance().flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleIncognito() {
        _isIncognito.value = !_isIncognito.value
    }

    private fun saveAccount(account: UserAccount) {
        val sp = prefs ?: return
        try {
            val obj = JSONObject().apply {
                put("id", account.id)
                put("name", account.name)
                put("email", account.email)
                put("handle", account.handle)
                put("initial", account.initial)
                put("avatarUrl", account.avatarUrl)
                put("avatarBgColorHex", account.avatarBgColorHex)
                put("isGoogleAuthenticated", account.isGoogleAuthenticated)
            }
            sp.edit().putString(KEY_ACCOUNT, obj.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
