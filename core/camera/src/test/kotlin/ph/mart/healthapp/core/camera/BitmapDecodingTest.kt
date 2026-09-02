package ph.mart.healthapp.core.camera

import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.reflect.Method

class BitmapDecodingTest {

    @Test
    fun testSampleSizeFor() {
        val method: Method = Class.forName("ph.mart.healthapp.core.camera.BitmapDecodingKt")
            .getDeclaredMethod("sampleSizeFor", Int::class.java, Int::class.java)
        method.isAccessible = true

        fun getSampleSize(width: Int, height: Int): Int {
            return method.invoke(null, width, height) as Int
        }

        // MAX_CAPTURE_EDGE is 1280
        assertEquals(1, getSampleSize(1280, 720))
        assertEquals(1, getSampleSize(2559, 1000))
        assertEquals(2, getSampleSize(2560, 1440)) // 2560 / 2 = 1280 >= 1280
        assertEquals(2, getSampleSize(5119, 3000))
        assertEquals(4, getSampleSize(5120, 4000)) // 5120 / 2 = 2560, 2560 / 2 = 1280
    }
}
