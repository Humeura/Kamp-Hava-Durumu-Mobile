package com.kampplus.hava.ui.forecast

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kampplus.hava.R
import com.kampplus.hava.model.CurrentWeather
import com.kampplus.hava.model.DailyForecast
import com.kampplus.hava.model.Forecast
import com.kampplus.hava.model.HourlyForecast
import com.kampplus.hava.model.WeatherCondition
import com.kampplus.hava.ui.LoadState
import com.kampplus.hava.ui.component.ErrorView
import com.kampplus.hava.ui.theme.HavaTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Tahmin ekranının bağlayıcı katmanı; ViewModel'i edinir ve state'i aktarır.
 * (Bkz. [com.kampplus.hava.ui.city.CityRoute] — aynı desen.)
 */
@Composable
fun ForecastRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: ForecastViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val geminiText by viewModel.geminiText.collectAsStateWithLifecycle()

    ForecastScreen(
        cityName = viewModel.cityName,
        subtitle = viewModel.subtitle,
        uiState = uiState,
        geminiText = geminiText,
        onBack = onBack,
        onRetry = viewModel::onRetry,
        modifier = modifier
    )
}

/**
 * Bir şehrin hava tahmini.
 *
 * CityScreen ile aynı weather_wallpaper arka planı + gradient overlay kullanılır.
 * Tüm kartlar glassmorphism (şeffaf + blur benzeri) stilde çizilir.
 */
@Composable
fun ForecastScreen(
    cityName: String,
    subtitle: String,
    uiState: LoadState<Forecast>,
    geminiText: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // --- Arka Plan Wallpaper (CityScreen ile aynı) ---
        Image(
            painter = painterResource(id = R.drawable.weather_wallpaper),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Hafif siyah gradient overlay — okunabilirliği artırır
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.30f),
                            Color.Black.copy(alpha = 0.10f),
                            Color.Black.copy(alpha = 0.60f)
                        )
                    )
                )
        )

        // --- İçerik ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Üst Bar: Geri butonu + şehir adı
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = Color.White
                    )
                }
                Text(
                    text = cityName.ifEmpty { stringResource(R.string.detail_title) },
                    style = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            // İçerik alanı
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (uiState) {
                    LoadState.Loading -> CircularProgressIndicator(color = Color.White)
                    LoadState.Empty   -> ErrorView(message = "Bu şehir için tahmin bulunamadı.")
                    is LoadState.Error  -> ErrorView(message = uiState.message, onRetry = onRetry)
                    is LoadState.Success -> ForecastContent(subtitle = subtitle, forecast = uiState.data, geminiText = geminiText)
                }
            }
        }
    }
}

@Composable
private fun ForecastContent(subtitle: String, forecast: Forecast, geminiText: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CurrentWeatherHeader(
            subtitle = subtitle,
            current = forecast.current
        )
        SectionTitle(stringResource(R.string.detail_hourly))
        HourlyRow(hours = forecast.hourly)
        SectionTitle(stringResource(R.string.detail_daily))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            forecast.daily.forEachIndexed { index, day ->
                if (index > 0) HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                DailyRow(day = day, isToday = index == 0)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        GeminiAiCard(geminiText ?: "✨ Gemini öneri hazırlıyor...")
    }
}

@Composable
private fun CurrentWeatherHeader(subtitle: String, current: CurrentWeather, modifier: Modifier = Modifier) {
    val condition = WeatherCondition.fromWeatherCode(current.weatherCode)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = subtitle,
            style = TextStyle(fontSize = 14.sp, color = Color.White.copy(alpha = 0.80f))
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = condition.emoji, style = MaterialTheme.typography.displayMedium)
            Column {
                Text(
                    text = degrees(current.temperatureC),
                    style = TextStyle(fontSize = 52.sp, fontWeight = FontWeight.Bold, color = Color.White)
                )
                Text(
                    text = condition.label,
                    style = TextStyle(fontSize = 18.sp, color = Color.White.copy(alpha = 0.85f))
                )
            }
        }

        // Hissedilen / Nem / Rüzgâr — glassmorphism kutu
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                current.apparentTemperatureC?.let {
                    Metric(stringResource(R.string.detail_feels_like), degrees(it))
                }
                current.humidityPercent?.let {
                    Metric(stringResource(R.string.detail_humidity), "%$it")
                }
                current.windSpeedKmh?.let {
                    Metric(stringResource(R.string.detail_wind), "${it.roundToInt()} km/sa")
                }
            }
        }
    }
}

/**
 * Önümüzdeki 24 saat, yatay kaydırılabilir glassmorphism kartlar hâlinde.
 */
@Composable
private fun HourlyRow(hours: List<HourlyForecast>, modifier: Modifier = Modifier) {
    val now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
    val upcoming = hours.filter { !it.time.isBefore(now) }.take(HOURLY_COUNT)
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = upcoming, key = { it.time.toString() }) { hour ->
            GlassCard(modifier = Modifier.width(76.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = hour.time.format(HOUR_FORMAT),
                        style = TextStyle(fontSize = 12.sp, color = Color.White.copy(alpha = 0.80f))
                    )
                    Text(
                        text = WeatherCondition.fromWeatherCode(hour.weatherCode).emoji,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = degrees(hour.temperatureC),
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    )
                    Text(
                        text = percent(hour.precipitationProbability) ?: " ",
                        style = TextStyle(fontSize = 11.sp, color = Color.Cyan.copy(alpha = 0.85f))
                    )
                }
            }
        }
    }
}

/** Günlük tahmin satırı: gün adı · yağış · emoji · en düşük / en yüksek. */
@Composable
private fun DailyRow(day: DailyForecast, isToday: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isToday) stringResource(R.string.today)
                   else day.date.format(DAY_FORMAT).replaceFirstChar(Char::titlecase),
            style = TextStyle(fontSize = 15.sp, color = Color.White),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = percent(day.precipitationProbability).orEmpty(),
            style = TextStyle(fontSize = 12.sp, color = Color.Cyan.copy(alpha = 0.85f)),
            modifier = Modifier.width(44.dp),
            textAlign = TextAlign.End
        )
        Text(
            text = WeatherCondition.fromWeatherCode(day.weatherCode).emoji,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Text(
            text = degrees(day.minTemperatureC),
            style = TextStyle(fontSize = 15.sp, color = Color.White.copy(alpha = 0.60f)),
            modifier = Modifier.width(44.dp),
            textAlign = TextAlign.End
        )
        Text(
            text = degrees(day.maxTemperatureC),
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
            modifier = Modifier.width(44.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        )
        Text(
            text = label,
            style = TextStyle(fontSize = 12.sp, color = Color.White.copy(alpha = 0.70f))
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.85f)),
        modifier = Modifier.padding(horizontal = 0.dp)
    )
}

/**
 * Yeniden kullanılabilir glassmorphism kutu.
 *
 * Arka plan: %22 beyaz + %35 beyaz ince border.
 * İçerik [content] lambda olarak alınır; böylece herhangi bir içerik yerleştirilebilir.
 */
@Composable
public fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.20f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.35f), shape = shape)
    ) {
        content()
    }
}

/** 18.4 → "18°". Kullanıcıya ondalık göstermeye gerek yok. */
private fun degrees(celsius: Double) = "${celsius.roundToInt()}°"

/** Yağış olasılığı yalnızca sıfırdan büyükse anlamlıdır. */
private fun percent(value: Int?) = value?.takeIf { it > 0 }?.let { "%$it" }

private const val HOURLY_COUNT = 24
private val HOUR_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("tr"))

@Preview(showBackground = true)
@Composable
private fun ForecastScreenPreview() {
    HavaTheme {
        ForecastScreen(
            cityName = "Ankara",
            subtitle = "Ankara, Türkiye",
            geminiText = "Ankara bugün güneşli! Anıtkabir'e bir ziyaret harika olur. 🌤️",
            uiState = LoadState.Success(
                Forecast(
                    current = CurrentWeather(
                        temperatureC = 21.0,
                        weatherCode = 0,
                        observedAt = LocalDateTime.of(2026, 1, 15, 14, 0),
                        apparentTemperatureC = 20.0,
                        humidityPercent = 45,
                        windSpeedKmh = 12.0
                    ),
                    hourly = List(8) {
                        HourlyForecast(
                            time = LocalDateTime.now().plusHours(it.toLong()),
                            temperatureC = 18.0 + it,
                            weatherCode = 2,
                            precipitationProbability = it * 5
                        )
                    },
                    daily = List(7) {
                        DailyForecast(
                            date = LocalDate.of(2026, 1, 15).plusDays(it.toLong()),
                            minTemperatureC = 9.0 + it,
                            maxTemperatureC = 19.0 + it,
                            weatherCode = 61,
                            precipitationProbability = 40
                        )
                    }
                )
            ),
            onBack = {},
            onRetry = {}
        )
    }
}


