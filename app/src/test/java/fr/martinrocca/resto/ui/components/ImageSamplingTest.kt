package fr.martinrocca.resto.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSamplingTest {
    @Test
    fun `large panorama is sampled from its longest side`() {
        assertEquals(
            16,
            calculateInSampleSize(
                width = 12_000,
                height = 1_000,
                requestedSize = 512,
            ),
        )
    }

    @Test
    fun `small image is not enlarged`() {
        assertEquals(
            1,
            calculateInSampleSize(width = 400, height = 300, requestedSize = 512),
        )
    }
}
