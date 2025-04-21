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


import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import dvansa.cankom.meteo.PrecipitationRegion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MapRouteUiState(
    val route: List<LatLng> = listOf(),
    val precipitationRegions : List<PrecipitationRegion> = listOf()
)

class MapRouteViewModel (mapRouteUiState : MapRouteUiState = MapRouteUiState()) : ViewModel() {

    private val _uiState = MutableStateFlow<MapRouteUiState>(mapRouteUiState);
    val uiState: StateFlow<MapRouteUiState> get() = _uiState;

    fun resetRoute() {
        _uiState.value = _uiState.value.copy(route = listOf());
    }

    fun addRoutePoint(point : LatLng) {
        val route : MutableList<LatLng> = _uiState.value.route.toMutableList();
        route.add(point)
        _uiState.value = _uiState.value.copy(route = route);
    }

    fun setPrecipitationRegions(regions : List<PrecipitationRegion>) {
        _uiState.value = _uiState.value.copy(precipitationRegions = regions);
    }

}


