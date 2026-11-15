package com.junkfood.seal.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppUpdateAssetSelectionTest {
    private fun asset(name: String, url: String = "https://github.com/Lanzkila/KirinDL/releases/download/v3.2.3/$name") =
        UpdateUtil.ReleaseAsset(
            name = name,
            browserDownloadUrl = url,
            contentType = "application/vnd.android.package-archive",
            size = 1024L,
        )

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
    fun returnsNullWhenThereIsNoCompatibleOrUniversalApk() {
        val selected = UpdateUtil.selectCompatibleAppUpdateAsset(
            assets = listOf(asset("KirinDL-3.2.3-x86_64.apk")),
            supportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
        )

        assertNull(selected)
    }
}
