package neth.iecal.questphone.core.workers

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileDownloadWorkerTest {

    @Test
    fun parseTotalSizeFromContentRange_standardHeader_returnsTotalSize() {
        val header = "bytes 0-1023/2048"
        val totalSize = FileDownloadWorker.parseTotalSizeFromContentRange(header)
        assertEquals(2048L, totalSize)
    }

    @Test
    fun parseTotalSizeFromContentRange_offsetRange_returnsTotalSize() {
        val header = "bytes 5242880-10485759/50000000"
        val totalSize = FileDownloadWorker.parseTotalSizeFromContentRange(header)
        assertEquals(50000000L, totalSize)
    }

    @Test
    fun parseTotalSizeFromContentRange_unknownTotalAsterisk_returnsMinusOne() {
        val header = "bytes 0-1023/*"
        val totalSize = FileDownloadWorker.parseTotalSizeFromContentRange(header)
        assertEquals(-1L, totalSize)
    }

    @Test
    fun parseTotalSizeFromContentRange_nullHeader_returnsMinusOne() {
        val totalSize = FileDownloadWorker.parseTotalSizeFromContentRange(null)
        assertEquals(-1L, totalSize)
    }

    @Test
    fun parseTotalSizeFromContentRange_malformedHeader_returnsMinusOne() {
        val header = "invalid-content-range"
        val totalSize = FileDownloadWorker.parseTotalSizeFromContentRange(header)
        assertEquals(-1L, totalSize)
    }

    @Test
    fun resolveEffectiveOffsetAndTotalSize_http206WithContentRange_preservesOffsetAndParsesTotal() {
        val (offset, total) = FileDownloadWorker.resolveEffectiveOffsetAndTotalSize(
            responseCode = 206,
            existingBytes = 1024L,
            contentLength = 1024L,
            contentRangeHeader = "bytes 1024-2047/4096"
        )
        assertEquals(1024L, offset)
        assertEquals(4096L, total)
    }

    @Test
    fun resolveEffectiveOffsetAndTotalSize_http206WithoutContentRange_computesTotalFromOffsetPlusLength() {
        val (offset, total) = FileDownloadWorker.resolveEffectiveOffsetAndTotalSize(
            responseCode = 206,
            existingBytes = 1000L,
            contentLength = 2500L,
            contentRangeHeader = null
        )
        assertEquals(1000L, offset)
        assertEquals(3500L, total)
    }

    @Test
    fun resolveEffectiveOffsetAndTotalSize_http200WhenRangeRequested_resetsOffsetToZero() {
        val (offset, total) = FileDownloadWorker.resolveEffectiveOffsetAndTotalSize(
            responseCode = 200,
            existingBytes = 1024L,
            contentLength = 4096L,
            contentRangeHeader = null
        )
        assertEquals(0L, offset)
        assertEquals(4096L, total)
    }

    @Test
    fun writeStreamToRandomAccessFile_freshWriteAndResumeAndTruncate_worksCorrectly() {
        val tempFile = java.io.File.createTempFile("download_test", ".part")
        try {
            val part1 = "Hello, ".toByteArray(Charsets.UTF_8)
            val part2 = "QuestPhone On-Device AI!".toByteArray(Charsets.UTF_8)
            val fullExpected = part1 + part2

            val progresses = mutableListOf<Int>()
            val written1 = FileDownloadWorker.writeStreamToRandomAccessFile(
                input = java.io.ByteArrayInputStream(part1),
                file = tempFile,
                downloadedBytes = 0L,
                totalFileSize = fullExpected.size.toLong(),
                isCancelled = { false },
                onProgress = { progresses.add(it) }
            )
            assertEquals(part1.size.toLong(), written1)
            assertEquals(part1.size.toLong(), tempFile.length())

            val written2 = FileDownloadWorker.writeStreamToRandomAccessFile(
                input = java.io.ByteArrayInputStream(part2),
                file = tempFile,
                downloadedBytes = part1.size.toLong(),
                totalFileSize = fullExpected.size.toLong(),
                isCancelled = { false },
                onProgress = { progresses.add(it) }
            )
            assertEquals(fullExpected.size.toLong(), written2)
            org.junit.Assert.assertArrayEquals(fullExpected, tempFile.readBytes())
            org.junit.Assert.assertTrue(progresses.contains(100))

            // Simulate HTTP 200 fallback where downloadedBytes = 0L on a file that already has more bytes
            val shortPayload = "Short".toByteArray(Charsets.UTF_8)
            val writtenShort = FileDownloadWorker.writeStreamToRandomAccessFile(
                input = java.io.ByteArrayInputStream(shortPayload),
                file = tempFile,
                downloadedBytes = 0L,
                totalFileSize = shortPayload.size.toLong(),
                isCancelled = { false },
                onProgress = {}
            )
            assertEquals(shortPayload.size.toLong(), writtenShort)
            assertEquals(shortPayload.size.toLong(), tempFile.length())
            org.junit.Assert.assertArrayEquals(shortPayload, tempFile.readBytes())
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun writeStreamToRandomAccessFile_whenStopped_abortsEarly() {
        val tempFile = java.io.File.createTempFile("download_stop_test", ".part")
        try {
            val payload = ByteArray(32768) { (it % 256).toByte() }
            org.junit.Assert.assertThrows(kotlinx.coroutines.CancellationException::class.java) {
                FileDownloadWorker.writeStreamToRandomAccessFile(
                    input = java.io.ByteArrayInputStream(payload),
                    file = tempFile,
                    downloadedBytes = 0L,
                    totalFileSize = payload.size.toLong(),
                    isCancelled = { true },
                    onProgress = {}
                )
            }
            assertEquals(0L, tempFile.length())
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun updatePrefsOnFailure_matchingDownloadingModel_clearsDownloadingFlag() {
        val sp = InMemorySharedPreferences().apply {
            edit().putString("downloading", "SIGLIP2_UINT8").commit()
        }

        FileDownloadWorker.updatePrefsOnFailure(
            sp = sp,
            modelId = "SIGLIP2_UINT8",
            isOneShotModel = true
        )

        assertFalse(sp.contains("downloading"))
        assertNull(sp.getString("downloading", null))
    }

    @Test
    fun updatePrefsOnFailure_differentActiveDownload_preservesNewDownloadingFlag() {
        val sp = InMemorySharedPreferences().apply {
            edit().putString("downloading", "SIGLIP2_Q4F16").commit()
        }

        // Simulate older cancelled/failed worker for SIGLIP2_UINT8 finishing after user started SIGLIP2_Q4F16
        FileDownloadWorker.updatePrefsOnFailure(
            sp = sp,
            modelId = "SIGLIP2_UINT8",
            isOneShotModel = true
        )

        assertTrue(sp.contains("downloading"))
        assertEquals("SIGLIP2_Q4F16", sp.getString("downloading", null))
    }

    @Test
    fun updatePrefsOnFailure_nonOneShotWorker_doesNotClearOneShotDownloadingFlag() {
        val sp = InMemorySharedPreferences().apply {
            edit().putString("downloading", "SIGLIP2_Q4F16").commit()
        }

        FileDownloadWorker.updatePrefsOnFailure(
            sp = sp,
            modelId = "tokenizer",
            isOneShotModel = false
        )

        assertEquals("SIGLIP2_Q4F16", sp.getString("downloading", null))
    }

    @Test
    fun updatePrefsOnSuccess_respectsActiveDownloadingOwner() {
        val sp = InMemorySharedPreferences().apply {
            edit().putString("downloading", "SIGLIP2_Q4F16").commit()
        }

        // Normal matching completion clears downloading and selects model
        FileDownloadWorker.updatePrefsOnSuccess(
            sp = sp,
            modelId = "SIGLIP2_Q4F16",
            version = "1.0.0",
            isOneShotModel = true
        )
        assertFalse(sp.contains("downloading"))
        assertEquals("SIGLIP2_Q4F16", sp.getString("selected_one_shot_model", null))
        assertTrue(sp.getBoolean("is_downloaded_SIGLIP2_Q4F16", false))
        assertEquals("1.0.0", sp.getString("version_SIGLIP2_Q4F16", null))

        // If another model is currently downloading, late completion of a different model preserves active download
        sp.edit().putString("downloading", "SIGLIP2_INT8").commit()
        FileDownloadWorker.updatePrefsOnSuccess(
            sp = sp,
            modelId = "SIGLIP2_UINT8",
            version = "1.0.0",
            isOneShotModel = true
        )
        assertEquals("SIGLIP2_INT8", sp.getString("downloading", null))
        assertEquals("SIGLIP2_Q4F16", sp.getString("selected_one_shot_model", null))
        assertTrue(sp.getBoolean("is_downloaded_SIGLIP2_UINT8", false))
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val data = linkedMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = data.toMutableMap()

        override fun getString(key: String?, defValue: String?): String? =
            (data[key] as? String) ?: defValue

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (data[key] as? MutableSet<String>) ?: defValues

        override fun getInt(key: String?, defValue: Int): Int =
            (data[key] as? Int) ?: defValue

        override fun getLong(key: String?, defValue: Long): Long =
            (data[key] as? Long) ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float =
            (data[key] as? Float) ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            (data[key] as? Boolean) ?: defValue

        override fun contains(key: String?): Boolean = data.containsKey(key)

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val pending = linkedMapOf<String, Any?>()
            private var clearRequested = false

            override fun putString(key: String?, value: String?) = apply {
                if (key != null) pending[key] = value
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?) = apply {
                if (key != null) pending[key] = values?.toMutableSet()
            }

            override fun putInt(key: String?, value: Int) = apply {
                if (key != null) pending[key] = value
            }

            override fun putLong(key: String?, value: Long) = apply {
                if (key != null) pending[key] = value
            }

            override fun putFloat(key: String?, value: Float) = apply {
                if (key != null) pending[key] = value
            }

            override fun putBoolean(key: String?, value: Boolean) = apply {
                if (key != null) pending[key] = value
            }

            override fun remove(key: String?) = apply {
                if (key != null) pending[key] = REMOVED
            }

            override fun clear() = apply {
                clearRequested = true
                pending.clear()
            }

            override fun commit(): Boolean {
                if (clearRequested) {
                    data.clear()
                }
                for ((k, v) in pending) {
                    if (v === REMOVED) {
                        data.remove(k)
                    } else if (v == null) {
                        data.remove(k)
                    } else {
                        data[k] = v
                    }
                }
                return true
            }

            override fun apply() {
                commit()
            }
        }

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) {}

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) {}

        private companion object {
            val REMOVED = Any()
        }
    }
}
