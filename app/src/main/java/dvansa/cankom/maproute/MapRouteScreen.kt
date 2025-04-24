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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
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
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import dvansa.cankom.meteo.MeteoSwissClient
import dvansa.cankom.meteo.PrecipitationRegion
import dvansa.cankom.useredit.TimePoint
import kotlinx.coroutines.launch


data class InitialState(
    val route : dvansa.cankom.model.MapPath
)

val MAP_INTENSITY_TO_COLOR : Map<Int, Color> = mapOf(
    0 to Color(0xFF9A7E95),
    1 to Color(0xFF0001fC),
    2 to Color(0xFF058C2D),
    3 to Color(0xFF05FF05),
    4 to Color(0xFFfEFF01),
    5 to Color(0xFFFFC703),
    6 to Color(0xFFFF7D01),
    7 to Color(0xFFFF0000),
    8 to Color(0xFFFF00FF)
)

@Composable
fun MapRouteScreen(
    initialState : InitialState,
    onSaveRoute : (newRoute : dvansa.cankom.model.MapPath) -> Unit,
    onBack: () -> Unit,
    checkPrecipitationCommute: suspend () -> List<PrecipitationRegion>,
    modifier: Modifier = Modifier,
    viewModel: MapRouteViewModel = MapRouteViewModel(
        MapRouteUiState(route=initialState.route.map {
            LatLng(it.latitude, it.longitude)
        })
    )
) {
    Scaffold(modifier = modifier.fillMaxSize()) { paddingValues ->
        Column(modifier = modifier.padding(paddingValues)) {
            val uiState by viewModel.uiState.collectAsState();
            Box(contentAlignment = Alignment.TopStart) {
                IconButton(onClick = {
                    onSaveRoute(/*newRoute=*/uiState.route.map{ dvansa.cankom.model.LatLng(it.latitude, it.longitude)})
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }

            // TODO. Remove, only used to test Meteo API
            Button(onClick = {

                viewModel.viewModelScope.launch {
                    try {
                        val closePrecipitationRegions = checkPrecipitationCommute()
                        viewModel.setPrecipitationRegions(closePrecipitationRegions.filter {it.intensity > -1})

//                        val client = MeteoSwissClient()
//                        val response = client.getPrecipitationRadarData(2025, 4, 21, 15, 0)
//
//                        println("Precipitation num regions ${response.size}")
//                        println("Precipitation region 0 polygon:  ${response[0].polygon}")
//
//                        viewModel.setPrecipitationRegions(response.filter {it.intensity > -1})
                    } catch (e: Exception) {
                        println("Http error while getting radar data: ${e.message}")
                    }
                }

            }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Test Query")
            }

            // Initial position at Zürich
            val startPosition = LatLng(47.3769, 8.54173)
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(startPosition, 14.5f)
            }
            Text(text = "Long Click: reset route. Click: add waypoint.")
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = true),
                onMapLongClick = {latLng -> viewModel.resetRoute(); },
                onMapClick = { latLng -> viewModel.addRoutePoint(latLng)}
            ) {

                uiState.precipitationRegions.forEach{
                    region : PrecipitationRegion ->
                    Polygon(points= region.polygon.map { LatLng(it.latitude, it.longitude)},fillColor = MAP_INTENSITY_TO_COLOR.get(region.intensity)!! )
                }

                Polyline(
                    points=uiState.route,
                    color = Color(0xFFAB3DEB),
                    width = 11.0f
                )
                if (!uiState.route.isEmpty()) {
                    Circle(center = uiState.route.get(0),
                    fillColor = Color(0xFFAB3DEB),
                    strokeColor = Color(0xFFAB3DEB),
                    radius = 21.0)
                }
            }
        }
    }
}