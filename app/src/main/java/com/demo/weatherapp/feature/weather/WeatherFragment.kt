package com.demo.weatherapp.feature.weather

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.demo.weatherapp.R
import com.demo.weatherapp.app.framework.GlideApp
import com.demo.weatherapp.app.location.LocationManager
import com.demo.weatherapp.app.observeOnce
import com.demo.weatherapp.databinding.WeatherFragmentBinding
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class WeatherFragment : Fragment(), MultiplePermissionsListener {

    // region Members

    private val viewModel: WeatherViewModel by viewModel()
    private lateinit var binding: WeatherFragmentBinding
    private var isGPSEnabled = false

    // endregion

    // region Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LocationManager(requireContext()).turnGPSOn(object : LocationManager.GpsProviderListener {
            override fun isGpsProviderEnabled(isGPSEnabled: Boolean) {
                this@WeatherFragment.isGPSEnabled = isGPSEnabled
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Configure binding
        binding = WeatherFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Refresh weather data on click of floating action button
        binding.fab.setOnClickListener { refresh() }

        // Observe ViewModel
        viewModel.weather.observe(viewLifecycleOwner, ::render)

        // Refresh or first load weather data on view creation
        refresh()
    }

    private fun refresh() {
        Dexter.withContext(requireContext())
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ).withListener(this)
            .check()
    }

    // endregion

    // region Render View

    private fun render(weatherState: WeatherState) {
        updateProgressSpinner(weatherState.refreshing)
        populateWeatherFields(weatherState)
        populateLastUpdatedField(weatherState)
        populateLocationField(weatherState)
        loadWeatherIcon(weatherState)
        handleNoData(weatherState)
        handleEvents(weatherState)
    }

    private fun populateWeatherFields(weatherData: WeatherState) {
        binding.valueCurrentCondition.text = weatherData.currentCondition
        binding.valueWindDirection.text = weatherData.windDirection
        binding.valueTemperature.text = weatherData.temperature
        binding.valueWindSpeed.text = weatherData.windSpeed
    }

    private fun populateLastUpdatedField(weatherData: WeatherState) {
        weatherData.updated?.let {
            binding.lastUpdated.text = it
        }
    }

    private fun populateLocationField(weatherData: WeatherState) {
        weatherData.location?.let {
            binding.location.text = it
        }
    }

    private fun handleEvents(weatherState: WeatherState) {
        weatherState.events.forEach { event ->
            when (event) {
                is WeatherState.Event.ShowSnackbar -> {
                    showSnackbar(event.message)
                }
            }
        }
    }

    // endregion

    // region Snackbar

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.weatherFragmentRoot, message, Snackbar.LENGTH_LONG).apply {
            (((view as ViewGroup)
                .getChildAt(0) as ViewGroup)
                .getChildAt(1) as TextView).apply {
                setTextColor(Color.WHITE)
                isAllCaps = false
            }
        }.show()
    }

    // endregion

    // region Progress animation

    private fun updateProgressSpinner(refreshing: Boolean) {
        when (refreshing) {
            true -> {
                savingStartTime = Date().time
                binding.progressSpinner.setImageDrawable(
                    CircularProgressDrawable(requireContext()).apply {
                        setColorSchemeColors(
                            *listOf(
                                ContextCompat.getColor(requireContext(), R.color.colorPrimary),
                                ContextCompat.getColor(requireContext(), R.color.colorAccent)
                            ).toIntArray()
                        )
                        setStyle(CircularProgressDrawable.LARGE)
                        start()
                    })
                binding.progressSpinner.visibility = VISIBLE
            }
            false -> {
                GlobalScope.launch(context = Dispatchers.Main) {
                    hideSpinner()
                }
            }
        }
    }

    private var savingStartTime = 0L
    private val delayMillis = 1300L

    private suspend fun hideSpinner() {

        // Ensure refreshing spinner shown for a min 1 sec duration.
        val currentTimeMillis = Date().time
        val elapsedTimeMillis = currentTimeMillis - savingStartTime
        val remainingTimeMillis = delayMillis - elapsedTimeMillis

        if (elapsedTimeMillis > delayMillis) {
            binding.progressSpinner.visibility = GONE
        } else {
            delay(remainingTimeMillis)
            binding.progressSpinner.visibility = GONE
        }
    }

    // endregion

    // region Weather icon

    private fun loadWeatherIcon(weatherData: WeatherState) =
        GlideApp.with(this@WeatherFragment)
            .load(Uri.parse(getString(R.string.weather_icon_url, weatherData.icon)))
            .priority(Priority.IMMEDIATE)
            .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
            .centerCrop().into(binding.imgCurrentCondition)

    // endregion

    // region Permissions

    override fun onPermissionsChecked(report: MultiplePermissionsReport) {

        if (report.areAllPermissionsGranted()) {
            viewModel.location.observeOnce(viewLifecycleOwner) { location ->
                viewModel.syncWeather(location)
            }
        }

        if (report.isAnyPermissionPermanentlyDenied) {
            // Permission is denied permanently, take user to settings.
            showSettingsDialog()
        }
    }

    private fun showSettingsDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Permissions Required")
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.")
        builder.setPositiveButton("GOTO SETTINGS")
        { dialog, which ->
            dialog.cancel()
            openSettings()
        }
        builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
        builder.show()
    }

    // navigating user to app settings
    private fun openSettings() {
        val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", requireActivity().packageName, null)
        intent.data = uri
        startActivityForResult(intent, 101)
    }

    override fun onPermissionRationaleShouldBeShown(
        permissionRequests: MutableList<PermissionRequest>,
        token: PermissionToken
    ) {
        token.continuePermissionRequest()
    }

    // endregion

    // region No data

    private fun handleNoData(weatherState: WeatherState) {
        when (weatherState.noData) {
            true -> {
                binding.noContentLayout.visibility = VISIBLE
                binding.weatherCard.visibility = GONE
            }
            false -> {
                binding.noContentLayout.visibility = GONE
                binding.weatherCard.visibility = VISIBLE
            }
        }
    }

    // endregion
}