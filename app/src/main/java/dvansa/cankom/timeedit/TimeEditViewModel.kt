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
package dvansa.cankom.timeedit


import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class TimePoint(
    var hour : Int,
    var min: Int
)

data class TimeEditUiState(
    var showLeaveTimeDialog : Boolean = false,
    var showArriveTimeDialog : Boolean = false,
    var leaveTime: TimePoint = TimePoint(hour=0, min=0),
    var arriveTime: TimePoint = TimePoint(hour=0, min=0)
)


class TimeEditViewModel () : ViewModel() {

    private val _uiState = MutableStateFlow<TimeEditUiState>(TimeEditUiState());
    val uiState: StateFlow<TimeEditUiState> get() = _uiState;

    fun showLeaveTimeDialog() {
        _uiState.value = _uiState.value.copy(showLeaveTimeDialog = true, showArriveTimeDialog = false);
    }

    fun showArriveTimeDialog() {
        _uiState.value = _uiState.value.copy(showLeaveTimeDialog = false, showArriveTimeDialog = true);
    }

    fun dismissDialogs() {
        _uiState.value = _uiState.value.copy(showLeaveTimeDialog = false, showArriveTimeDialog = false);
    }

    fun updateLeaveTime(hour: Int, min: Int) {
        _uiState.value = _uiState.value.copy(leaveTime = TimePoint(hour=hour, min=min));
    }

    fun updateArriveTime(hour: Int, min: Int) {
        _uiState.value = _uiState.value.copy(arriveTime = TimePoint(hour=hour, min=min));
    }

}


