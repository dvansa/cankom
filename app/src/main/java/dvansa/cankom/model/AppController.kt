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

import dvansa.cankom.meteo.MeteoClient
import dvansa.cankom.meteo.PrecipitationRegion
import kotlin.math.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import glm_.glm
import glm_.vec2.Vec2
import glm_.vec2.Vec2d

// Queries to meteorological data are made every <QUERY_TIME_RESOLUTION_MINUTES> minutes.
// Highly impacts app processing performance and latency.
const val QUERY_TIME_RESOLUTION_MINUTES = 10;

// Route distance tolerance in km.
const val ROUTE_DISTANCE_TOL_IN_KM = 0.5

class AppController(val meteoClient : MeteoClient) {
    private val _commuteParams = CommuteParameters()
    private var _route: MapPath = listOf()

    // Commute Parameters
    fun setCommuteTemperatureRange(minTemperature : Int? = null, maxTemperature : Int? = null) {
        minTemperature?.let {
            _commuteParams.minTemperature = it
        }
        maxTemperature?.let {
            _commuteParams.maxTemperature = it
        }
    }

    fun setCommuteTime(leaveTime: TimePoint? = null, arriveTime: dvansa.cankom.model.TimePoint? = null) {
        leaveTime?.let {
            _commuteParams.leaveTime = it
        }
        arriveTime?.let {
            _commuteParams.arriveTime = it
        }
    }

    fun getCommuteParameters() : CommuteParameters {
        return _commuteParams
    }

    // Route
    fun setCommuteRoute(newRoute : MapPath) {
        _route = newRoute
    }

    fun getCommuteRoute() : MapPath {
        return _route
    }

    // Validate commute with precipitation data
    suspend fun checkCommutePrecipitation() : List<PrecipitationRegion> {
        val queryTimes = getQueryCommuteTimes(
            _commuteParams.leaveTime,
            _commuteParams.arriveTime,
            QUERY_TIME_RESOLUTION_MINUTES
        )
        queryTimes.forEach{println("Querying precipitation at time $it")}

        // Compute commute route boudning box

        val (routeMin, routeMax) = getLatLngBoundingBox(_route, ROUTE_DISTANCE_TOL_IN_KM)
        println("Commute route bounding box $routeMin , $routeMax")

        // 2D axis aligned bounding box intersection check
        val bboxOverlap : (Vec2d, Vec2d, Vec2d, Vec2d) -> Boolean = { box1min : Vec2d, box1max : Vec2d, box2min : Vec2d, box2max : Vec2d ->
            box1min.x < box2max.x && box2min.x < box1max.x && box1min.y < box2max.y && box2min.y < box1max.y
        }

        var closePrecipitationRegions : List<PrecipitationRegion> = listOf()
        coroutineScope {
            val resultsPrecipitation = queryTimes.map {
                async {
                    meteoClient.getPrecipitationRadarData(
                        it.year,
                        it.monthValue,
                        it.dayOfMonth,
                        it.hour,
                        it.minute
                    )
                }
            }.awaitAll()

            for ((i, precipitationRegions) in resultsPrecipitation.withIndex()) {
                // Filter regions far away from commute route with simple AABB checks.
                val aabbRegions: List<Triple<Int, Vec2d, Vec2d>> = precipitationRegions.mapIndexed {
                    index, it ->
                    val (polyMin, polyMax) = getLatLngBoundingBox(it.polygon)
                    Triple(index, polyMin, polyMax)
                }
                val closeRouteRegions = aabbRegions.filter{
                    bboxOverlap(it.second, it.third, routeMin, routeMax)
                }

                // TODO: fine grained polygon-line distance checks

                // Return indices
                closePrecipitationRegions = closeRouteRegions.map{ precipitationRegions[it.first] }
                println("$i) Received ${precipitationRegions.size} regions -> ${closeRouteRegions.size} regions close to route.")

                // Already found precipitation regions in commute route. Skip rest of times.
                if(!closePrecipitationRegions.isEmpty()) {
                    break
                }
            }
        }

        return closePrecipitationRegions
    }
}