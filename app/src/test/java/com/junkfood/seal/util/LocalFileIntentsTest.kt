package com.junkfood.seal.util

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import java.io.File
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class LocalFileIntentsTest {
    private lateinit var context: Context
    private val bytes = "local video bytes".toByteArray()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        // Each test has a new cache directory. Attach a fresh provider so its static
        // path cache cannot point to the preceding test's Application directory.
        Robolectric.buildContentProvider(FileProvider::class.java).create()
    }

    private fun video(name: String = "Video #1 ü.mp4") =
        File(context.cacheDir, name).apply { writeBytes(bytes) }

    private fun assertReadableLocalVideo(intent: Intent, expectedType: String = "video/mp4") {
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("content", intent.data!!.scheme)
        assertEquals(expectedType, intent.type)
        assertEquals(intent.data, intent.clipData!!.getItemAt(0).uri)
        assertTrue(intent.clipData!!.description.hasMimeType(expectedType))
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertArrayEquals(bytes, context.contentResolver.openInputStream(intent.data!!)!!.use { it.readBytes() })
    }

    @Test
    fun playHandsOffAnEncodedReadableLocalFileWithMimeTypeAndReadPermission() {
        val file = video()
        assertReadableLocalVideo(LocalFileIntents.open(context, file.absolutePath)!!)
    }

    @Test
    fun fileUriUsesFileProviderAndNeverExposesFileScheme() {
        assertReadableLocalVideo(LocalFileIntents.open(context, video().toURI().toString())!!)
    }

    @Test
    fun existingContentUriRemainsLocalAndReadable() {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", video())
        val intent = LocalFileIntents.open(context, uri.toString())!!
        assertEquals(uri, intent.data)
        assertReadableLocalVideo(intent)
    }

    @Test
    fun sdcardDocumentWithGenericMimeTypeUsesMediaNameWithoutChangingItsUri() {
        val provider = Robolectric.buildContentProvider(LocalDocumentProvider::class.java)
            .create("test.documents").get()
        provider.file = video("Video.mkv")
        val uri = Uri.parse("content://test.documents/document/video")
        val intent = LocalFileIntents.open(context, uri.toString())!!
        assertEquals(uri, intent.data)
        assertReadableLocalVideo(intent, "video/x-matroska")
    }

    @Test
    fun sharingUsesTheSameReadableUriAndExplicitAudioType() {
        val file = video("Audio.m4a")
        val intent = LocalFileIntents.share(context, file.absolutePath)!!
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("audio/mp4", intent.type)
        assertEquals(intent.data, intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        assertEquals(intent.data, intent.clipData!!.getItemAt(0).uri)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertArrayEquals(bytes, context.contentResolver.openInputStream(intent.data!!)!!.use { it.readBytes() })
    }

    @Test
    fun unavailableFilesDirectoriesAndRemoteUrlsCannotBePlayed() {
        for (path in listOf(null, "", "https://youtu.be/N5V8HEoo_10",
            "https://example.com/expired.mp4", "file:///missing.mp4",
            File(context.cacheDir, "missing.mp4").absolutePath, context.cacheDir.absolutePath)) {
            assertNull(LocalFileIntents.open(context, path))
        }
    }

    class LocalDocumentProvider : ContentProvider() {
        lateinit var file: File
        override fun onCreate() = true
        override fun getType(uri: Uri) = "application/octet-stream"
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
            selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
            val columns = projection ?: arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            return MatrixCursor(columns).apply {
                addRow(columns.map { column ->
                    when (column) {
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID -> "video"
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME -> file.name
                        DocumentsContract.Document.COLUMN_SIZE -> file.length()
                        DocumentsContract.Document.COLUMN_MIME_TYPE -> getType(uri)
                        DocumentsContract.Document.COLUMN_FLAGS -> 0
                        else -> null
                    }
                })
            }
        }
        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor =
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    }
}
