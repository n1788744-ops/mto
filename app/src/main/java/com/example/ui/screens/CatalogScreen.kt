package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Apartment
import com.example.data.DealType
import com.example.data.RegionFilter
import com.example.ui.components.ApartmentCard
import com.example.ui.viewmodel.CatalogFilterState

@Composable
fun CatalogScreen(
  apartments: List<Apartment>,
  filters: CatalogFilterState,
  favoriteIds: Set<String>,
  onSearchChange: (String) -> Unit,
  onSelectRegion: (RegionFilter) -> Unit,
  onSelectDealType: (DealType?) -> Unit,
  onSelectRooms: (Int?) -> Unit,
  onResetFilters: () -> Unit,
  onOpenMap: () -> Unit,
  onOpenAi: () -> Unit,
  onToggleFavorite: (Apartment) -> Unit,
  onOpenApartment: (Apartment) -> Unit,
  onPayApartment: (Apartment) -> Unit,
) {
  var showExtraFilters by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Квартиры ЮгДом",
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
              text = "Мелитополь · Херсонская · Запорожская обл.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier
                .clickable(onClick = onOpenAi)
                .testTag("catalog_ai_shortcut"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "ИИ об условиях",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Условия ИИ",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
          }
        }

        // Search input + filter toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = filters.searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Город, улица или ЖК...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
              if (filters.searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchChange("") }) {
                  Icon(Icons.Filled.Clear, contentDescription = "Очистить поиск")
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.weight(1f).testTag("catalog_search_input"),
          )

          Surface(
            color =
              if (showExtraFilters) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 1.dp,
          ) {
            IconButton(
              onClick = { showExtraFilters = !showExtraFilters },
              modifier = Modifier.size(52.dp).testTag("toggle_extra_filters_btn"),
            ) {
              Icon(
                imageVector = Icons.Filled.Tune,
                contentDescription = "Дополнительные фильтры",
                tint =
                  if (showExtraFilters) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }

        // Location filter pills
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          RegionFilter.entries.forEach { region ->
            val selected = filters.selectedRegion == region
            FilterChip(
              selected = selected,
              onClick = { onSelectRegion(region) },
              label = { Text(region.shortName) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primary,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
              modifier = Modifier.testTag("catalog_region_${region.code}"),
            )
          }
        }

        // Deal type row
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = filters.selectedDealType == null,
            onClick = { onSelectDealType(null) },
            label = { Text("Все сделки") },
            modifier = Modifier.testTag("deal_filter_all"),
          )
          DealType.entries.forEach { deal ->
            FilterChip(
              selected = filters.selectedDealType == deal,
              onClick = {
                onSelectDealType(if (filters.selectedDealType == deal) null else deal)
              },
              label = { Text(deal.displayName) },
              modifier = Modifier.testTag("deal_filter_${deal.code}"),
            )
          }
        }

        if (showExtraFilters) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = "Количество комнат",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              val roomOptions = listOf(null to "Любое", 0 to "Студия", 1 to "1-комн.", 2 to "2-комн.", 3 to "3+ комн.")
              roomOptions.forEach { (roomsVal, label) ->
                FilterChip(
                  selected = filters.selectedRooms == roomsVal,
                  onClick = { onSelectRooms(roomsVal) },
                  label = { Text(label) },
                  modifier = Modifier.testTag("rooms_filter_${roomsVal ?: "any"}"),
                )
              }
            }
          }
        }

        // Quick Map Banner
        Surface(
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenMap).testTag("open_map_banner"),
        ) {
          Box(
            modifier =
              Modifier
                .fillMaxWidth()
                .background(
                  Brush.horizontalGradient(
                    listOf(
                      MaterialTheme.colorScheme.secondary,
                      MaterialTheme.colorScheme.secondary.copy(alpha = 0.82f),
                    )
                  )
                )
                .padding(16.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Поиск по карте и радиусу",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSecondary,
                  fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Найдено объектов: ${apartments.size} · Выбрать район на карте",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.85f),
                )
              }
              Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Filled.Map,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Карта",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                  )
                }
              }
            }
          }
        }
      }
    }

    if (apartments.isEmpty()) {
      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "По выбранным фильтрам квартир не найдено",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "Попробуйте изменить локацию, тип сделки или сбросить фильтры.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
              onClick = onResetFilters,
              modifier = Modifier.heightIn(min = 48.dp).testTag("reset_filters_btn"),
            ) {
              Text("Сбросить фильтры")
            }
          }
        }
      }
    } else {
      items(apartments, key = { it.id }) { apartment ->
        ApartmentCard(
          apartment = apartment,
          isFavorite = favoriteIds.contains(apartment.id),
          onToggleFavorite = { onToggleFavorite(apartment) },
          onOpenDetail = { onOpenApartment(apartment) },
          onPayClick = { onPayApartment(apartment) },
        )
      }
    }
  }
}
