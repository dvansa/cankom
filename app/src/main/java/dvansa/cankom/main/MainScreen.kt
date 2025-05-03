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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import dvansa.cankom.R
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Composable
fun MainScreen(
    onEditButton: () -> Unit,
    onMapRouteButton: () -> Unit,
    onRefresh: suspend (Context, Boolean) -> Unit,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val localContext = LocalContext.current
    // Refresh on load.
    LaunchedEffect(Unit) {
        viewModel.updateRefreshing(isRefreshing = true)
        viewModel.viewModelScope.launch {
            onRefresh(localContext, /*useCached=*/true)
            viewModel.refreshCommuteState()
            viewModel.updateRefreshing(isRefreshing = false)
        }
    }
    val uiState by viewModel.uiState.collectAsState()
    Scaffold { paddingValues ->
        Column(modifier = modifier.padding(paddingValues)) {
            // Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "CanKom",
                    fontSize = dimensionResource(R.dimen.title_font_size).value.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(dimensionResource(R.dimen.horizontal_margin)),
                )
                Box(
                    contentAlignment = Alignment.CenterEnd,
                    modifier = Modifier.fillMaxWidth().padding(dimensionResource(R.dimen.horizontal_small_margin)),
                ) {
                    // Settings
                    Row {
                        Button(
                            onClick = {
                                onMapRouteButton()
                            },
                            enabled = !uiState.isRefreshing,
                            modifier = Modifier.padding(dimensionResource(R.dimen.horizontal_small_margin)),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = "Map Route",
                                tint = Color.White,
                            )
                        }
                        Button(
                            onClick = {
                                onEditButton()
                            },
                            enabled = !uiState.isRefreshing,
                            modifier = Modifier.padding(dimensionResource(R.dimen.horizontal_small_margin)),
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = "Edit")
                        }
                    }
                }
            }

            // Separator
            HorizontalDivider(
                color = Color.Gray,
                thickness = 1.dp,
            )

            // Commute info
            CommuteStatus(
                uiState.commuteWarnings,
                uiState.nextCommuteTime,
                refreshing = uiState.isRefreshing,
            )

            // Refresh button
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        viewModel.updateRefreshing(isRefreshing = true)
                        viewModel.viewModelScope.launch {
                            onRefresh(localContext, /*useCached=*/false)
                            viewModel.refreshCommuteState()
                            viewModel.updateRefreshing(isRefreshing = false)
                        }
                    },
                    shape = CircleShape,
                    enabled = !uiState.isRefreshing,
                    modifier = Modifier.padding(dimensionResource(R.dimen.horizontal_large_margin)).size(90.dp),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", modifier = Modifier.size(50.dp))
                }
            }
        }
    }
}

const val EMOJI_HEATED_FACE = "\uD83E\uDD75"
const val EMOJI_COLD_FACE = "\uD83E\uDD76"
const val EMOJI_RAIN_CLOUD = "\uD83C\uDF27"
const val EMOJI_PERSON_WALKING = "\uD83D\uDEB6"
const val EMOJI_BIKE = "\uD83D\uDEB2"
const val EMOJI_WARNING = "\u26A0\uFE0F"
const val EMOJI_NO_ENTRY = "\u26D4\uFE0F"

@Composable
fun CommuteStatus(
    commuteWarnings: List<CommuteWarning>,
    nextCommuteTime: LocalDateTime?,
    refreshing: Boolean = false,
) {
    val commuteStatusHeightFraction = 0.5f
    if (refreshing) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight(fraction = commuteStatusHeightFraction).fillMaxWidth(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
            ) {
                Text("Refreshing...", fontSize = dimensionResource(R.dimen.commute_warning_font_size).value.sp)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxHeight(fraction = commuteStatusHeightFraction)) {
            if (nextCommuteTime != null) {
                Text(
                    String.format(
                        "Next commute ${nextCommuteTime.dayOfWeek} at %02d:%02d",
                        nextCommuteTime.hour,
                        nextCommuteTime.minute,
                    ),
                    modifier = Modifier.padding(dimensionResource(R.dimen.horizontal_margin)),
                )
            }
            if (commuteWarnings.isEmpty()) {
                TextCommuteWarning("$EMOJI_BIKE$EMOJI_PERSON_WALKING Good weather conditions. Ready to commute!")
            } else {
                commuteWarnings.forEach {
                    val statusText =
                        when (it.state) {
                            CommuteState.ROUTE_UNKNOWN -> "$EMOJI_WARNING Commute route is not set."
                            CommuteState.PRECIPITATION_UNKNOWN -> "$EMOJI_WARNING Precipitation data unavailable."
                            CommuteState.TEMPERATURE_UNKNOWN -> "$EMOJI_WARNING Temperature data unavailable."
                            CommuteState.TEMPERATURE_HOT -> "$EMOJI_NO_ENTRY $EMOJI_HEATED_FACE Hot temperatures reaching ${it.param1.toInt()} °C (higher than ${it.param2.toInt()} °C)."
                            CommuteState.TEMPERATURE_COLD -> "$EMOJI_NO_ENTRY $EMOJI_COLD_FACE Cold temperatures reaching ${it.param1.toInt()} °C (lower than ${it.param2.toInt()} °C)."
                            CommuteState.PRECIPITATION -> "$EMOJI_NO_ENTRY $EMOJI_RAIN_CLOUD Precipitations on commute route."
                        }
                    TextCommuteWarning(statusText)
                }
            }
        }
    }
}

@Composable
fun TextCommuteWarning(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        fontSize = dimensionResource(R.dimen.commute_warning_font_size).value.sp,
        modifier =
            modifier.padding(
                horizontal = dimensionResource(R.dimen.horizontal_large_margin),
                vertical = dimensionResource(R.dimen.horizontal_margin),
            ),
    )
}
