package fr.martinrocca.resto.data.remote.google

import org.junit.Assert.*
import org.junit.Test

class GoogleCoordinateCacheTest {
    @Test fun expiryUsesMonotonicTimeAndDeletesThePosition() {
        var clock = 0L
        val cache = GoogleCoordinateCache { clock }
        cache.put(GoogleCoordinates("place", 48.8, 2.3))
        clock = GoogleCoordinateCache.MAX_AGE_MILLIS - 1
        assertNotNull(cache.get("place"))
        clock++
        assertNull(cache.get("place"))
        assertTrue(cache.snapshot().isEmpty())
    }
    @Test fun clockRollbackCannotExtendRetention() {
        var clock = 100L
        val cache = GoogleCoordinateCache { clock }
        cache.put(GoogleCoordinates("place", 48.8, 2.3))
        clock = 0
        assertNull(cache.get("place"))
    }
    @Test fun newProcessHasNoGooglePositionAndClearDropsAllPositions() {
        val first = GoogleCoordinateCache()
        first.put(GoogleCoordinates("place", 48.8, 2.3))
        assertTrue(GoogleCoordinateCache().snapshot().isEmpty())
        first.clear()
        assertTrue(first.snapshot().isEmpty())
    }
    @Test fun boundedCacheEvictsOldestAndKeepsNewest() {
        val cache = GoogleCoordinateCache { 0 }
        repeat(GoogleCoordinateCache.MAX_ENTRIES + 1) { cache.put(GoogleCoordinates("place-$it", 48.8, 2.3)) }
        assertEquals(GoogleCoordinateCache.MAX_ENTRIES, cache.snapshot().size)
        assertNull(cache.get("place-0"))
        assertNotNull(cache.get("place-${GoogleCoordinateCache.MAX_ENTRIES}"))
    }
    @Test(expected = IllegalArgumentException::class) fun invalidCoordinatesAreRejected() {
        GoogleCoordinateCache().put(GoogleCoordinates("place", Double.NaN, 2.3))
    }
}
