package com.demo.weatherapp.feature.weather

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.demo.weatherapp.R
import com.demo.weatherapp.app.framework.GlideApp
import com.demo.weatherapp.databinding.WeatherFragmentBinding
import com.demo.weatherapp.feature.weather.WeatherState.Action
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

class WeatherFragment : Fragment() {

    // region Members

    private val viewModel: WeatherViewModel by viewModel()
    private lateinit var binding: WeatherFragmentBinding

    // endregion

    // region Lifecycle

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = WeatherFragmentBinding.inflate(
            inflater, container, false
        ).apply {
            viewModel = viewModel
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fab.setOnClickListener { viewModel.actions.value = Action.Refresh }

        viewModel.weather.observe(viewLifecycleOwner, ::render)

        viewModel.actions.value = Action.Refresh
    }

    // endregion

    // region Render View

    private fun render(weatherData: WeatherState) {
        updateProgressSpinner(weatherData.refreshing)
        populateWeatherFields(weatherData)
        populateLastUpdatedField(weatherData)
        loadWeatherIcon(weatherData)
    }

    private fun populateWeatherFields(weatherData: WeatherState) {
        binding.valueCurrentCondition.text = weatherData.currentCondition
        binding.valueWindDirection.text = weatherData.windDirection
        binding.valueTemperature.text = weatherData.temperature
        binding.valueWindSpeed.text = weatherData.windSpeed
    }

    private fun populateLastUpdatedField(weatherData: WeatherState) {
        weatherData.updated?.atZone(ZoneId.systemDefault())?.let {
            binding.lastUpdated.text = getString(
                R.string.last_updated,
                it.format(
                    DateTimeFormatter.ofLocalizedDateTime(
                        FormatStyle.MEDIUM,
                        FormatStyle.SHORT
                    )
                )
            )
        }
    }

    // endregion

    // region Snackbar

    //showSnackbar(getString(R.string.no_internet_connection))

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
                binding.progressSpinner.visibility = GONE
            }
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

    // region Companion

    companion object {
        fun newInstance() = WeatherFragment()
    }

    // endregion
}