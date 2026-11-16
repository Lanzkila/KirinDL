package com.junkfood.seal.util.instaloader

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.IOException
import org.json.JSONObject

/**
 * Calls KirinDL's isolated Python adapter. This only reads public profile metadata;
 * the existing GalleryDL engine remains responsible for downloading selected posts.
 */
object InstagramPreviewBridge {
    const val EXTRACTOR_NAME = "InstaloaderProfile"
    const val PREVIEW_LIMIT = 36

    fun preview(context: Context, profileUrl: String): Result<InstagramProfilePreview> =
        runCatching {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context.applicationContext))
            }

            val module =
                Python.getInstance().getModule("kirindl_instaloader.instaloader_bridge")
            val raw = module.callAttr("preview_profile", profileUrl, PREVIEW_LIMIT).toString()
            val result = JSONObject(raw)

            if (!result.optBoolean("ok", false)) {
                throw IOException(
                    result.optString("error").ifBlank {
                        "Instaloader could not preview this Instagram profile"
                    }
                )
            }

            val postsArray = result.optJSONArray("items")
            val posts =
                buildList {
                    if (postsArray != null) {
                        for (index in 0 until postsArray.length()) {
                            val item = postsArray.optJSONObject(index) ?: continue
                            val postUrl =
                                InstagramPostSelection.canonicalPostUrl(item.optString("url"))
                                    ?: continue
                            val thumbnail =
                                item.optString("thumbnail")
                                    .takeIf { it.startsWith("https://") }
                                    .orEmpty()
                            add(
                                InstagramPreviewPost(
                                    id = item.optString("id").ifBlank { postUrl },
                                    title = item.optString("title").ifBlank { postUrl },
                                    url = postUrl,
                                    thumbnailUrl = thumbnail,
                                    creator = item.optString("creator"),
                                )
                            )
                        }
                    }
                }

            InstagramProfilePreview(
                username = result.optString("username"),
                fullName = result.optString("full_name"),
                mediaCount =
                    if (result.has("media_count") && !result.isNull("media_count")) {
                        result.optInt("media_count").takeIf { it >= 0 }
                    } else {
                        null
                    },
                posts = posts,
            )
        }
}
