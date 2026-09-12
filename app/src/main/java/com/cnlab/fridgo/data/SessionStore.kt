package com.cnlab.fridgo.data

import android.content.Context

/**
 * The signed-in user for the in-app store. Persisted to SharedPreferences so the
 * session survives app restarts (unlike the device-local [CartStore]). Reads are
 * synchronous; call [load] once at startup before reading.
 */
object SessionStore {

    private const val PREFS = "fridgo_session"

    private var user: AuthUser? = null

    /** Restore any saved session. Safe to call once at app startup. */
    fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = p.getInt("id", 0)
        if (id == 0) { user = null; return }
        user = AuthUser(
            id = id,
            username = p.getString("username", "").orEmpty(),
            email = p.getString("email", "").orEmpty(),
            firstName = p.getString("firstName", "").orEmpty(),
            lastName = p.getString("lastName", "").orEmpty(),
            image = p.getString("image", null),
            accessToken = p.getString("token", null)
        )
    }

    fun isLoggedIn(): Boolean = user != null

    fun current(): AuthUser? = user

    /** Persist a freshly authenticated user. */
    fun save(context: Context, authUser: AuthUser) {
        user = authUser
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            putInt("id", authUser.id)
            putString("username", authUser.username)
            putString("email", authUser.email)
            putString("firstName", authUser.firstName)
            putString("lastName", authUser.lastName)
            putString("image", authUser.image)
            putString("token", authUser.accessToken)
            apply()
        }
    }

    /** Sign out and forget the saved session. */
    fun clear(context: Context) {
        user = null
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
