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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import glm_.vec2.Vec2d
import java.time.ZonedDateTime

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

    // Validate commute with precipitation data-
    // Returns intersecting regions with route and other near precipitation regions.
    suspend fun checkCommutePrecipitation() : Pair<List<PrecipitationRegion>, List<PrecipitationRegion>> {
        if (ZonedDateTime.now().offset.id != "+02:00") {
            throw Exception("Can only query in GMT+2 time zone.")
        }
        val timeZoneHourOffset = 2

        val queryTimes = getQueryCommuteTimes(
            _commuteParams.leaveTime,
            _commuteParams.arriveTime,
            QUERY_TIME_RESOLUTION_MINUTES
        ).map{ it.minusHours(timeZoneHourOffset.toLong()) }
        queryTimes.forEach{println("Querying precipitation at time $it GMT+0")}

        // Compute commute route bounding box
        val (routeMin, routeMax) = getLatLngBoundingBox(_route, ROUTE_DISTANCE_TOL_IN_KM)
        println("Commute route bounding box $routeMin , $routeMax")

        var closePrecipitationRegions : List<PrecipitationRegion> = listOf()
        var intersectingPrecipitationRegions : List<PrecipitationRegion> = listOf()
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

            val routePoints = _route.map{ Vec2d(it.latitude, it.longitude) }

            for ((i, precipitationRegions) in resultsPrecipitation.withIndex()) {
                // Filter out regions far away from commute route with simple AABB checks.
                val aabbRegions: List<Triple<Int, Vec2d, Vec2d>> = precipitationRegions.mapIndexed {
                    index, it ->
                    val (polyMin, polyMax) = getLatLngBoundingBox(it.polygon)
                    Triple(index, polyMin, polyMax)
                }
                val closeRouteRegions = aabbRegions.filter{
                    bboxOverlap(it.second, it.third, routeMin, routeMax)
                }

                // Fine-grained precipitation region intersection with route path.
                val intersectingRouteRegions = closeRouteRegions.map{
                    // Get original region polygon and convert to vector coordinates.
                    Pair<Int, List<Vec2d>>(it.first, precipitationRegions[it.first].polygon.map { Vec2d(it.latitude, it.longitude)})
                }.filter {
                    // Check for each route segment if at least intersects with one precipitation region polygon segment.
                    var intersects = false
                    for (routePoints in routePoints.windowed(2)) {
                            for (regionPoints in it.second.windowed(2)) {
                                // Check if segments intersect.
                                intersects = intersects || linesIntersect(regionPoints.get(0), regionPoints.get(1), routePoints.get(0), routePoints.get(1))
                            }
                    }
                    // Otherwise, there is also the case where the precipitation region encloses the whole route without intersecting any segments.
                    // In that case, pick first route point and check if it's contained within the polygon.
                    if(!intersects && !routePoints.isEmpty()) {
                        intersects = pointContainedInPolygon(routePoints.get(0), it.second)
                    }
                    intersects
                }

                // Resulting close precipitation regions.
                closePrecipitationRegions = closeRouteRegions.map{
                    precipitationRegions[it.first]
                }
                // Resulting intersecting precipitation regions.
                intersectingPrecipitationRegions = closeRouteRegions.filter{
                    intersectingRouteRegions.find { it2 -> it2.first == it.first} != null
                }.map {
                    precipitationRegions[it.first]
                }
                println("T$i) Received ${precipitationRegions.size} regions -> ${closeRouteRegions.size} regions close to route -> ${intersectingRouteRegions.size} intersecting regions.")

                // Already found precipitation regions intersecting commute route. Skip rest of times.
                if(!intersectingPrecipitationRegions.isEmpty()) {
                    break
                }
            }
        }

        return Pair(intersectingPrecipitationRegions, closePrecipitationRegions)
    }
}