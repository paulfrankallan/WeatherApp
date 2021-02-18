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
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class WeatherFragment : Fragment(), MultiplePermissionsListener {

    // region Members

    private val viewModel: WeatherViewModel by viewModels()
    private lateinit var binding: WeatherFragmentBinding
    private var isGPSEnabled = false

    // endregion

    // region Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prompt user to turn on Gps if it's not already on.
        LocationManager(requireContext()).turnOnGps { isGPSEnabled ->
            this@WeatherFragment.isGPSEnabled = isGPSEnabled
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Configure view binding
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
        // Permissions library call to check permissions.
        // Will refresh if permissions good as per onPermissionsChecked(..) below.
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

    private fun showSnackbar(message: String) = view?.let {
        Snackbar.make(it, message, Snackbar.LENGTH_LONG)
            .setTextColor(Color.WHITE)
            .show()
    }

    // endregion

    // region Refresh spinner

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

        // Ensure refreshing spinner shown for a min [delayMillis] duration.
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

    private fun showSettingsDialog() = AlertDialog.Builder(requireActivity()).apply {
        setTitle(getString(R.string.permissions_dialog_title))
        setMessage(getString(R.string.permissions_dialog_message))
        setPositiveButton(getString(R.string.go_to_settings)) { dialog, _ ->
            dialog.cancel()
            openSettings()
        }
        setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }
        show()
    }

    // Navigate user to app settings.
    private val requestCode = 101
    private fun openSettings() =
        startActivityForResult(Intent(ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireActivity().packageName, null)
        }, requestCode)


    override fun onPermissionRationaleShouldBeShown(
        permissionRequests: MutableList<PermissionRequest>,
        token: PermissionToken
    ) = token.continuePermissionRequest()

    // endregion

    // region No data

    private fun handleNoData(weatherState: WeatherState) {
        binding.noContentLayout.visibility = if(weatherState.noData) VISIBLE else GONE
        binding.weatherCard.visibility = if(weatherState.noData) GONE else VISIBLE
    }

    // endregion
}