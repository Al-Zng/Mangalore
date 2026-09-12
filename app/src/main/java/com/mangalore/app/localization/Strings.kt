package com.mangalore.app.localization

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.LayoutDirection

enum class AppLanguage(val code: String) {
    AR("ar"), EN("en");

    val displayName: String get() = if (this == AR) "العربية" else "English"
    val layoutDirection: LayoutDirection get() = if (this == AR) LayoutDirection.Rtl else LayoutDirection.Ltr
}

object LocalizationManager {
    private lateinit var prefs: android.content.SharedPreferences

    var language: AppLanguage by mutableStateOf(AppLanguage.EN)
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences("mangalore_locale", Context.MODE_PRIVATE)
        val saved = prefs.getString("app_language", null)
        language = saved?.let { code -> AppLanguage.values().firstOrNull { it.code == code } } ?: AppLanguage.EN
    }

    fun setLanguage(lang: AppLanguage) {
        language = lang
        if (::prefs.isInitialized) prefs.edit().putString("app_language", lang.code).apply()
    }

    fun string(key: String): String = Strings.table[key]?.get(language) ?: key
}

/** Shorthand: L("home.title") */
fun L(key: String): String = LocalizationManager.string(key)

object Strings {
    val table: Map<String, Map<AppLanguage, String>> = mapOf(
        "onboarding.chooseLanguage" to mapOf(AppLanguage.EN to "Choose your language", AppLanguage.AR to "اختر لغتك"),
        "onboarding.continue" to mapOf(AppLanguage.EN to "Continue", AppLanguage.AR to "متابعة"),
        "auth.welcome" to mapOf(AppLanguage.EN to "Welcome to Mangalore", AppLanguage.AR to "مرحبًا بك في Mangalore"),
        "auth.subtitle" to mapOf(AppLanguage.EN to "Sign in to sync your library across devices", AppLanguage.AR to "سجّل الدخول لمزامنة مكتبتك عبر أجهزتك"),
        "auth.email" to mapOf(AppLanguage.EN to "Email", AppLanguage.AR to "البريد الإلكتروني"),
        "auth.password" to mapOf(AppLanguage.EN to "Password", AppLanguage.AR to "كلمة المرور"),
        "auth.username" to mapOf(AppLanguage.EN to "Username", AppLanguage.AR to "اسم المستخدم"),
        "auth.login" to mapOf(AppLanguage.EN to "Log In", AppLanguage.AR to "تسجيل الدخول"),
        "auth.signup" to mapOf(AppLanguage.EN to "Create Account", AppLanguage.AR to "إنشاء حساب"),
        "auth.guest" to mapOf(AppLanguage.EN to "Continue as Guest", AppLanguage.AR to "الدخول كضيف"),
        "auth.switchToSignup" to mapOf(AppLanguage.EN to "Don't have an account? Sign up", AppLanguage.AR to "ليس لديك حساب؟ أنشئ حسابًا"),
        "auth.switchToLogin" to mapOf(AppLanguage.EN to "Already have an account? Log in", AppLanguage.AR to "لديك حساب بالفعل؟ سجّل الدخول"),

        "nav.home" to mapOf(AppLanguage.EN to "Home", AppLanguage.AR to "الرئيسية"),
        "nav.search" to mapOf(AppLanguage.EN to "Search for Manga", AppLanguage.AR to "البحث عن مانجا"),
        "nav.newReleases" to mapOf(AppLanguage.EN to "New Releases", AppLanguage.AR to "أعمال جديدة"),
        "nav.library" to mapOf(AppLanguage.EN to "My Library", AppLanguage.AR to "مكتبتي"),
        "nav.downloads" to mapOf(AppLanguage.EN to "Downloads", AppLanguage.AR to "التحميلات"),
        "nav.history" to mapOf(AppLanguage.EN to "Watch History", AppLanguage.AR to "سجل المشاهدة"),
        "nav.profile" to mapOf(AppLanguage.EN to "Profile", AppLanguage.AR to "الملف الشخصي"),
        "nav.messages" to mapOf(AppLanguage.EN to "Messages", AppLanguage.AR to "الرسائل"),
        "nav.faq" to mapOf(AppLanguage.EN to "FAQ", AppLanguage.AR to "الأسئلة الشائعة"),
        "nav.privacy" to mapOf(AppLanguage.EN to "Privacy Policy", AppLanguage.AR to "سياسة الخصوصية"),
        "nav.terms" to mapOf(AppLanguage.EN to "Terms of Use", AppLanguage.AR to "شروط الاستخدام"),
        "nav.settings" to mapOf(AppLanguage.EN to "Settings", AppLanguage.AR to "الإعدادات"),
        "nav.signOut" to mapOf(AppLanguage.EN to "Sign Out", AppLanguage.AR to "تسجيل الخروج"),

        "detail.chapters" to mapOf(AppLanguage.EN to "Chapters", AppLanguage.AR to "الفصول"),
        "detail.ratingsAndComments" to mapOf(AppLanguage.EN to "Ratings & Comments", AppLanguage.AR to "التقييم والتعليقات"),
        "detail.writeComment" to mapOf(AppLanguage.EN to "Write a comment...", AppLanguage.AR to "اكتب تعليقًا..."),
        "detail.post" to mapOf(AppLanguage.EN to "Post", AppLanguage.AR to "نشر"),
        "detail.send" to mapOf(AppLanguage.EN to "Send to a friend", AppLanguage.AR to "إرسال إلى صديق"),
        "detail.synopsis" to mapOf(AppLanguage.EN to "Synopsis", AppLanguage.AR to "القصة"),
        "detail.startReading" to mapOf(AppLanguage.EN to "Start Reading", AppLanguage.AR to "ابدأ القراءة"),
        "detail.noChapters" to mapOf(AppLanguage.EN to "No chapters available", AppLanguage.AR to "لا توجد فصول متاحة"),
        "detail.noComments" to mapOf(AppLanguage.EN to "No comments yet — be the first!", AppLanguage.AR to "لا توجد تعليقات بعد — كن أول من يعلّق!"),

        "profile.chaptersRead" to mapOf(AppLanguage.EN to "Chapters Read", AppLanguage.AR to "الفصول المقروءة"),
        "profile.favorites" to mapOf(AppLanguage.EN to "Favorites", AppLanguage.AR to "المفضلة"),
        "profile.memberSince" to mapOf(AppLanguage.EN to "Member since", AppLanguage.AR to "عضو منذ"),
        "profile.admin" to mapOf(AppLanguage.EN to "Admin", AppLanguage.AR to "مشرف"),

        "messages.newMessage" to mapOf(AppLanguage.EN to "New Message", AppLanguage.AR to "رسالة جديدة"),
        "messages.typeMessage" to mapOf(AppLanguage.EN to "Type a message...", AppLanguage.AR to "اكتب رسالة..."),
        "messages.searchUser" to mapOf(AppLanguage.EN to "Search by username", AppLanguage.AR to "ابحث باسم المستخدم"),
        "messages.open" to mapOf(AppLanguage.EN to "Open", AppLanguage.AR to "فتح"),

        "home.downloads" to mapOf(AppLanguage.EN to "Downloads", AppLanguage.AR to "التحميلات"),
        "home.mostViewed" to mapOf(AppLanguage.EN to "Most Viewed", AppLanguage.AR to "الأكثر مشاهدة"),
        "home.latestUpdates" to mapOf(AppLanguage.EN to "Latest Updates", AppLanguage.AR to "آخر التحديثات"),
        "home.continueReading" to mapOf(AppLanguage.EN to "Continue Reading", AppLanguage.AR to "متابعة القراءة"),
        "home.noInternet" to mapOf(AppLanguage.EN to "No Internet Connection", AppLanguage.AR to "لا يوجد اتصال بالإنترنت"),
        "home.noDownloads" to mapOf(AppLanguage.EN to "No downloads yet", AppLanguage.AR to "لا توجد تحميلات بعد"),

        "library.title" to mapOf(AppLanguage.EN to "My Library", AppLanguage.AR to "مكتبتي"),
        "library.empty" to mapOf(AppLanguage.EN to "Nothing here yet", AppLanguage.AR to "لا يوجد شيء هنا بعد"),

        "search.placeholder" to mapOf(AppLanguage.EN to "Search for manga...", AppLanguage.AR to "ابحث عن مانجا..."),
        "search.noResults" to mapOf(AppLanguage.EN to "No results found", AppLanguage.AR to "لا توجد نتائج"),

        "downloads.title" to mapOf(AppLanguage.EN to "Downloads", AppLanguage.AR to "التحميلات"),
        "downloads.empty" to mapOf(AppLanguage.EN to "No downloads yet", AppLanguage.AR to "لا توجد تحميلات بعد"),
        "history.title" to mapOf(AppLanguage.EN to "Watch History", AppLanguage.AR to "سجل المشاهدة"),
        "history.empty" to mapOf(AppLanguage.EN to "No history yet", AppLanguage.AR to "لا يوجد سجل بعد"),

        "settings.language" to mapOf(AppLanguage.EN to "Language", AppLanguage.AR to "اللغة")
    )
}
