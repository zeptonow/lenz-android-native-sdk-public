package com.openreplay.tracker

import com.openreplay.tracker.managers.NetworkManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

/**
 * Verifies the 5 MB image-archive upload guard in [NetworkManager.isImageArchiveTooLarge].
 * Uses sparse temp files so we exercise the real [File.length]-based decision without
 * writing gigabytes to disk.
 */
class ImageUploadSizeLimitTest {

    private val limit = 5L * 1024 * 1024 // must match NetworkManager.MAX_IMAGE_UPLOAD_SIZE_BYTES

    private fun fileOfSize(bytes: Long): File {
        val f = File.createTempFile("archive", ".tar.gz").apply { deleteOnExit() }
        RandomAccessFile(f, "rw").use { it.setLength(bytes) }
        return f
    }

    @Test
    fun underLimit_isNotTooLarge() {
        assertFalse(NetworkManager.isImageArchiveTooLarge(fileOfSize(limit - 1)))
    }

    @Test
    fun exactlyAtLimit_isAllowed() {
        assertFalse(NetworkManager.isImageArchiveTooLarge(fileOfSize(limit)))
    }

    @Test
    fun oneByteOverLimit_isTooLarge() {
        assertTrue(NetworkManager.isImageArchiveTooLarge(fileOfSize(limit + 1)))
    }

    @Test
    fun missingFile_isNotTooLarge() {
        val missing = File.createTempFile("missing", ".tar.gz").apply { delete() }
        assertFalse(NetworkManager.isImageArchiveTooLarge(missing))
    }
}
