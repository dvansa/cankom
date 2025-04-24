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

import glm_.glm
import glm_.vec2.Vec2d
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

// Get bounding box enclosing all LatLng points in mapPath.
// Optionally a margin in km can be passed to expand bbox borders.
fun getLatLngBoundingBox(mapPath : MapPath, marginInKm: Double = 0.0) : Pair<Vec2d, Vec2d> {
    var bbMin = Vec2d(9e9, 9e9)
    var bbMax = Vec2d(-9e9, -9e9)
    mapPath.forEach {
        val p = Vec2d(it.latitude, it.longitude)
        bbMin = glm.min(bbMin, p)
        bbMax = glm.max(bbMax, p)
    }

    if(marginInKm > 0.0) {
        // Convert tolerance in km to deg
        val latTolerance = ROUTE_DISTANCE_TOL_IN_KM / 110.574
        val lngToleranceMin = ROUTE_DISTANCE_TOL_IN_KM / (glm.cos(bbMin.x) * 111.320)
        val lngToleranceMax = ROUTE_DISTANCE_TOL_IN_KM / (glm.cos(bbMax.x) * 111.320)

        bbMin = bbMin - Vec2d(latTolerance, lngToleranceMin)
        bbMax = bbMax + Vec2d(latTolerance, lngToleranceMax)
    }

    return Pair<Vec2d, Vec2d>(bbMin, bbMax)
}