package com.demo.weatherapp.data.repository

import android.location.Location
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.demo.weatherapp.data.model.*
import com.demo.weatherapp.data.network.WeatherAppApi
import com.nhaarman.mockitokotlin2.*
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.*
import retrofit2.Response

@ExperimentalCoroutinesApi
class WeatherAppRepositoryTest {

    // region Setup

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()
    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    // Test subject
    private lateinit var weatherRepository: WeatherAppRepository

    // Mocks
    private var realm: Realm = mock()
    private var realmQuery: RealmQuery<WeatherDataDAO> = mock()
    private var weatherAppApi: WeatherAppApi = mock()
    private var weatherDataStateObserver: Observer<Result<WeatherDataDAO>> = mock()

    @Before
    fun setUp() {

        Dispatchers.setMain(testCoroutineDispatcher)

        whenever(realm.where(WeatherDataDAO::class.java)).thenReturn(realmQuery)
        whenever(realmQuery.findFirst()).thenReturn(WeatherDataDAO(name = "BOOM"))

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
    fun `syncWeather - Happy Path`() = runBlockingTest {

        // Given

        val locationName = "Belfast"
        val currentCondition = "Sunny"
        val temperature = 30F
        val windSpeed = 3F
        val windDirection = 360F
        val timeStamp = 123456789L
        val icon = "sunny.png"

        val weatherData = WeatherDataDAO(
            name = locationName,
            weatherDAO = RealmList(WeatherDAO(main = currentCondition, icon = icon)),
            wind = WindDAO(speed = windSpeed, deg = windDirection),
            main = MainDAO(temp = temperature),
            dt = timeStamp
        )

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
        val repositoryObserver = MutableLiveData<Result<WeatherDataDAO>>()

        repositoryObserver.observeForever(weatherDataStateObserver)

        // when

        weatherRepository.syncWeather(repositoryObserver = repositoryObserver, location = location)

        // Then

        // Verify weatherStateObserver interactions
        argumentCaptor<Result<WeatherDataDAO>>().run {

            verify(weatherDataStateObserver, times(3)).onChanged(capture())

            val result1 = firstValue as Result.Refreshing
            val result2 = secondValue as Result.Refreshing
            val result3 = thirdValue as Result.Success

            // Check we got expected results
            Assert.assertEquals(result1, Result.Refreshing(true))
            Assert.assertEquals(result2, Result.Refreshing(false))
            Assert.assertEquals(result3, Result.Success(weatherData))

            // Check all weather values
            Assert.assertEquals(result3.data.name, locationName)
            Assert.assertEquals(result3.data.weatherDAO?.get(0)?.main, currentCondition)
            Assert.assertEquals(result3.data.weatherDAO?.get(0)?.icon, icon)
            Assert.assertEquals(result3.data.main?.temp, temperature)
            Assert.assertEquals(result3.data.wind?.speed, windSpeed)
            Assert.assertEquals(result3.data.wind?.deg, windDirection)
            Assert.assertEquals(result3.data.dt, timeStamp)
        }

        // Verify no further interactions with weatherStateObserver
        verifyNoMoreInteractions(weatherDataStateObserver)
    }

    // endregion
}