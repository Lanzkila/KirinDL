package com.junkfood.seal.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateAssetSelectionTest {
    private fun asset(name: String, url: String = "https://github.com/Lanzkila/KirinDL/releases/download/v3.2.3/$name") =
        UpdateUtil.ReleaseAsset(
            name = name,
            browserDownloadUrl = url,
            contentType = "application/vnd.android.package-archive",
            size = 1024L,
        )

    private fun release(tag: String, prerelease: Boolean) =
        UpdateUtil.Release(
            tagName = tag,
            draft = false,
            preRelease = prerelease,
        )

    private fun version(patch: Int) =
        UpdateUtil.Version.Stable(
            versionMajor = 3,
            versionMinor = 2,
            versionPatch = patch,
        )

    @Test
    fun prereleaseChannelPrefersStableWhenStableIsNewerThanInstalled() {
        val selected = UpdateUtil.selectLatestRelease(
            releases = listOf(
                release("v3.2.4", prerelease = true),
                release("v3.2.3", prerelease = false),
                release("v3.2.2", prerelease = true),
            ),
            installedVersion = version(2),
            selectedChannel = PRE_RELEASE,
        )

        assertEquals("v3.2.1", selected?.tagName)
    }

    @Test
    fun installedPreReleaseMovesToStableEvenWhenStableIsOlder() {
        val selected = UpdateUtil.selectLatestRelease(
            releases = listOf(
                release("v3.2.3", prerelease = true),
                release("v3.2.2", prerelease = true),
                release("v3.2.1", prerelease = false),
            ),
            installedVersion = version(2),
            selectedChannel = PRE_RELEASE,
        )

        assertEquals("v3.2.3", selected?.tagName)
    }

    @Test
    fun prereleaseInstallIsForcedToOlderStableUntilItIsPromoted() {
        val releases = listOf(
            release("v3.2.3", prerelease = true),
            release("v3.2.2", prerelease = true),
            release("v3.2.1", prerelease = false),
        )
        val installedVersion = version(2)
        val selected = UpdateUtil.selectLatestRelease(
            releases = releases,
            installedVersion = installedVersion,
            selectedChannel = STABLE,
        )
        val installedFromPreRelease =
            UpdateUtil.isInstalledFromPreRelease(releases, installedVersion)
        val selectedVersion = with(UpdateUtil) { selected?.tagName.toVersionOrNull() }

        assertEquals("v3.2.1", selected?.tagName)
        assertTrue(installedFromPreRelease)
        assertTrue(
            UpdateUtil.shouldOfferSelectedRelease(
                installedVersion = installedVersion,
                selectedReleaseVersion = selectedVersion!!,
                installedFromPreRelease = installedFromPreRelease,
            ),
        )
    }

    @Test
    fun stableInstallIsNotForcedBackToAnOlderStableVersion() {
        val releases = listOf(
            release("v3.2.3", prerelease = true),
            release("v3.2.2", prerelease = true),
            release("v3.2.1", prerelease = false),
        )
        val installedVersion = version(1)
        val installedFromPreRelease =
            UpdateUtil.isInstalledFromPreRelease(releases, installedVersion)

        assertFalse(installedFromPreRelease)
        assertFalse(
            UpdateUtil.shouldOfferSelectedRelease(
                installedVersion = installedVersion,
                selectedReleaseVersion = version(1),
                installedFromPreRelease = installedFromPreRelease,
            ),
        )
    }

    @Test
    fun keepsOlderStableReleaseCachedWhileInstalledPreReleaseIsAwaitingPromotion() {
        assertTrue(
            UpdateUtil.shouldKeepCachedRelease(
                installedVersion = version(2),
                cachedVersion = version(1),
                forcedStableMigration = true,
            ),
        )
    }

    @Test
    fun clearsOlderStableReleaseCacheAfterStableVersionIsInstalled() {
        assertFalse(
            UpdateUtil.shouldKeepCachedRelease(
                installedVersion = version(1),
                cachedVersion = version(1),
                forcedStableMigration = false,
            ),
        )
    }

    @Test
    fun stableChannelIgnoresNewerPrereleasesForStableInstall() {
        val selected = UpdateUtil.selectLatestRelease(
            releases = listOf(
                release("v3.2.4", prerelease = true),
                release("v3.2.3", prerelease = false),
                release("v3.2.2", prerelease = false),
            ),
            installedVersion = version(2),
            selectedChannel = STABLE,
        )

        assertEquals("v3.2.3", selected?.tagName)
    }

    @Test
    fun prefersTheFirstSupportedDeviceAbiOverUniversal() {
        val arm64 = asset("KirinDL-3.2.3-arm64-v8a.apk")
        val universal = asset("KirinDL-3.2.3-universal.apk")
        val selected = UpdateUtil.selectCompatibleAppUpdateAsset(
            assets = listOf(universal, arm64),
            supportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
        )

        assertEquals(arm64, selected)
    }

    @Test
    fun followsDeviceAbiPriorityWhenMultipleAbiBuildsArePresent() {
        val armeabi = asset("KirinDL-3.2.3-armeabi-v7a.apk")
        val arm64 = asset("KirinDL-3.2.3-arm64-v8a.apk")
        val selected = UpdateUtil.selectCompatibleAppUpdateAsset(
            assets = listOf(armeabi, arm64),
            supportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
        )

        assertEquals(arm64, selected)
    }

    @Test
    fun fallsBackToUniversalWhenNoDeviceAbiBuildExists() {
        val universal = asset("KirinDL-3.2.3-universal.apk")
        val selected = UpdateUtil.selectCompatibleAppUpdateAsset(
            assets = listOf(asset("KirinDL-3.2.3-x86_64.apk"), universal),
            supportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
        )

        assertEquals(universal, selected)
    }

    @Test
    fun ignoresNonApkAndNonHttpsAssets() {
        val invalidUrl = asset(
            "KirinDL-3.2.3-arm64-v8a.apk",
            url = "http://example.com/KirinDL.apk",
        )
        val checksum = asset("KirinDL-3.2.3-arm64-v8a.apk.sha256")
        val universal = asset("KirinDL-3.2.3-universal.apk")

        val selected = UpdateUtil.selectCompatibleAppUpdateAsset(
            assets = listOf(invalidUrl, checksum, universal),
            supportedAbis = listOf("arm64-v8a"),
        )

        assertEquals(universal, selected)
    }

    @Test
    fun aboutBackgroundUpdaterKeepsItsUniversalOnlyAssetContract() {
        val abiOnlyRelease = UpdateUtil.Release(
            assets = listOf(asset("KirinDL-3.2.3-arm64-v8a.apk")),
        )
        val universalRelease = UpdateUtil.Release(
            assets = listOf(asset("KirinDL-3.2.3-universal.apk")),
        )

        assertFalse(UpdateUtil.hasBackgroundAppUpdate(abiOnlyRelease))
        assertTrue(UpdateUtil.hasBackgroundAppUpdate(universalRelease))
    }

    @Test
    fun returnsNullWhenThereIsNoCompatibleOrUniversalApk() {
        val selected = UpdateUtil.selectCompatibleAppUpdateAsset(
            assets = listOf(asset("KirinDL-3.2.3-x86_64.apk")),
            supportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
        )

        assertNull(selected)
    }
}
