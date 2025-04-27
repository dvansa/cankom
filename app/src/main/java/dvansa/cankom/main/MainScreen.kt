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

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime


@Composable
fun MainScreen(
    onEditButton: () -> Unit,
    onMapRouteButton: () -> Unit,
    onRefresh: suspend (Context) -> Unit,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val localContext = LocalContext.current
    // Refresh on load.
    LaunchedEffect(Unit) {
        viewModel.updateRefreshing(isRefreshing = true)
        viewModel.viewModelScope.launch {
            onRefresh(localContext)
            viewModel.refreshCommuteState()
            viewModel.updateRefreshing(isRefreshing = false)
        }
    }

    Scaffold() { paddingValues ->
        Column(modifier = modifier.padding(paddingValues)) {
            val uiState by viewModel.uiState.collectAsState();
            Text(text = "Main screen")
            Button(onClick = {
                onEditButton();
            }) {
                Icon(Icons.Filled.Settings, contentDescription = "Edit")
            }
            Button(onClick = {
                onMapRouteButton();

            },
            enabled = !uiState.isRefreshing) {
                Icon(Icons.Filled.Settings, contentDescription = "Map Route")
            }

            Button(onClick = {
                viewModel.updateRefreshing(isRefreshing = true)
                viewModel.viewModelScope.launch {
                    onRefresh(localContext)
                    viewModel.refreshCommuteState()
                    viewModel.updateRefreshing(isRefreshing = false)
                }
            },
            enabled = !uiState.isRefreshing) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
            CommuteStatus(uiState.commuteWarnings, uiState.nextCommuteTime, refreshing = uiState.isRefreshing)
        }
    }
}

const val EMOJI_HEATED_FACE = "\uD83E\uDD75"
const val EMOJI_COLD_FACE = "\uD83E\uDD76"
const val EMOJI_RAIN_CLOUD = "\uD83C\uDF27"
const val EMOJI_PERSON_WALKING = "\uD83D\uDEB6"
const val EMOJI_BIKE = "\uD83D\uDEB2"
const val EMOJI_WARNING = "\u26A0\uFE0F"
const val EMOJI_NO_ENTRY= "\u26D4\uFE0F"

@Composable
fun CommuteStatus(commuteWarnings : List<CommuteWarning>, nextCommuteTime: LocalDateTime?, refreshing : Boolean = false) {
    if(refreshing) {
        Text("Refreshing...")
    } else {
        if(nextCommuteTime != null) {
            Text(String.format("Next commute on ${nextCommuteTime.dayOfWeek} at %02d:%02d", nextCommuteTime.hour, nextCommuteTime.minute))
        }
        if (commuteWarnings.isEmpty()) {
            Text("$EMOJI_BIKE$EMOJI_PERSON_WALKING Good weather conditions. Ready to commute!")
        } else {
            commuteWarnings.forEach{
                val statusText = when (it.state) {
                    CommuteState.ROUTE_UNKNOWN -> "$EMOJI_WARNING Commute route is not set."
                    CommuteState.PRECIPITATION_UNKNOWN -> "$EMOJI_WARNING Precipitation data unavailable."
                    CommuteState.TEMPERATURE_UNKNOWN -> "$EMOJI_WARNING Temperature data unavailable."
                    CommuteState.TEMPERATURE_HOT -> "$EMOJI_NO_ENTRY $EMOJI_HEATED_FACE Hot temperatures reaching ${it.param1.toInt()} °C (higher than ${it.param2.toInt()} °C)."
                    CommuteState.TEMPERATURE_COLD -> "$EMOJI_NO_ENTRY $EMOJI_COLD_FACE Cold temperatures reaching ${it.param1.toInt()} °C (lower than ${it.param2.toInt()} °C)."
                    CommuteState.PRECIPITATION -> "$EMOJI_NO_ENTRY $EMOJI_RAIN_CLOUD Precipitations on commute route."
                }
                Text(statusText)
            }
        }
    }
}