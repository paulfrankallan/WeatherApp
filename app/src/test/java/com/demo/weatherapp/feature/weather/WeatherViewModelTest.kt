package com.demo.weatherapp.feature.weather

import android.content.Context
import android.location.Location
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.demo.weatherapp.app.framework.DefaultResourceProvider
import com.demo.weatherapp.app.framework.ResourceProvider
import com.demo.weatherapp.app.location.LocationClientLiveData
import com.demo.weatherapp.data.repository.WeatherAppRepository
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mock

@ExperimentalCoroutinesApi
class WeatherViewModelTest: KoinComponent {

    // region Setup

    // Test subject
    private lateinit var weatherViewModel: WeatherViewModel

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private var weatherObserver: Observer<WeatherState> = mock()
    private val context: Context = mock()
    private val weatherRepository: WeatherAppRepository = mock()
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

        startKoin {
            modules(testModule)
        }

        weatherViewModel = WeatherViewModel()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    // endregion

    @Test
    fun getActions() = runBlockingTest {

        val location: Location = mock()

        weatherViewModel.weather.observeForever(weatherObserver)

        weatherViewModel.syncWeather(location)

        verify(weatherRepository, times(1)).syncWeather(any(), any())
    }

    @Test
    fun getLocationLiveData() {

        weatherViewModel.location
    }

    @Test
    fun getWeather() {

        weatherViewModel.weather
    }
}