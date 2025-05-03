/*
* MIT License
*
* Copyright (c) 2025 Daniel van Sabben Alsina
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*/
package dvansa.cankom.maproute

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import dvansa.cankom.R
import dvansa.cankom.meteo.PrecipitationRegion
import dvansa.cankom.model.MapPath
import kotlinx.coroutines.launch

data class InitialState(
    val route: dvansa.cankom.model.MapPath,
)

val MAP_INTENSITY_TO_COLOR: Map<Int, Color> =
    mapOf(
        0 to Color(0xDD9A7E95),
        1 to Color(0xDD0001fC),
        2 to Color(0xDD058C2D),
        3 to Color(0xDD05FF05),
        4 to Color(0xDDfEFF01),
        5 to Color(0xDDFFC703),
        6 to Color(0xDDFF7D01),
        7 to Color(0xDDFF0000),
        8 to Color(0xDDFF00FF),
    )

val INTERSECTING_REGIONS_COLOR = Color(0xDDFF0000)

@Composable
fun MapRouteScreen(
    initialState: InitialState,
    onSaveRoute: (MapPath) -> Unit,
    onBack: () -> Unit,
    checkPrecipitationCommute: suspend () -> Pair<List<PrecipitationRegion>, List<PrecipitationRegion>>?,
    checkTemperatureCommute: suspend (Context) -> Pair<Int, Int>?,
    modifier: Modifier = Modifier,
    viewModel: MapRouteViewModel =
        MapRouteViewModel(
            MapRouteUiState(
                route =
                    initialState.route.map {
                        LatLng(it.latitude, it.longitude)
                    },
            ),
        ),
) {
    Scaffold(modifier = modifier.fillMaxSize()) { paddingValues ->
        Column(modifier = modifier.padding(paddingValues)) {
            val uiState by viewModel.uiState.collectAsState()
            Box(contentAlignment = Alignment.TopStart) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        onSaveRoute( // newRoute=
                            uiState.route.map {
                                dvansa.cankom.model.LatLng(
                                    it.latitude,
                                    it.longitude,
                                )
                            },
                        )
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        "Route",
                        fontSize = dimensionResource(R.dimen.title_font_size).value.sp,
                        modifier = Modifier.padding(dimensionResource(R.dimen.horizontal_margin)),
                    )
                }
            }

            // Separator
            HorizontalDivider(
                color = Color.Gray,
                thickness = 1.dp,
            )

            // Initial position at Zürich
            val startPosition = LatLng(47.3769, 8.54173)
            val cameraPositionState =
                rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(startPosition, 14.5f)
                }
            Row {
                Column {
                    Text(
                        text = "Click: add waypoint.  ",
                        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.horizontal_small_margin)),
                    )
                    Text(
                        text = "Long Click: reset route.",
                        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.horizontal_small_margin)),
                    )
                }
                // Check precipitation
                Button(onClick = {
                    viewModel.viewModelScope.launch {
                        try {
                            val (intersectingPrecipitationRegions, closePrecipitationRegions) =
                                checkPrecipitationCommute().let {
                                    it ?: Pair<List<PrecipitationRegion>, List<PrecipitationRegion>>(
                                        listOf(),
                                        listOf(),
                                    )
                                }
                            viewModel.setPrecipitationRegions(
                                intersectingPrecipitationRegions,
                                closePrecipitationRegions,
                            )
                        } catch (e: Exception) {
                            println("Http error while getting radar data: ${e.message}")
                        }
                    }
                }, modifier = Modifier.padding(dimensionResource(R.dimen.horizontal_small_margin))) {
                    Image(
                        painter = painterResource(id = R.drawable.rainy_24px),
                        contentDescription = "Check precipitations",
                    )
                }
            }
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = true),
                onMapLongClick = { latLng ->
                    viewModel.resetRoute()
                    viewModel.setPrecipitationRegions(
                        listOf(),
                        listOf(),
                    )
                },
                onMapClick = { latLng ->
                    viewModel.addRoutePoint(latLng)
                    onSaveRoute(/*newRoute=*/uiState.route.map { dvansa.cankom.model.LatLng(it.latitude, it.longitude) })
                },
            ) {
                // Precipitation regions
                uiState.closePrecipitationRegions.forEach { region: PrecipitationRegion ->
                    Polygon(
                        points = region.polygon.map { LatLng(it.latitude, it.longitude) },
                        fillColor =
                            MAP_INTENSITY_TO_COLOR.get(region.intensity) ?: Color(0xFFFFFFFF),
                        strokeWidth = 0.0f,
                    )
                }
                uiState.intersectingPrecipitationRegions.forEach { region: PrecipitationRegion ->
                    Polygon(
                        points = region.polygon.map { LatLng(it.latitude, it.longitude) },
                        fillColor = INTERSECTING_REGIONS_COLOR,
                        strokeWidth = 0.0f,
                    )
                }

                // Commute route
                Polyline(
                    points = uiState.route,
                    color = Color(0xFFC04DD1),
                    width = 11.0f,
                )
                if (!uiState.route.isEmpty()) {
                    Circle(
                        center = uiState.route.get(0),
                        fillColor = Color(0xFFC04DD1),
                        strokeColor = Color(0xFFC04DD1),
                        radius = 21.0,
                    )
                }
            }
        }
    }
}
