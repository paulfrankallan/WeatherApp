package com.demo.weatherapp.app

import android.content.Context
import com.demo.weatherapp.app.framework.DefaultResourceProvider
import com.demo.weatherapp.app.framework.ResourceProvider
import com.demo.weatherapp.data.network.WeatherAppClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import io.realm.Realm
import kotlinx.coroutines.Dispatchers

@InstallIn(ViewModelComponent::class)
@Module
class WeatherAppModule {

    @Provides
    fun providesResourceProvider(
        @ApplicationContext context: Context
    ): DefaultResourceProvider = ResourceProvider(context)

    @Provides
    fun providesRealm(): Realm = Realm.getDefaultInstance()

    @Provides
    fun providesDispatcher() = Dispatchers.IO

    @Provides
    fun providesWeatherAppClient(
        @ApplicationContext context: Context
    ) = WeatherAppClient(context).service
}