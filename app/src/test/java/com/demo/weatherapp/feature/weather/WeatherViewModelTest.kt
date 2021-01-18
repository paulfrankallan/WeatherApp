package com.demo.weatherapp.feature.weather

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.demo.weatherapp.R
import com.demo.weatherapp.app.degreesToHeadingString
import com.demo.weatherapp.app.framework.DefaultResourceProvider
import com.demo.weatherapp.app.location.LocationClientLiveData
import com.demo.weatherapp.app.utcTimeStampToLocalDateTime
import com.demo.weatherapp.data.model.Main
import com.demo.weatherapp.data.model.Weather
import com.demo.weatherapp.data.model.WeatherData
import com.demo.weatherapp.data.model.Wind
import com.demo.weatherapp.data.network.WeatherAppApi
import com.demo.weatherapp.data.repository.WeatherAppRepository
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
import org.koin.core.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import retrofit2.Response
import kotlin.math.roundToInt

@ExperimentalCoroutinesApi
class WeatherViewModelTest: KoinComponent {

    // region Setup

    // Test subject
    private lateinit var weatherViewModel: WeatherViewModel

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()
    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    private var realm: Realm = mock()
    private var realmQuery: RealmQuery<WeatherData> = mock()
    private var weatherAppApi: WeatherAppApi = mock()
    private var weatherObserver: Observer<WeatherState> = mock()
    private val context: Context = mock()
    private lateinit var weatherRepository: WeatherAppRepository
    private val resourceProvider: DefaultResourceProvider = mock()
    private val locationClientLiveData: LocationClientLiveData = mock()

    private val testModule = module {
        single { context }
        single { weatherRepository }
        single { resourceProvider }
        single { locationClientLiveData }
    }

    @Before
    fun setUp() {

        Dispatchers.setMain(testCoroutineDispatcher)

        whenever(realm.where(WeatherData::class.java)).thenReturn(realmQuery)
        whenever(realmQuery.findFirst()).thenReturn(WeatherData(name = "BOOM"))

        startKoin {
            modules(testModule)
        }

        weatherRepository = WeatherAppRepository(
            realm = realm,
            ioDispatcher = testCoroutineDispatcher,
            weatherAppApi = weatherAppApi
        )

        val noNetworkMessage = "CONNECTION"
        val generalErrorMessage = "OOPS"

        whenever(
            resourceProvider.getResource(R.string.no_internet_connection)
        ).thenReturn(noNetworkMessage)
        whenever(
            resourceProvider.getResource(R.string.something_went_wrong)
        ).thenReturn(generalErrorMessage)

        weatherViewModel = WeatherViewModel()
    }

    @After
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
        testCoroutineDispatcher.cleanupTestCoroutines()
    }

    // endregion

    @Test
    fun `syncWeather happy path`() = runBlockingTest {

        // Given

        val locationName = "Belfast"
        val currentCondition = "Sunny"
        val temperature = 30F
        val windSpeed = 3F
        val windDirection = 360F
        val icon = "sunny.png"

        val localDateTimeNow = LocalDateTime.now().withNano(0)
        val utcDateTimeNow = localDateTimeNow.atZone(ZoneOffset.UTC)
        val epochSecond: Long = utcDateTimeNow.toEpochSecond()
        val timeStamp = epochSecond.utcTimeStampToLocalDateTime()
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))

        whenever(resourceProvider
            .getResource(R.string.formatted_temperature, temperature.roundToInt().toString())
        ).thenReturn(temperature.toString())
        whenever(resourceProvider
            .getResource(R.string.formatted_wind_speed, windSpeed.roundToInt().toString()))
            .thenReturn(windSpeed.toString())
        whenever(resourceProvider
            .getResource(R.string.location, locationName))
            .thenReturn(locationName)
        whenever(resourceProvider
            .getResource(R.string.last_updated, timeStamp))
            .thenReturn(timeStamp.toString())

        val weatherData = WeatherData(
            name = locationName,
            weather = RealmList(Weather(main = currentCondition, icon = icon)),
            wind = Wind(speed = windSpeed, deg = windDirection),
            main = Main(temp = temperature),
            dt = epochSecond
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

        weatherViewModel.weather.observeForever(weatherObserver)

        // When

        weatherViewModel.syncWeather(location = mock())

        // Then

        // Verify weatherStateObserver interactions
        argumentCaptor<WeatherState>().run {

            verify(weatherObserver, times(4)).onChanged(capture())

            val initState = allValues[0]
            val refreshingStartedState = allValues[1]
            val refreshingStoppedState= allValues[2]
            val successWeatherDataState = allValues[3]

            // Verify init state

            // Should be only value set.
            Assert.assertEquals(initState.refreshing, true)

            Assert.assertEquals(initState.location, null)
            Assert.assertEquals(initState.currentCondition, "")
            Assert.assertEquals(initState.icon, null)
            Assert.assertEquals(initState.temperature, "")
            Assert.assertEquals(initState.windSpeed, "")
            Assert.assertEquals(initState.windDirection,  "")
            Assert.assertEquals(initState.updated, null)
            Assert.assertEquals(initState.noData, false)
            Assert.assertTrue(initState.events.isEmpty())

            // Verify refreshing started state

            // Should be only value set.
            Assert.assertEquals(refreshingStartedState.refreshing, true)

            Assert.assertEquals(refreshingStartedState.location, null)
            Assert.assertEquals(refreshingStartedState.currentCondition, "")
            Assert.assertEquals(refreshingStartedState.icon, null)
            Assert.assertEquals(refreshingStartedState.temperature, "")
            Assert.assertEquals(refreshingStartedState.windSpeed, "")
            Assert.assertEquals(refreshingStartedState.windDirection,  "")
            Assert.assertEquals(refreshingStartedState.updated, null)
            Assert.assertEquals(refreshingStartedState.noData, false)
            Assert.assertTrue(refreshingStartedState.events.isEmpty())

            // Verify refreshing stopped state

            // Should be only value set.
            Assert.assertEquals(refreshingStoppedState.refreshing, false)

            Assert.assertEquals(refreshingStoppedState.location, null)
            Assert.assertEquals(refreshingStoppedState.currentCondition, "")
            Assert.assertEquals(refreshingStoppedState.icon, null)
            Assert.assertEquals(refreshingStoppedState.temperature, "")
            Assert.assertEquals(refreshingStoppedState.windSpeed, "")
            Assert.assertEquals(refreshingStoppedState.windDirection,  "")
            Assert.assertEquals(refreshingStoppedState.updated, null)
            Assert.assertEquals(refreshingStoppedState.noData, false)
            Assert.assertTrue(refreshingStoppedState.events.isEmpty())

            // Verify success with weather dataState

            // All values should be set (with no pending events).
            Assert.assertEquals(successWeatherDataState.refreshing, false)

            Assert.assertEquals(successWeatherDataState.location, locationName)
            Assert.assertEquals(successWeatherDataState.currentCondition, currentCondition)
            Assert.assertEquals(successWeatherDataState.icon, icon)
            Assert.assertEquals(successWeatherDataState.temperature, temperature.toString())
            Assert.assertEquals(successWeatherDataState.windSpeed, windSpeed.toString())
            Assert.assertEquals(successWeatherDataState.windDirection,  windDirection.degreesToHeadingString())
            Assert.assertEquals(successWeatherDataState.updated, timeStamp)
            Assert.assertEquals(successWeatherDataState.noData, false)
            Assert.assertTrue(successWeatherDataState.events.isEmpty())
        }
    }
}