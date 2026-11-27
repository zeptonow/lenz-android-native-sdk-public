package com.openreplay.tracker

import com.openreplay.tracker.managers.NetworkManager
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

/**
 * Verifies the configurable image-archive upload guard in
 * [NetworkManager.isImageArchiveTooLarge]. Uses sparse temp files so we exercise the
 * real [File.length]-based decision without writing large files to disk.
 */
class ImageUploadSizeLimitTest {

    private val limit = 5L * 1024 * 1024 // test limit

    @Before
    fun setUp() {
        NetworkManager.maxImageUploadSizeBytes = limit
    }

    @After
    fun tearDown() {
        NetworkManager.maxImageUploadSizeBytes = NetworkManager.DEFAULT_MAX_IMAGE_UPLOAD_SIZE_BYTES
    }

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

    @Test
    fun honorsCustomConfiguredLimit() {
        // A 2 MB archive is rejected when the client configures a 1 MB limit...
        NetworkManager.maxImageUploadSizeBytes = 1L * 1024 * 1024
        assertTrue(NetworkManager.isImageArchiveTooLarge(fileOfSize(2L * 1024 * 1024)))

        // ...and accepted when the client raises the limit to 10 MB.
        NetworkManager.maxImageUploadSizeBytes = 10L * 1024 * 1024
        assertFalse(NetworkManager.isImageArchiveTooLarge(fileOfSize(2L * 1024 * 1024)))
    }
}
