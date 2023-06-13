package io.glassfy.androidsdk

import io.glassfy.androidsdk.BuildConfig.SDK_VERSION
import io.glassfy.androidsdk.model.Offerings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class GlassfyTest {
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")
    private var mainThreadId = -1L
    private val lock = CountDownLatch(1)

//    @Mock
//    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
        runBlocking(Dispatchers.Main) {
            mainThreadId = Thread.currentThread().id
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    @Test
    fun `Check SDK version`() {
        assertEquals(Glassfy.sdkVersion, SDK_VERSION)
    }

    @Test
    fun `Offering no initialize`() {
        var _offerings: Offerings? = null
        var _error: GlassfyError? = null
        Glassfy.offerings() { result, error ->
            assertEquals(Thread.currentThread().id, mainThreadId)

            _offerings = result
            _error = error
            lock.countDown()
        }

        lock.await(15000L, TimeUnit.MILLISECONDS)
        assertNull(_offerings)
        assertNotNull(_error)
    }
}