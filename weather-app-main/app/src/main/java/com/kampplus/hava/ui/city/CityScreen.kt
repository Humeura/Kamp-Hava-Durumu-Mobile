package com.kampplus.hava.ui.city

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kampplus.hava.R
import com.kampplus.hava.model.City
import com.kampplus.hava.ui.LoadState
import com.kampplus.hava.ui.component.EmptyView
import com.kampplus.hava.ui.component.ErrorView
import com.kampplus.hava.ui.component.ShimmerList
import com.kampplus.hava.ui.theme.HavaTheme

/**
 * Şehir arama ekranının "bağlayıcı" katmanı.
 *
 * Tek işi ViewModel'i edinip state'i toplamak ve [CityScreen]'e aktarmaktır.
 * Bu ayrım sayesinde [CityScreen] ViewModel tanımaz: @Preview'da ve testte
 * elle state verilerek çalıştırılabilir.
 *
 * `collectAsStateWithLifecycle`: StateFlow'u Compose state'ine
 * çevirir ve uygulama arka plandayken dinlemeyi durdurur.
 */
@Composable
fun CityRoute(onCityClick: (City) -> Unit, modifier: Modifier = Modifier, viewModel: CityViewModel = viewModel()) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CityScreen(
        query = query,
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::search,
        onCityClick = onCityClick,
        modifier = modifier
    )
}

/**
 * Şehir arama ekranı: tam ekran wallpaper arka plan, üstte başlık,
 * altta şeffaf/blur glassmorphism arama kutusu.
 *
 * State tutmaz, yalnızca aldığı değerleri çizer ve olayları yukarı bildirir.
 */
@Composable
fun CityScreen(
    query: String,
    uiState: LoadState<List<City>>,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onCityClick: (City) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // --- Arka Plan Wallpaper ---
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
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.10f),
                            Color.Black.copy(alpha = 0.55f)
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
            // Üst başlık
            Text(
                text = "Hava Durumu.",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
            )

            // Şehir listesi veya durum mesajı (listenin arka planı şeffaf)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (uiState) {
                    LoadState.Loading -> ShimmerList()
                    is LoadState.Success -> CityList(cities = uiState.data, onCityClick = onCityClick)
                    LoadState.Empty -> EmptyView(
                        title = "Şehir bulunamadı",
                        message = "Yazımı kontrol et ya da başka bir şehir dene."
                    )
                    is LoadState.Error -> ErrorView(message = uiState.message, onRetry = onSearch)
                }
            }

            // --- Alt Glassmorphism Search Bar ---
            GlassSearchBar(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            )
        }
    }
}

/**
 * Şeffaf + bulanık arka planlı arama kutusu (glassmorphism stili).
 */
@Composable
private fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    // Glassmorphism arka plan: %30 beyaz + ince beyaz border efekti
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.22f))
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = stringResource(R.string.search_hint),
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.action_clear),
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    onSearch()
                }
            ),
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 16.sp
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color.White
            )
        )
    }
}

/**
 * Kaydırılabilir şehir listesi.
 *
 * `LazyColumn` tembeldir: yüzlerce şehir olsa da yalnızca ekranda görünen satırlar
 * çizilir, kaybolanlar geri dönüştürülür.
 *
 * `key = { it.id }`: listedeki öğeler değiştiğinde Compose hangi satırın hangisi
 * olduğunu bilir; gereksiz yeniden çizim yapmaz.
 */
@Composable
private fun CityList(cities: List<City>, onCityClick: (City) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = cities, key = { it.id }) { city ->
            CityCard(city = city, onClick = { onCityClick(city) })
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CityScreenPreview() {
    HavaTheme {
        CityScreen(
            query = "Ankara",
            uiState = LoadState.Success(
                listOf(
                    City(323786, "Ankara", 39.91987, 32.85427, "TR", "Türkiye", "Ankara"),
                    City(738648, "Ankara", 41.0050, 39.7269, "TR", "Türkiye", "Trabzon")
                )
            ),
            onQueryChange = {},
            onSearch = {},
            onCityClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CityScreenEmptyPreview() {
    HavaTheme {
        CityScreen(
            query = "xyz",
            uiState = LoadState.Empty,
            onQueryChange = {},
            onSearch = {},
            onCityClick = {}
        )
    }
}
