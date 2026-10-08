package com.junkfood.seal.ui.page.settings.network

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.junkfood.seal.database.objects.CookieProfile
import com.junkfood.seal.util.DatabaseUtil
import com.junkfood.seal.util.isYouTubeUrl
import com.junkfood.seal.util.cookieProfileUrl
import com.junkfood.seal.util.makeToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

/** Reuse the cookie manager so browser sign-in and manual import stay available. */
@Composable
fun YouTubeCookiesButton(modifier: Modifier = Modifier) {
    var showCookies by remember { mutableStateOf(false) }
    var showBrowser by remember { mutableStateOf(false) }
    val cookiesViewModel: CookiesViewModel = koinViewModel()
    val context = LocalContext.current

    FilledTonalButton(modifier = modifier, onClick = { showCookies = true }) {
        Text("Sign in / cookies")
    }
    if (showCookies) {
        LaunchedEffect(Unit) {
            runCatching {
                withContext(Dispatchers.IO) {
                    if (DatabaseUtil.getCookieProfileList().none { isYouTubeUrl(cookieProfileUrl(it.url)) }) {
                        DatabaseUtil.insertCookieProfile(
                            CookieProfile(id = 0, url = "https://www.youtube.com", content = ""),
                        )
                    }
                }
            }.onFailure {
                if (it is CancellationException) throw it
                context.makeToast("Could not prepare the YouTube cookie profile")
            }
        }
        Dialog(
            onDismissRequest = { showCookies = false; showBrowser = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                if (showBrowser) {
                    WebViewPage(cookiesViewModel) { showBrowser = false }
                } else {
                    CookieProfilePage(
                        cookiesViewModel = cookiesViewModel,
                        navigateToCookieGeneratorPage = { showBrowser = true },
                        onNavigateBack = { showCookies = false },
                    )
                }
            }
        }
    }
}
