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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import dvansa.cankom.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEditScreen(
    onSaveUserParameters: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Scaffold(modifier = modifier.fillMaxSize()) { paddingValues ->
        Column(modifier = modifier.padding(paddingValues) /*horizontalAlignment = Alignment.CenterHorizontally*/) {
            Box(contentAlignment = Alignment.TopStart) {
                IconButton(onClick = {
                    onBack();
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            TemperatureSlider(value = 0 /*TODO pass state value*/, label = "Minimum Temperature", minValue = -20, maxValue = 20, onTemperatureChange={ temperature -> /*TODO update model println("On value change $temperature")*/;});
            TemperatureSlider(value = 10 /*TODO pass state value*/, label = "Maximum Temperature", minValue = 0, maxValue = 40, onTemperatureChange={ temperature -> /*TODO update model*/});
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
    Text(text = "$label - $value °C", modifier = Modifier.padding(dimensionResource(R.dimen.horizontal_margin)))
    val barValue = (value - minValue).toFloat() / (maxValue - minValue).toFloat();
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        Slider(value= barValue, steps = maxValue - minValue + 1,onValueChange={value: Float -> onTemperatureChange( ((maxValue - minValue) * value).toInt());},modifier= Modifier.fillMaxWidth(0.7f));
    }
}