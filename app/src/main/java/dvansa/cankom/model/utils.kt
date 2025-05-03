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
fun getQueryCommuteTimes(
    leaveTime: dvansa.cankom.model.TimePoint,
    arriveTime: dvansa.cankom.model.TimePoint,
    minutesResolution: Int = 5,
): List<LocalDateTime> {
    val dateTime = LocalDateTime.now()
    // Round based on minutes resolution
    val leaveTimeRounded = TimePoint(leaveTime.hour, leaveTime.min - (leaveTime.min % minutesResolution))
    val arriveTimeRounded = TimePoint(arriveTime.hour, arriveTime.min - (arriveTime.min % minutesResolution))

    var commuteLeaveTime = LocalDateTime.of(dateTime.year, dateTime.month, dateTime.dayOfMonth, leaveTimeRounded.hour, leaveTimeRounded.min)

    // Commute time already passed. Check for next day.
    if (dateTime.isAfter(commuteLeaveTime)) {
        commuteLeaveTime = commuteLeaveTime.plusDays(1)
    }

    var commuteArriveTime =
        LocalDateTime.of(
            commuteLeaveTime.year,
            commuteLeaveTime.month,
            commuteLeaveTime.dayOfMonth,
            arriveTimeRounded.hour,
            arriveTimeRounded.min,
        )
    if (commuteArriveTime.isBefore(commuteLeaveTime)) {
        commuteArriveTime = commuteArriveTime.plusDays(1)
    }
    check(commuteArriveTime.isAfter(commuteLeaveTime))

    val queryDateTimes: MutableList<LocalDateTime> = mutableListOf()
    while (commuteLeaveTime.isBefore(commuteArriveTime)) {
        queryDateTimes.add(commuteLeaveTime)
        commuteLeaveTime = commuteLeaveTime.plusMinutes(minutesResolution.toLong())
    }
    return queryDateTimes.toList()
}

// Get bounding box enclosing all LatLng points in mapPath.
// Optionally a margin in km can be passed to expand bbox borders.
fun getLatLngBoundingBox(
    mapPath: MapPath,
    marginInKm: Double = 0.0,
): Pair<Vec2d, Vec2d> {
    var bbMin = Vec2d(9e9, 9e9)
    var bbMax = Vec2d(-9e9, -9e9)
    mapPath.forEach {
        val p = Vec2d(it.latitude, it.longitude)
        bbMin = glm.min(bbMin, p)
        bbMax = glm.max(bbMax, p)
    }

    if (marginInKm > 0.0) {
        // Convert tolerance in km to deg
        val latTolerance = ROUTE_DISTANCE_TOL_IN_KM / 110.574
        val lngToleranceMin = ROUTE_DISTANCE_TOL_IN_KM / (glm.cos(bbMin.x) * 111.320)
        val lngToleranceMax = ROUTE_DISTANCE_TOL_IN_KM / (glm.cos(bbMax.x) * 111.320)

        bbMin = bbMin - Vec2d(latTolerance, lngToleranceMin)
        bbMax = bbMax + Vec2d(latTolerance, lngToleranceMax)
    }

    return Pair<Vec2d, Vec2d>(bbMin, bbMax)
}

// Checks if 2 axis aligned bounding boxes intersect.
fun bboxOverlap(
    box1min: Vec2d,
    box1max: Vec2d,
    box2min: Vec2d,
    box2max: Vec2d,
): Boolean = box1min.x < box2max.x && box2min.x < box1max.x && box1min.y < box2max.y && box2min.y < box1max.y

// Check if 2 line segments p0->p1 and q0->q1 intersect.
fun linesIntersect(
    p0: Vec2d,
    p1: Vec2d,
    q0: Vec2d,
    q1: Vec2d,
): Boolean {
    val dP = p1 - p0
    val dQ = q1 - q0

    val v0 = dQ.y * (q1.x - p0.x) - dQ.x * (q1.y - p0.y)
    val v1 = dQ.y * (q1.x - p1.x) - dQ.x * (q1.y - p1.y)
    val v2 = dP.y * (p1.x - q0.x) - dP.x * (p1.y - q0.y)
    val v3 = dP.y * (p1.x - q1.x) - dP.x * (p1.y - q1.y)

    return (v0 * v1 <= 0) && (v2 * v3 <= 0)
}

// Check if point is contained in polygon.
// Implementation from https://wrfranklin.org/Research/Short_Notes/pnpoly.html.
// Subject to the following license note:
/*
* Copyright (c) 1970-2003, Wm. Randolph Franklin
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
*
* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimers.
* Redistributions in binary form must reproduce the above copyright notice in the documentation and/or other materials provided with the distribution.
* The name of W. Randolph Franklin may not be used to endorse or promote products derived from this Software without specific prior written permission.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
fun pointContainedInPolygon(
    p: Vec2d,
    poly: List<Vec2d>,
): Boolean {
    var c = false
    var j = poly.size - 1
    for (i in 0 until poly.size) {
        if ((poly[i].y > p.y) != (poly[j].y > p.y) &&
            (p.x < (poly[j].x - poly[i].x) * (p.y - poly[i].y) / (poly[j].y - poly[i].y) + poly[i].x)
        ) {
            c = !c
        }
        j = i
    }
    return c
}
