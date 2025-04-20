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
package dvansa.cankom.useredit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.viewModelScope
import dvansa.cankom.R
import dvansa.cankom.meteo.MeteoClient
import kotlinx.coroutines.launch


data class InitialState(
    val minTemperature: Int,
    val maxTemperature: Int,
    val leaveTime : dvansa.cankom.model.TimePoint,
    val arriveTime : dvansa.cankom.model.TimePoint
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEditScreen(
    initialState : InitialState,
    onSaveUserParameters: (minTemperature: Int, maxTemperature: Int, leaveTime : TimePoint, arriveTime : TimePoint) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: UserEditViewModel = UserEditViewModel(
            UserEditUiState(
                minTemperature = initialState.minTemperature,
                maxTemperature = initialState.maxTemperature,
                leaveTime = TimePoint(initialState.leaveTime.hour, initialState.leaveTime.min),
                arriveTime = TimePoint(initialState.arriveTime.hour, initialState.arriveTime.min)
            )
        ),
) {
    val uiState by viewModel.uiState.collectAsState();
    Scaffold(modifier = modifier.fillMaxSize()) { paddingValues ->
        Column(modifier = modifier.padding(paddingValues)) {
            Box(contentAlignment = Alignment.TopStart) {
                IconButton(onClick = {
                    onSaveUserParameters(uiState.minTemperature, uiState.maxTemperature, uiState.leaveTime, uiState.arriveTime)
                    onBack();
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }

            // Temperature
            TemperatureSlider(value = uiState.minTemperature, label = "Minimum Temperature", minValue = -20, maxValue = 20, onTemperatureChange={ temperature -> viewModel.updateMinTemperature(temperature);});
            TemperatureSlider(value = uiState.maxTemperature, label = "Maximum Temperature", minValue = 0, maxValue = 40, onTemperatureChange={ temperature -> viewModel.updateMaxTemperature(temperature);});

            // Time edit
            TextTimeDuration(
                startHour=uiState.leaveTime.hour,
                startMin = uiState.leaveTime.min,
                endHour=uiState.arriveTime.hour,
                endMin = uiState.arriveTime.min,
            )
            EditableTime(label = "Leave time", onEdit = {
                viewModel.showLeaveTimeDialog()
            }, hour= uiState.leaveTime.hour, min=uiState.leaveTime.min)

            EditableTime(label = "Arrive time", onEdit = {
                viewModel.showArriveTimeDialog()
            }, hour= uiState.arriveTime.hour, min=uiState.arriveTime.min)

            if (uiState.showLeaveTimeDialog || uiState.showArriveTimeDialog) {
                val timeInterval = if (uiState.showLeaveTimeDialog) uiState.leaveTime else uiState.arriveTime
                val label = if (uiState.showLeaveTimeDialog) "Set leave time" else "Set arrival time"
                val state = TimePickerState(
                    initialHour = timeInterval.hour,
                    initialMinute = timeInterval.min,
                    is24Hour = false
                )
                TimePickerDialog(
                    onDismiss = { viewModel.dismissDialogs() },
                    onSet = {
                        if (uiState.showLeaveTimeDialog) {
                            viewModel.updateLeaveTime(hour=state.hour, min=state.minute)
                            viewModel.dismissDialogs()
                        } else {
                            viewModel.updateArriveTime(hour=state.hour, min=state.minute)
                            viewModel.dismissDialogs()
                        }
                    }
                ) {
                    Text(text = label)
                    TimePicker(
                        state = state
                    )
                }
            }

            // TODO. Remove, only used to test Http API
            Button(onClick = {
                val client = MeteoClient()
                viewModel.viewModelScope.launch {
                    try {
                        val response = client.getPrecipitationRadarData(2025, 4, 20, 17, 0)
                        println("Receieved system coords: ${response.coords.system}. #Areas: ${response.areas.size} ")
                        val area = response.areas.get(0)
                        println("[Area 0] color: ${area.color}. #Shapes: ${area.shapes.size} ")
                        check(area.shapes.size >= 10 && !area.shapes.get(10).isEmpty())
                        val shape00 = area.shapes.get(10).get(0)
                        println("[Area 0][Shape 10 0] i: ${shape00.i} j: ${shape00.j} l: '${shape00.l} o: '${shape00.o}' d: '${shape00.d}'")

                    } catch (e: Exception) {
                        println("Http error while getting radar data: ${e.message}")
                    }
                }

            }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Test Query")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemperatureSlider(
    value: Int,
    label: String,
    minValue : Int = -10,
    maxValue : Int = 10,
    onTemperatureChange: (Int) -> Unit
) {
    Text(text = "$label   $value °C", modifier = Modifier.padding(dimensionResource(R.dimen.horizontal_margin)))
    val barValue = (value - minValue).toFloat() / (maxValue - minValue).toFloat()
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        Slider(value= barValue, steps = maxValue - minValue + 1,onValueChange={value: Float -> onTemperatureChange(((maxValue - minValue) * value + minValue).toInt());},modifier= Modifier.fillMaxWidth(0.7f));
    }
}

@Composable
fun EditableTime (
    label: String,
    onEdit: () -> Unit,
    hour: Int,
    min: Int,
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, modifier=Modifier.padding(dimensionResource(R.dimen.horizontal_margin)))
            Text(
                text = " ${hour.toString().padStart(2,'0')}:${min.toString().padStart(2,'0')}",
                modifier=Modifier.padding(dimensionResource(R.dimen.horizontal_margin))
            )
            Button(onClick = {
                onEdit();
            }) {
                Icon(Icons.Filled.Edit, contentDescription = label)
            }

        }
    }
}

@Composable
fun TextTimeDuration (startHour: Int, startMin: Int, endHour: Int, endMin: Int, modifier: Modifier = Modifier) {
    val durationFullMin = (60 * (endHour  - startHour) + endMin - startMin).let{if (it < 0) it + 24 * 60 else it};
    val durationHours = durationFullMin / 60;
    val durationMins = durationFullMin % 60;

    val textHours = if (durationHours > 0) "$durationHours hours${if (durationMins > 0) " and" else ""}" else ""
    val textMins = if (durationMins > 0)  "$durationMins minutes" else  ""
    Text(
        text = "Duration: $textHours $textMins",
        modifier = modifier.padding(dimensionResource(R.dimen.horizontal_margin))
    )
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit = {},
    onSet: () -> Unit = {},
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Dismiss")
            }
        },
        confirmButton = {
            TextButton(onClick = { onSet() }) {
                Text("Set")
            }
        },
        text = { content() }
    )
}