package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Apartment
import com.example.data.DealType
import com.example.data.RegionFilter
import com.example.data.formatRub
import com.example.ui.components.RegionalMapCanvas
import com.example.ui.viewmodel.CatalogFilterState
import kotlin.math.roundToInt

@Composable
fun MapSearchScreen(
  apartments: List<Apartment>,
  filters: CatalogFilterState,
  onSelectRegion: (RegionFilter) -> Unit,
  onSelectDealType: (DealType?) -> Unit,
  onRadiusChange: (Float) -> Unit,
  onSelectPin: (String?) -> Unit,
  onOpenApartment: (Apartment) -> Unit,
  onPayApartment: (Apartment) -> Unit,
) {
  val selectedApartment =
    apartments.firstOrNull { it.id == filters.selectedMapApartmentId }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text(
      text = "Карта и фильтры по локациям",
      style = MaterialTheme.typography.headlineMedium,
      color = MaterialTheme.colorScheme.onBackground,
    )

    // Location & Deal filter row
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      RegionFilter.entries.forEach { region ->
        FilterChip(
          selected = filters.selectedRegion == region,
          onClick = { onSelectRegion(region) },
          label = { Text(region.shortName) },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
          modifier = Modifier.testTag("map_region_${region.code}"),
        )
      }
      DealType.entries.forEach { deal ->
        FilterChip(
          selected = filters.selectedDealType == deal,
          onClick = {
            onSelectDealType(if (filters.selectedDealType == deal) null else deal)
          },
          label = { Text(deal.displayName) },
          modifier = Modifier.testTag("map_deal_${deal.code}"),
        )
      }
    }

    if (filters.selectedRegion != RegionFilter.ALL) {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
          val radiusLabel =
            if (filters.mapRadiusKm >= 150f) "Весь регион"
            else "Радиус от центра (${filters.selectedRegion.shortName}): ${filters.mapRadiusKm.roundToInt()} км"
          Text(
            text = radiusLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Slider(
            value = filters.mapRadiusKm,
            onValueChange = onRadiusChange,
            valueRange = 15f..150f,
            modifier = Modifier.testTag("map_radius_slider"),
          )
        }
      }
    }

    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
      RegionalMapCanvas(
        apartments = apartments,
        selectedRegion = filters.selectedRegion,
        selectedApartmentId = filters.selectedMapApartmentId,
        radiusKm = filters.mapRadiusKm,
        onSelectRegion = onSelectRegion,
        onSelectApartment = { apt -> onSelectPin(apt.id) },
        modifier = Modifier.fillMaxSize(),
      )

      // Bottom floating apartment preview card
      this@Column.AnimatedVisibility(
        visible = selectedApartment != null,
        modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp),
      ) {
        if (selectedApartment != null) {
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth().testTag("map_selected_preview"),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Image(
                  painter = painterResource(id = selectedApartment.photos.first().resId),
                  contentDescription = selectedApartment.title,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)),
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${formatRub(selectedApartment.priceRub)} ${selectedApartment.dealType.priceSuffix}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  Text(
                    text = selectedApartment.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = selectedApartment.address,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                IconButton(
                  onClick = { onSelectPin(null) },
                  modifier = Modifier.size(48.dp).testTag("close_map_preview_btn"),
                ) {
                  Icon(Icons.Filled.Close, contentDescription = "Закрыть карточку")
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedButton(
                  onClick = { onOpenApartment(selectedApartment) },
                  modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("map_preview_detail_btn"),
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Text("Фото и детали")
                }
                Button(
                  onClick = { onPayApartment(selectedApartment) },
                  modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("map_preview_pay_btn"),
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Платить")
                }
              }
            }
          }
        }
      }
    }

    if (selectedApartment == null) {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = "На карте показано объектов: ${apartments.size}. Нажмите на город или ценник, чтобы открыть квартиру.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(12.dp),
        )
      }
    }
  }
}
