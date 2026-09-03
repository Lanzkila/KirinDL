package com.junkfood.seal.util

import android.net.Uri
import com.junkfood.seal.App

/** Per-site Smart Download Profile memory. UI preference layer only. */
object SiteProfileManager {
    private const val PREFS_NAME = "kirin_site_profiles"
    private const val KEY_PREFIX = "profile_"

    data class Site(val key: String, val label: String)

    val generic = Site("generic", "Other site")

    private val preferences by lazy { App.context.getSharedPreferences(PREFS_NAME, 0) }

    fun detect(url: String): Site {
        val host =
            runCatching {
                    Uri.parse(url.trim()).host.orEmpty().lowercase().removePrefix("www.")
                }
                .getOrDefault("")
        if (host.isBlank()) return generic

        return when {
            host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com") ->
                Site("youtube", "YouTube")
            host == "b23.tv" ||
                host == "bilibili.com" ||
                host.endsWith(".bilibili.com") ||
                host == "bilibili.tv" ||
                host.endsWith(".bilibili.tv") -> Site("bilibili", "Bilibili")
            host == "tiktok.com" || host.endsWith(".tiktok.com") -> Site("tiktok", "TikTok")
            host == "instagram.com" || host.endsWith(".instagram.com") ->
                Site("instagram", "Instagram")
            host == "x.com" ||
                host.endsWith(".x.com") ||
                host == "twitter.com" ||
                host.endsWith(".twitter.com") -> Site("x", "X / Twitter")
            host == "facebook.com" || host.endsWith(".facebook.com") || host == "fb.watch" ->
                Site("facebook", "Facebook")
            host == "reddit.com" || host.endsWith(".reddit.com") || host == "redd.it" ->
                Site("reddit", "Reddit")
            host == "soundcloud.com" || host.endsWith(".soundcloud.com") ->
                Site("soundcloud", "SoundCloud")
            else -> Site("host_${host.replace('.', '_')}", host)
        }
    }

    fun rememberedProfile(site: Site): Int? =
        preferences
            .getInt(KEY_PREFIX + site.key, Int.MIN_VALUE)
            .takeIf { it != Int.MIN_VALUE }

    fun remember(url: String, profileId: Int) {
        val site = detect(url)
        if (site != generic) {
            preferences.edit().putInt(KEY_PREFIX + site.key, profileId).apply()
        }
    }

    fun clear(url: String) {
        val site = detect(url)
        if (site != generic) {
            preferences.edit().remove(KEY_PREFIX + site.key).apply()
        }
    }
}
