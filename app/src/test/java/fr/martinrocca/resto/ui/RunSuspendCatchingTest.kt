package fr.martinrocca.resto.ui

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RunSuspendCatchingTest {
    @Test
    fun `ordinary errors are returned`() = runBlocking {
        val error = IOException("offline")

        val result = runSuspendCatching<Unit> { throw error }

        assertSame(error, result.exceptionOrNull())
    }

    @Test
    fun `cancellation is never converted to a failure result`() {
        val cancellation = CancellationException("screen closed")
        var propagated: CancellationException? = null

        try {
            runBlocking {
                runSuspendCatching<Unit> { throw cancellation }
            }
        } catch (error: CancellationException) {
            propagated = error
        }

        assertTrue(propagated === cancellation)
    }
}
