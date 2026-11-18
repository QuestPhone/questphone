package neth.iecal.questphone.core.workers

import org.junit.Assert.assertEquals
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
}
