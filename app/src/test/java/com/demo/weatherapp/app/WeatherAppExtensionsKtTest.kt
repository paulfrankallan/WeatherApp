package com.demo.weatherapp.app

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.*
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset.UTC

class WeatherAppExtensionsKtTest {

    // region Setup

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private var testLiveDataObserver: Observer<Unit> = mock()

    // endregion

    // region Test wasLessThan24HrsAgo

    // region Test future dates

    @Test
    fun `1 second from  now - wasLessThan24HrsAgo`() {

        val timeNow = LocalDateTime.now().plusSeconds(1)

        assertTrue(timeNow.wasLessThan24HrsAgo())
    }

    @Test
    fun `1 hour from now - wasLessThan24HrsAgo`() {

        val timeNow = LocalDateTime.now().plusHours(1)

        assertTrue(timeNow.wasLessThan24HrsAgo())
    }

    @Test
    fun `23 hours 59 minutes and 59 seconds from now - wasLessThan24HrsAgo`() {

        val timeNow = LocalDateTime.now()
            .plusHours(23)
            .plusMinutes(59)
            .plusSeconds(59)

        assertTrue(timeNow.wasLessThan24HrsAgo())
    }

    @Test
    fun `1 day from now - wasLessThan24HrsAgo`() {

        val timeNow = LocalDateTime.now()
            .plusDays(23)

        assertTrue(timeNow.wasLessThan24HrsAgo())
    }

    @Test
    fun `1 year from now - wasLessThan24HrsAgo`() {

        val timeNow = LocalDateTime.now()
            .plusYears(1)

        assertTrue(timeNow.wasLessThan24HrsAgo())
    }

    // endregion

    // region Test past dates

    @Test
    fun `1 second ago - wasLessThan24HrsAgo`() {

        val timeNow = LocalDateTime.now().minusSeconds(1)

        assertTrue(timeNow.wasLessThan24HrsAgo())
    }

    @Test
    fun `1 hour ago - wasLessThan24HrsAgo`() {

        val timeNow = LocalDateTime.now().minusHours(1)

        assertTrue(timeNow.wasLessThan24HrsAgo())
    }

    @Test
    fun `23 hours 59 minutes and 59 seconds ago - wasLessThan24HrsAgo`() {

        val timeNow = LocalDateTime.now()
            .minusHours(23)
            .minusMinutes(59)
            .minusSeconds(59)

        assertTrue(timeNow.wasLessThan24HrsAgo())
    }

    @Test
    fun `1 day ago - wasLessThan24HrsAgo`() {

        val timeNow = LocalDateTime.now()
            .minusDays(23)

        assertFalse(timeNow.wasLessThan24HrsAgo())
    }

    @Test
    fun `1 year ago - wasLessThan24HrsAgo`() {

        val timeNow = LocalDateTime.now()
            .minusYears(1)

        assertFalse(timeNow.wasLessThan24HrsAgo())
    }

    // endregion

    // endregion

    // region Degrees to headings
    @Test
    fun `degreesToHeadingString - 0 degrees = North`() {

        val heading = "North"

        assertEquals(heading, 0F.degreesToHeadingString())
    }

    @Test
    fun `degreesToHeadingString - 90 degrees = East`() {

        val heading = "East"

        assertEquals(heading, 90F.degreesToHeadingString())
    }

    @Test
    fun `degreesToHeadingString - 180 degrees = South`() {

        val heading = "South"

        assertEquals(heading, 180F.degreesToHeadingString())
    }

    @Test
    fun `degreesToHeadingString - 270 degrees = South`() {

        val heading = "West"

        assertEquals(heading, 270F.degreesToHeadingString())
    }

    @Test
    fun `degreesToHeadingString - 360 degrees = North`() {

        val heading = "North"

        assertEquals(heading, 360F.degreesToHeadingString())
    }

    // endregion

    // region Test observeOnce

    @Test
    fun `observeOnce - called once`() {

        val testLiveData = MutableLiveData<Unit>()

        testLiveData.observeOnce(mockLifecycleOwner(), testLiveDataObserver)

        testLiveData.value = Unit

        verify(testLiveDataObserver, times(1)).onChanged(Unit)
    }

    @Test
    fun `observeOnce - called twice`() {

        val testLiveData = MutableLiveData<Unit>()

        testLiveData.observeOnce(mockLifecycleOwner(), testLiveDataObserver)

        testLiveData.value = Unit
        testLiveData.value = Unit

        verify(testLiveDataObserver, times(1)).onChanged(Unit)
    }

    // endregion

    // region Test utcTimeStampToLocalDateTime

    @Test
    fun utcTimeStampToLocalDateTime() {

        // Get date-time now as UTC timestamp
        val localDateTimeNow = LocalDateTime.now().withNano(0)
        val utcDateTimeNow = localDateTimeNow.atZone(UTC)
        val epochSecond: Long = utcDateTimeNow.toEpochSecond()

        // Assert utc timestamp converted back to LocalDateTime as expected
        assertEquals(localDateTimeNow, epochSecond.utcTimeStampToLocalDateTime())

        println(localDateTimeNow)
    }

    // endregion

    // region Test helpers

    private fun mockLifecycleOwner(): LifecycleOwner {
        val owner: LifecycleOwner = mock()
        val lifecycle = LifecycleRegistry(owner)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        `when`(owner.lifecycle).thenReturn(lifecycle)
        return owner
    }

    // endregion
}