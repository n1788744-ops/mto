package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Apartment
import com.example.data.DealType
import com.example.data.RegionFilter
import kotlin.math.hypot

@Composable
fun RegionalMapCanvas(
  apartments: List<Apartment>,
  selectedRegion: RegionFilter,
  selectedApartmentId: String?,
  radiusKm: Float,
  onSelectRegion: (RegionFilter) -> Unit,
  onSelectApartment: (Apartment) -> Unit,
  modifier: Modifier = Modifier,
) {
  var zoom by remember { mutableFloatStateOf(1f) }
  var pan by remember { mutableStateOf(Offset.Zero) }
  val textMeasurer = rememberTextMeasurer()

  val landColor = Color(0xFFF3ECE2)
  val parkColor = Color(0xFFDDE8D5)
  val waterColor = Color(0xFF9CD0E6)
  val deepWaterColor = Color(0xFF7CBAD6)
  val roadColor = Color(0xFFFFFFFF)
  val roadBorderColor = Color(0xFFD9CBB8)
  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = MaterialTheme.colorScheme.secondary
  val surfaceColor = MaterialTheme.colorScheme.surface
  val textColor = MaterialTheme.colorScheme.onSurface

  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(22.dp))
        .background(landColor)
        .testTag("regional_map_canvas")
  ) {
    Canvas(
      modifier =
        Modifier
          .fillMaxSize()
          .pointerInput(Unit) {
            detectTransformGestures { _, panChange, zoomChange, _ ->
              val nextZoom = (zoom * zoomChange).coerceIn(1f, 2.6f)
              zoom = nextZoom
              val maxPanX = size.width * (nextZoom - 1f) * 0.5f + 60f
              val maxPanY = size.height * (nextZoom - 1f) * 0.5f + 60f
              pan =
                Offset(
                  x = (pan.x + panChange.x).coerceIn(-maxPanX, maxPanX),
                  y = (pan.y + panChange.y).coerceIn(-maxPanY, maxPanY),
                )
            }
          }
          .pointerInput(apartments, zoom, pan) {
            detectTapGestures { tapOffset ->
              val w = size.width.toFloat()
              val h = size.height.toFloat()
              fun toScreen(nx: Float, ny: Float): Offset {
                val cx = w / 2f
                val cy = h / 2f
                return Offset(
                  x = cx + (nx * w - cx) * zoom + pan.x,
                  y = cy + (ny * h - cy) * zoom + pan.y,
                )
              }

              // First check apartment pins
              val hitApartment =
                apartments.minByOrNull { apt ->
                  val pos = toScreen(apt.mapX, apt.mapY)
                  hypot(tapOffset.x - pos.x, tapOffset.y - pos.y)
                }
              if (hitApartment != null) {
                val pos = toScreen(hitApartment.mapX, hitApartment.mapY)
                if (hypot(tapOffset.x - pos.x, tapOffset.y - pos.y) <= 64f) {
                  onSelectApartment(hitApartment)
                  return@detectTapGestures
                }
              }

              // Check region hub taps
              val hubs = listOf(RegionFilter.MELITOPOL, RegionFilter.KHERSON, RegionFilter.ZAPORIZHZHIA)
              val hitHub =
                hubs.firstOrNull { region ->
                  val pos = toScreen(region.mapX, region.mapY)
                  hypot(tapOffset.x - pos.x, tapOffset.y - pos.y) <= 84f
                }
              if (hitHub != null) {
                onSelectRegion(hitHub)
              }
            }
          }
    ) {
      val w = size.width
      val h = size.height
      val cx = w / 2f
      val cy = h / 2f

      fun mapPt(nx: Float, ny: Float): Offset =
        Offset(
          x = cx + (nx * w - cx) * zoom + pan.x,
          y = cy + (ny * h - cy) * zoom + pan.y,
        )

      // 1. Subtle coordinate grid
      for (i in 1..7) {
        val gx = mapPt(i / 8f, 0f).x
        drawLine(
          color = roadBorderColor.copy(alpha = 0.28f),
          start = Offset(gx, 0f),
          end = Offset(gx, h),
          strokeWidth = 1.dp.toPx(),
        )
        val gy = mapPt(0f, i / 8f).y
        drawLine(
          color = roadBorderColor.copy(alpha = 0.28f),
          start = Offset(0f, gy),
          end = Offset(w, gy),
          strokeWidth = 1.dp.toPx(),
        )
      }

      // 2. Dnieper / Kakhovka river & green zone (top-left to middle-left)
      val riverPath =
        Path().apply {
          val p0 = mapPt(0.0f, 0.36f)
          val p1 = mapPt(0.24f, 0.42f)
          val p2 = mapPt(0.42f, 0.25f)
          val p3 = mapPt(0.46f, 0.0f)
          moveTo(p0.x, p0.y)
          lineTo(p1.x, p1.y)
          lineTo(p2.x, p2.y)
          lineTo(p3.x, p3.y)
        }
      drawPath(
        path = riverPath,
        color = waterColor,
        style = Stroke(width = 16.dp.toPx() * zoom),
      )

      // 3. Azov Sea along bottom-right / bottom-center
      val seaPath =
        Path().apply {
          val s0 = mapPt(0.0f, 0.86f)
          val s1 = mapPt(0.22f, 0.78f)
          val s2 = mapPt(0.38f, 0.80f)
          val s3 = mapPt(0.54f, 0.66f)
          val s4 = mapPt(0.72f, 0.56f)
          val s5 = mapPt(0.86f, 0.51f)
          val s6 = mapPt(1.0f, 0.45f)
          moveTo(s0.x, s0.y)
          lineTo(s1.x, s1.y)
          lineTo(s2.x, s2.y)
          lineTo(s3.x, s3.y)
          lineTo(s4.x, s4.y)
          lineTo(s5.x, s5.y)
          lineTo(s6.x, s6.y)
          lineTo(w + 200f, h + 200f)
          lineTo(-200f, h + 200f)
          close()
        }
      drawPath(path = seaPath, color = waterColor)

      val seaHighlight =
        Path().apply {
          val s1 = mapPt(0.28f, 0.88f)
          val s2 = mapPt(0.62f, 0.72f)
          val s3 = mapPt(1.0f, 0.58f)
          moveTo(s1.x, s1.y)
          lineTo(s2.x, s2.y)
          lineTo(s3.x, s3.y)
          lineTo(w + 200f, h + 200f)
          lineTo(-200f, h + 200f)
          close()
        }
      drawPath(path = seaHighlight, color = deepWaterColor.copy(alpha = 0.45f))

      // 4. Green parks / nature reserves
      drawCircle(
        color = parkColor,
        radius = 28.dp.toPx() * zoom,
        center = mapPt(0.57f, 0.44f),
      )
      drawCircle(
        color = parkColor,
        radius = 34.dp.toPx() * zoom,
        center = mapPt(0.18f, 0.60f),
      )

      // 5. Major regional highways (M14 / E105)
      val highways =
        listOf(
          mapPt(0.08f, 0.68f) to mapPt(0.35f, 0.68f),
          mapPt(0.35f, 0.68f) to mapPt(0.52f, 0.41f),
          mapPt(0.52f, 0.41f) to mapPt(0.81f, 0.47f),
          mapPt(0.52f, 0.41f) to mapPt(0.45f, 0.16f),
          mapPt(0.21f, 0.44f) to mapPt(0.52f, 0.41f),
        )
      highways.forEach { (start, end) ->
        drawLine(color = roadBorderColor, start = start, end = end, strokeWidth = 6.dp.toPx())
        drawLine(color = roadColor, start = start, end = end, strokeWidth = 3.5.dp.toPx())
      }

      // 6. Search radius overlay around active region hub
      if (selectedRegion != RegionFilter.ALL && radiusKm < 150f) {
        val hubCenter = mapPt(selectedRegion.mapX, selectedRegion.mapY)
        val radiusPx = (radiusKm / 120f) * (w * 0.36f) * zoom
        drawCircle(
          color = primaryColor.copy(alpha = 0.12f),
          radius = radiusPx,
          center = hubCenter,
        )
        drawCircle(
          color = primaryColor.copy(alpha = 0.7f),
          radius = radiusPx,
          center = hubCenter,
          style =
            Stroke(
              width = 2.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f),
            ),
        )
      }

      // 7. Region hub labels
      val regionLabels =
        listOf(
          Triple(RegionFilter.MELITOPOL, "Мелитополь", Offset(0.52f, 0.33f)),
          Triple(RegionFilter.KHERSON, "Херсонская обл.", Offset(0.22f, 0.56f)),
          Triple(RegionFilter.ZAPORIZHZHIA, "Запорожская обл.", Offset(0.75f, 0.36f)),
        )
      regionLabels.forEach { (region, label, pos) ->
        val pt = mapPt(pos.x, pos.y)
        val isActive = selectedRegion == region
        val measured =
          textMeasurer.measure(
            text = label,
            style =
              TextStyle(
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isActive) primaryColor else secondaryColor,
              ),
          )
        val pillW = measured.size.width + 20f
        val pillH = measured.size.height + 10f
        drawRoundRect(
          color =
            if (isActive) surfaceColor.copy(alpha = 0.96f)
            else surfaceColor.copy(alpha = 0.80f),
          topLeft = Offset(pt.x - pillW / 2f, pt.y - pillH / 2f),
          size = Size(pillW, pillH),
          cornerRadius = CornerRadius(14f, 14f),
        )
        drawText(
          textLayoutResult = measured,
          topLeft = Offset(pt.x - measured.size.width / 2f, pt.y - measured.size.height / 2f),
        )
      }

      // Sea label
      val seaLabel =
        textMeasurer.measure(
          text = "Азовское море",
          style =
            TextStyle(
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              color = Color(0xFF1A5770),
            ),
        )
      val seaPt = mapPt(0.76f, 0.84f)
      drawText(
        textLayoutResult = seaLabel,
        topLeft = Offset(seaPt.x - seaLabel.size.width / 2f, seaPt.y - seaLabel.size.height / 2f),
      )

      // 8. Apartment price pins
      apartments.forEach { apt ->
        val pinPos = mapPt(apt.mapX, apt.mapY)
        val isSelected = apt.id == selectedApartmentId
        val shortPrice =
          if (apt.dealType == DealType.RENT) "${apt.priceRub / 1000}т"
          else "${String.format("%.1f", apt.priceRub / 1_000_000.0)}М"
        val pinColor =
          when {
            isSelected -> textColor
            apt.dealType == DealType.RENT -> secondaryColor
            else -> primaryColor
          }
        val measuredPrice =
          textMeasurer.measure(
            text = shortPrice,
            style =
              TextStyle(
                fontSize = if (isSelected) 12.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
              ),
          )
        val badgeW = (measuredPrice.size.width + 22f).coerceAtLeast(52f)
        val badgeH = (measuredPrice.size.height + 12f).coerceAtLeast(32f)

        // Shadow
        drawRoundRect(
          color = Color.Black.copy(alpha = 0.22f),
          topLeft = Offset(pinPos.x - badgeW / 2f, pinPos.y - badgeH - 4f),
          size = Size(badgeW, badgeH),
          cornerRadius = CornerRadius(18f, 18f),
        )
        // Pin body
        drawRoundRect(
          color = pinColor,
          topLeft = Offset(pinPos.x - badgeW / 2f, pinPos.y - badgeH - 7f),
          size = Size(badgeW, badgeH),
          cornerRadius = CornerRadius(18f, 18f),
        )
        if (isSelected) {
          drawRoundRect(
            color = Color.White,
            topLeft = Offset(pinPos.x - badgeW / 2f, pinPos.y - badgeH - 7f),
            size = Size(badgeW, badgeH),
            cornerRadius = CornerRadius(18f, 18f),
            style = Stroke(width = 2.5.dp.toPx()),
          )
        }
        // Pin stem dot
        drawCircle(color = pinColor, radius = 4.dp.toPx(), center = pinPos)
        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pinPos)

        drawText(
          textLayoutResult = measuredPrice,
          topLeft =
            Offset(
              x = pinPos.x - measuredPrice.size.width / 2f,
              y = pinPos.y - badgeH - 7f + (badgeH - measuredPrice.size.height) / 2f,
            ),
        )
      }
    }

    // Zoom & reset controls
    Column(
      modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Surface(shape = CircleShape, shadowElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
        IconButton(
          onClick = { zoom = (zoom + 0.35f).coerceAtMost(2.6f) },
          modifier = Modifier.size(48.dp).testTag("map_zoom_in_btn"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = "Увеличить карту")
        }
      }
      Surface(shape = CircleShape, shadowElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
        IconButton(
          onClick = { zoom = (zoom - 0.35f).coerceAtLeast(1f) },
          modifier = Modifier.size(48.dp).testTag("map_zoom_out_btn"),
        ) {
          Icon(Icons.Filled.Remove, contentDescription = "Уменьшить карту")
        }
      }
      Surface(shape = CircleShape, shadowElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
        IconButton(
          onClick = {
            zoom = 1f
            pan = Offset.Zero
          },
          modifier = Modifier.size(48.dp).testTag("map_reset_btn"),
        ) {
          Icon(Icons.Filled.MyLocation, contentDescription = "Сбросить масштаб карты")
        }
      }
    }

    // Map legend at top-left
    Surface(
      modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
      shadowElevation = 2.dp,
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        LegendDot(color = MaterialTheme.colorScheme.secondary, label = "Аренда")
        LegendDot(color = MaterialTheme.colorScheme.primary, label = "Продажа")
      }
    }
  }
}

@Composable
private fun LegendDot(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(color))
    Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
  }
}
