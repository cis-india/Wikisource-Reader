package org.cis_india.wsreader.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CoverStorageTest {

    @Test
    fun `image already smaller than the cover is not downsampled`() {
        assertThat(CoverStorage.calculateInSampleSize(100, 150)).isEqualTo(1)
    }

    @Test
    fun `image the same size as the cover is not downsampled`() {
        assertThat(CoverStorage.calculateInSampleSize(120, 200)).isEqualTo(1)
    }

    @Test
    fun `image just under twice the cover size is not downsampled`() {
        assertThat(CoverStorage.calculateInSampleSize(239, 399)).isEqualTo(1)
    }

    @Test
    fun `image twice the cover size is halved`() {
        assertThat(CoverStorage.calculateInSampleSize(240, 400)).isEqualTo(2)
    }

    @Test
    fun `large cover is downsampled but stays at least the cover size`() {
        val sampleSize = CoverStorage.calculateInSampleSize(1200, 2000)

        assertThat(sampleSize).isEqualTo(8)
        assertThat(1200 / sampleSize).isAtLeast(120)
        assertThat(2000 / sampleSize).isAtLeast(200)
    }

    @Test
    fun `the smaller side limits the sample size`() {
        // A wide image: the height reaches the 200px target long before the width does.
        assertThat(CoverStorage.calculateInSampleSize(6000, 400)).isEqualTo(2)
    }

    @Test
    fun `failed bounds decode does not downsample`() {
        // BitmapFactory reports -1 when it cannot read the image size.
        assertThat(CoverStorage.calculateInSampleSize(-1, -1)).isEqualTo(1)
    }
}
