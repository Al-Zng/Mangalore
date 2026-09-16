package com.mangalore.app

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object AuthStore {
    private const val BASE = "https://ifczsjsqazlnogmyomsk.supabase.co"
    private const val KEY = "sb_publishable_HA8TWsQQG8IjQHko1JVwJA_IHFgUAk-"
    private const val PREFS = "mangalore_auth"
    private const val ACCESS = "access_token"
    private const val REFRESH = "refresh_token"
    private const val USER_ID = "user_id"
    private const val NAME = "display_name"
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient()

    var accessToken: String = ""
        private set
    var userId: String = ""
        private set
    var displayName: String = ""
        private set

    fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        accessToken = p.getString(ACCESS, "").orEmpty()
        userId = p.getString(USER_ID, "").orEmpty()
        displayName = p.getString(NAME, "").orEmpty()
    }

    fun hasSession() = accessToken.isNotBlank() && userId.isNotBlank()
    fun googleAuthUrl(): String = BASE + "/auth/v1/authorize?provider=google&redirect_to=mangalore://auth/callback"
    suspend fun completeGoogle(context: Context, uri: Uri): Result<Unit> = runCatching {
        val fragment = uri.fragment.orEmpty().removePrefix("#")
        val params = fragment.split("&").mapNotNull { part ->
            val bits = part.split("=", limit = 2)
            if (bits.size == 2) java.net.URLDecoder.decode(bits[0], "UTF-8") to java.net.URLDecoder.decode(bits[1], "UTF-8") else null
        }.toMap()
        val token = params["access_token"].orEmpty()
        if (token.isBlank()) error("لم يكتمل تسجيل Google")
        accessToken = token
        val refresh = params["refresh_token"].orEmpty()
        val user = request("/auth/v1/user", "GET", null, token)
        userId = user.optString("id")
        displayName = user.optJSONObject("user_metadata")?.optString("full_name")
            ?.ifBlank { user.optJSONObject("user_metadata")?.optString("name") }
            .orEmpty()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(ACCESS, accessToken).putString(REFRESH, refresh)
            .putString(USER_ID, userId).putString(NAME, displayName).apply()
        fetchProfile(context)
    }

    private fun save(context: Context, obj: JSONObject) {
        accessToken = obj.optString("access_token")
        userId = obj.optJSONObject("user")?.optString("id").orEmpty()
        if (userId.isBlank()) userId = obj.optString("user_id")
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(ACCESS, accessToken).putString(REFRESH, obj.optString("refresh_token"))
            .putString(USER_ID, userId).putString(NAME, displayName).apply()
    }

    private suspend fun request(path: String, method: String, body: JSONObject?, token: String = ""): JSONObject = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(BASE + path)
            .header("apikey", KEY)
            .header("Accept", if (path.startsWith("/rest/v1/profiles")) "application/vnd.pgrst.object+json" else "application/json")
        if (token.isNotBlank()) builder.header("Authorization", "Bearer $token")
        if (body != null) builder.method(method, body.toString().toRequestBody(jsonType)) else builder.method(method, null)
        http.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IllegalStateException(JSONObject(text).optString("msg").ifBlank { JSONObject(text).optString("message") }.ifBlank { "تعذر إكمال الطلب" })
            JSONObject(if (text.isBlank()) "{}" else text)
        }
    }

    suspend fun signIn(context: Context, email: String, password: String): Result<Unit> = runCatching {
        val obj = request("/auth/v1/token?grant_type=password", "POST", JSONObject().put("email", email).put("password", password))
        save(context, obj)
        fetchProfile(context)
    }

    suspend fun signUp(context: Context, email: String, password: String, name: String): Result<Unit> = runCatching {
        displayName = name
        val obj = request("/auth/v1/signup", "POST", JSONObject().put("email", email).put("password", password).put("data", JSONObject().put("display_name", name)))
        if (obj.optString("access_token").isBlank()) throw IllegalStateException("تم إنشاء الحساب. افحص بريدك لتأكيد الحساب ثم سجل الدخول")
        save(context, obj)
        fetchProfile(context)
    }

    private suspend fun fetchProfile(context: Context) {
        val obj = request("/rest/v1/profiles?id=eq.$userId&select=display_name,username", "GET", null, accessToken)
        displayName = obj.optString("display_name", displayName)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(NAME, displayName).apply()
    }

    suspend fun updateDisplayName(context: Context, name: String) {
        require(name.isNotBlank()) { "الاسم لا يمكن أن يكون فارغاً" }
        request(
            "/rest/v1/profiles?id=eq.$userId",
            "PATCH",
            JSONObject().put("display_name", name.trim()),
            accessToken
        )
        displayName = name.trim()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(NAME, displayName).apply()
    }

    fun signOut(context: Context) {
        accessToken = ""; userId = ""; displayName = ""
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
