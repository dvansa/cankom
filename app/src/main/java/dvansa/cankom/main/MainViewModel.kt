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
package dvansa.cankom.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class MainUiState(
    val isRefreshing: Boolean = false,
    val commuteWarnings: List<CommuteWarning> = listOf(),
    val nextCommuteTime: LocalDateTime? = null,
)

enum class CommuteState {
    ROUTE_UNKNOWN,
    TEMPERATURE_COLD,
    TEMPERATURE_HOT,
    TEMPERATURE_UNKNOWN,
    PRECIPITATION,
    PRECIPITATION_UNKNOWN,
}

data class CommuteWarning(
    val state: CommuteState,
    val param1: Float = 0.0f,
    val param2: Float = 0.0f,
)

class MainViewModel(
    getAllowedTemperatureRange: () -> Pair<Int, Int>,
    getCommuteTemperatureRange: suspend () -> Pair<Int, Int>?,
    checkPrecipitationsInRoute: suspend () -> Boolean?,
    checkRouteAvailable: suspend () -> Boolean,
    getNextCommuteTime: () -> LocalDateTime,
    mainUiState: MainUiState = MainUiState(),
) : ViewModel() {
    private val _getAllowedTemperatureRange = getAllowedTemperatureRange
    private val _getCommuteTemperatureRange = getCommuteTemperatureRange
    private val _checkPrecipitationsInRoute = checkPrecipitationsInRoute
    private val _checkRouteAvailable = checkRouteAvailable
    private val _getNextCommuteTime = getNextCommuteTime

    private val _uiState = MutableStateFlow<MainUiState>(mainUiState)
    val uiState: StateFlow<MainUiState> get() = _uiState

    fun refreshCommuteState() {
        viewModelScope.launch {
            val commuteWarnings: MutableList<CommuteWarning> = mutableListOf()

            if (!_checkRouteAvailable()) {
                commuteWarnings.add(CommuteWarning(CommuteState.ROUTE_UNKNOWN))
            } else {
                // Temperature
                val temperatureRange = _getCommuteTemperatureRange()
                if (temperatureRange == null) {
                    commuteWarnings.add(CommuteWarning(CommuteState.TEMPERATURE_UNKNOWN))
                } else {
                    val allowedTemperatureRange = _getAllowedTemperatureRange()
                    println("Allowed temperature $allowedTemperatureRange - queried $temperatureRange")
                    if (temperatureRange.first < allowedTemperatureRange.first) {
                        commuteWarnings.add(
                            CommuteWarning(
                                CommuteState.TEMPERATURE_COLD,
                                temperatureRange.first.toFloat(),
                                allowedTemperatureRange.first.toFloat(),
                            ),
                        )
                    }
                    if (temperatureRange.second > allowedTemperatureRange.second) {
                        commuteWarnings.add(
                            CommuteWarning(
                                CommuteState.TEMPERATURE_HOT,
                                temperatureRange.second.toFloat(),
                                allowedTemperatureRange.second.toFloat(),
                            ),
                        )
                    }
                }

                // Precipitation
                val precipitationsInRoute = _checkPrecipitationsInRoute()
                if (precipitationsInRoute == null) {
                    commuteWarnings.add(CommuteWarning(CommuteState.PRECIPITATION_UNKNOWN))
                } else if (precipitationsInRoute) {
                    commuteWarnings.add(CommuteWarning(CommuteState.PRECIPITATION))
                }
            }

            _uiState.value = _uiState.value.copy(commuteWarnings = commuteWarnings.toList(), nextCommuteTime = _getNextCommuteTime())
        }
    }

    fun updateRefreshing(isRefreshing: Boolean) {
        _uiState.value = _uiState.value.copy(isRefreshing = isRefreshing)
    }
}
