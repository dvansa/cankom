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
package dvansa.cankom.model

import java.time.LocalDateTime

// Get range of times to query meteo info for next commute based on curent time.
fun getQueryCommuteTimes(leaveTime : dvansa.cankom.model.TimePoint, arriveTime : dvansa.cankom.model.TimePoint, minutesResolution : Int = 5) : List<LocalDateTime> {
    val dateTime = LocalDateTime.now()
    // Round based on minutes resolution
    val leaveTimeRounded = TimePoint(leaveTime.hour, leaveTime.min - (leaveTime.min % minutesResolution))
    val arriveTimeRounded = TimePoint(arriveTime.hour, arriveTime.min - (arriveTime.min % minutesResolution))

    var commuteLeaveTime = LocalDateTime.of(dateTime.year,dateTime.month, dateTime.dayOfMonth, leaveTimeRounded.hour, leaveTimeRounded.min)

    // Commute time already passed. Check for next day.
    if (dateTime.isAfter(commuteLeaveTime)) {
        commuteLeaveTime = commuteLeaveTime.plusDays(1)
    }

    var commuteArriveTime = LocalDateTime.of(commuteLeaveTime.year,commuteLeaveTime.month, commuteLeaveTime.dayOfMonth, arriveTimeRounded.hour, arriveTimeRounded.min)
    if(commuteArriveTime.isBefore(commuteLeaveTime)) {
        commuteArriveTime = commuteArriveTime.plusDays(1)
    }
    check(commuteArriveTime.isAfter(commuteLeaveTime))

    val queryDateTimes : MutableList<LocalDateTime>  = mutableListOf()
    while(commuteLeaveTime.isBefore(commuteArriveTime)) {
        queryDateTimes.add(commuteLeaveTime)
        commuteLeaveTime = commuteLeaveTime.plusMinutes(minutesResolution.toLong())
    }
    return queryDateTimes.toList()
}