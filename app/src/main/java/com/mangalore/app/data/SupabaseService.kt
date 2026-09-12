package com.mangalore.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Local-only session. No Mangaera/Supabase credentials are bundled.
class SupabaseAuth private constructor(context: Context) {
    companion object {
        @Volatile private var instance: SupabaseAuth? = null
        fun getInstance(context: Context): SupabaseAuth = instance ?: synchronized(this) {
            instance ?: SupabaseAuth(context.applicationContext).also { instance = it }
        }
    }
    var isAuthenticated: Boolean by mutableStateOf(false); private set
    var isGuest: Boolean by mutableStateOf(true); private set
    var profile: SBProfile? by mutableStateOf(null)
    val userId: String? get() = null
    fun continueAsGuest() { isGuest = true; isAuthenticated = false }
    fun signOut() { isGuest = true; isAuthenticated = false; profile = null }
    suspend fun signUp(email: String, password: String, username: String) = continueAsGuest()
    suspend fun signIn(email: String, password: String) = continueAsGuest()
    suspend fun refreshProfile() {}
    fun authHeaders(): Map<String, String> = emptyMap()
}

data class SBProfile(val id: String, var username: String, var email: String? = null, var avatar_url: String? = null, var role: String = "user", var chapters_read_count: Int = 0, var created_at: String = "", var current_streak: Int? = 0, var longest_streak: Int? = 0, var last_read_date: String? = null)
data class SBProfileLite(val id: String, var username: String, var avatar_url: String? = null, var role: String = "user")
data class SBManga(val id: String, var slug: String, var title: String, var cover_url: String? = null, var description: String? = null, var chapters_count: Int = 0, var added_by: String? = null, var created_at: String = "")
data class SBComment(val id: String, var manga_id: String, var user_id: String, var rating: Int? = null, var content: String, var created_at: String = "", var profiles: SBProfileLite? = null)
data class SBMessage(val id: String, var sender_id: String, var receiver_id: String, var content: String? = null, var shared_manga_id: String? = null, var created_at: String = "", var read_at: String? = null)
data class SBAchievement(val id: String, var title: String, var description: String, var icon: String, var threshold: Int, var kind: String)
data class SBUserAchievement(var user_id: String, var achievement_id: String, var unlocked_at: String = "")
data class SBFollow(var follower_id: String, var following_id: String, var created_at: String = "")
sealed class SupabaseException(message: String) : Exception(message) { class NotAuthenticated : SupabaseException("This feature requires an online account."); class Server(message: String) : SupabaseException(message) }

// Cloud/social APIs are intentionally unavailable in the local build.
object SupabaseSync {
    suspend fun upsertManga(manga: Manga, auth: SupabaseAuth) = SBManga(manga.slug, manga.slug, manga.title, manga.coverURL, manga.description, manga.chapters.size)
    suspend fun toggleFavorite(mangaId: String, status: String, auth: SupabaseAuth) {}
    suspend fun removeFavorite(mangaId: String, auth: SupabaseAuth) {}
    suspend fun saveProgress(mangaId: String, chapterSlug: String, chapterNumber: String, pageIndex: Int, auth: SupabaseAuth) {}
    suspend fun postComment(mangaId: String, rating: Int?, content: String, auth: SupabaseAuth): SBComment = throw SupabaseException.NotAuthenticated()
    suspend fun fetchComments(mangaId: String, auth: SupabaseAuth): List<SBComment> = emptyList()
    suspend fun deleteComment(id: String, auth: SupabaseAuth) {}
    suspend fun sendMessage(receiverId: String, content: String?, sharedMangaId: String?, auth: SupabaseAuth) {}
    suspend fun fetchConversation(otherUserId: String, auth: SupabaseAuth): List<SBMessage> = emptyList()
    suspend fun markConversationRead(otherUserId: String, auth: SupabaseAuth) {}
    suspend fun findUser(username: String, auth: SupabaseAuth): SBProfile? = null
    suspend fun recentConversationPartners(auth: SupabaseAuth): List<SBProfile> = emptyList()
    suspend fun unreadMessageCount(auth: SupabaseAuth): Int = 0
    suspend fun fetchAllAchievements(auth: SupabaseAuth): List<SBAchievement> = emptyList()
    suspend fun fetchUnlockedAchievements(userId: String, auth: SupabaseAuth): Set<String> = emptySet()
    suspend fun fetchLeaderboard(auth: SupabaseAuth): List<SBProfile> = emptyList()
    suspend fun fetchPublicProfile(userId: String, auth: SupabaseAuth): SBProfile? = null
    suspend fun followerCount(userId: String, auth: SupabaseAuth): Int = 0
    suspend fun followingCount(userId: String, auth: SupabaseAuth): Int = 0
    suspend fun isFollowing(userId: String, auth: SupabaseAuth): Boolean = false
    suspend fun follow(userId: String, auth: SupabaseAuth) {}
    suspend fun unfollow(userId: String, auth: SupabaseAuth) {}
    suspend fun fetchMangaByIds(ids: List<String>, auth: SupabaseAuth): List<SBManga> = emptyList()
    suspend fun fetchAllManga(auth: SupabaseAuth): List<SBManga> = emptyList()
    suspend fun deleteManga(id: String, auth: SupabaseAuth) {}
    suspend fun fetchAllComments(auth: SupabaseAuth): List<SBComment> = emptyList()
}
