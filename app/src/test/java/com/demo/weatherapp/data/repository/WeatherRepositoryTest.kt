package com.demo.weatherapp.data.repository

import android.location.Location
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.demo.weatherapp.TestCoroutineRule
import com.demo.weatherapp.data.model.Result
import com.demo.weatherapp.data.model.WeatherData
import com.demo.weatherapp.data.network.WeatherAppApi
import com.nhaarman.mockitokotlin2.*
import io.realm.Realm
import io.realm.RealmQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import retrofit2.Response

@ExperimentalCoroutinesApi
class WeatherRepositoryTest {

    // region Setup

    // Test subject
    private lateinit var weatherRepository: WeatherAppRepository

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var testCoroutineRule = TestCoroutineRule()

    private val testCoroutineDispatcher = TestCoroutineDispatcher()
    private val testCoroutineScope = TestCoroutineScope(testCoroutineDispatcher)

    private var realm: Realm = mock()
    private var realmQuery: RealmQuery<WeatherData> = mock()
    private var weatherAppApi: WeatherAppApi = mock()

    @Mock
    private var weatherStateObserver: Observer<Result<WeatherData>> = mock()

    @Before
    fun setUp() {

        Dispatchers.setMain(testCoroutineDispatcher)

        whenever(realm.where(WeatherData::class.java)).thenReturn(realmQuery)
        whenever(realmQuery.findFirst()).thenReturn(WeatherData(name = "BOOM"))

        weatherRepository = WeatherAppRepository(
            realm = realm,
            ioDispatcher = testCoroutineDispatcher,
            weatherAppApi = weatherAppApi
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testCoroutineDispatcher.cleanupTestCoroutines()
    }

    // endregion

    // region Tests

    @Test
    fun `syncWeather - Happy Path`() = testCoroutineRule.runBlockingTest {

        val weatherData = WeatherData(name = "BOOM")

        weatherAppApi.stub {
            onBlocking {
                getWeather(
                    any(),
                    any(),
                    any(),
                    any()
                )
            }.doReturn(Response.success(weatherData))
        }

        val location: Location = mock()
        val repositoryObserver = MutableLiveData<Result<WeatherData>>()

        weatherRepository.syncWeather(repositoryObserver = repositoryObserver, location = location)

        repositoryObserver.observeForever(weatherStateObserver)

        verify(weatherStateObserver, times(3)).onChanged(any())

//        verify(weatherStateObserver, times(1)).onChanged(Result.Success(weatherData))

//        // Verify weatherStateObserver
//        argumentCaptor<Result<WeatherData>>().run {
//            verify(weatherStateObserver, times(1)).onChanged(capture())
//            Assert.assertEquals(firstValue, Result.Refreshing(true))
//        }
//
//        repositoryObserver.captureValues {
//            assertSendsValues(1000, Result.Refreshing(false), Result.Refreshing(true))
//        }

        // Verify no further interactions with weatherStateObserver
//        verifyNoMoreInteractions(weatherStateObserver)
    }

    // endregion
}