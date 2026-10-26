package com.kampplus.hava.ui.city

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kampplus.hava.model.City
import com.kampplus.hava.ui.theme.HavaTheme

/**
 * Listedeki tek bir şehir satırı — glassmorphism stili.
 *
 * Yapısı: `Box(glass)` → `Row` → rozet + `Column(weight(1f))` + sağ ok.
 *
 * `Modifier.weight(1f)`: ortadaki sütun kalan boşluğun tamamını kaplar, böylece
 * sağ oku sağ kenara iter. Uzun şehir adları (Afyonkarahisar) rozeti ezmez.
 *
 * @param onClick Karta dokunulduğunda çağrılır. Bileşen ne olacağını bilmez,
 *   yalnızca haber verir — olay yukarı, veri aşağı (state hoisting).
 */
@Composable
fun CityCard(city: City, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val glassShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(glassShape)
            .background(Color.White.copy(alpha = 0.22f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.35f),
                shape = glassShape
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CountryBadge(code = city.countryCode)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = city.name,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    ),
                    // Tek satıra sığmayan ad "..." ile kısaltılır, satır kaymaz.
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = city.subtitle,
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                // Ok yalnızca görseldir; "dokunulabilir" bilgisini Card zaten verir.
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

/**
 * Ülke kodu rozeti ("TR") — glassmorphism stili.
 *
 * Veri API'den geldiği için elimizde plaka numarası yok; rozete ülke kodunu
 * koyuyoruz. Kod gelmezse "?" gösterilir.
 */
@Composable
private fun CountryBadge(code: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.28f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = code ?: "?",
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF4A7A8A)
@Composable
private fun CityCardPreview() {
    HavaTheme {
        CityCard(
            city = City(
                id = 323786,
                name = "Ankara",
                latitude = 39.91987,
                longitude = 32.85427,
                countryCode = "TR",
                country = "Türkiye",
                admin1 = "Ankara"
            ),
            onClick = {}
        )
    }
}
