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

import android.content.Context
import android.location.Geocoder
import androidx.datastore.dataStore
import dvansa.cankom.meteo.MeteoClient
import dvansa.cankom.meteo.PrecipitationRegion
import glm_.vec2.Vec2d
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.min

// Queries to meteorological data are made every <QUERY_TIME_RESOLUTION_MINUTES> minutes.
// Highly impacts app processing performance and latency.
const val QUERY_TIME_RESOLUTION_MINUTES = 10

// Route distance tolerance in km.
const val ROUTE_DISTANCE_TOL_IN_KM = 0.5

// Model data store
val Context.modelDataStore by dataStore(
    fileName = "data_model.json",
    serializer = DataModelSerializer,
)

class AppController(
    val meteoClient: MeteoClient,
) {
    private var model: DataModel = DataModel()

    // Cached meteo data
    private var precipitationRegions: Pair<List<PrecipitationRegion>, List<PrecipitationRegion>>? = null
    private var temperatureRange: Pair<Int, Int>? = null

    suspend fun loadModel(context: Context) {
        model = context.modelDataStore.data.first()
    }

    suspend fun saveModel(context: Context) {
        context.modelDataStore.updateData { currentModel ->
            currentModel.copy(commuteParams = model.commuteParams, route = model.route)
        }
    }

    // Commute Parameters
    fun setCommuteTemperatureRange(
        minTemperature: Int? = null,
        maxTemperature: Int? = null,
    ) {
        minTemperature?.let {
            model = model.copy(commuteParams = model.commuteParams.copy(minTemperature = it))
        }
        maxTemperature?.let {
            model = model.copy(commuteParams = model.commuteParams.copy(maxTemperature = it))
        }
        // Invalidate cache
        temperatureRange = null
    }

    fun setCommuteTime(
        leaveTime: TimePoint? = null,
        arriveTime: dvansa.cankom.model.TimePoint? = null,
    ) {
        leaveTime?.let {
            model = model.copy(commuteParams = model.commuteParams.copy(leaveTime = it))
        }
        arriveTime?.let {
            model = model.copy(commuteParams = model.commuteParams.copy(arriveTime = it))
        }
        // Invalidate cache
        temperatureRange = null
        precipitationRegions = null
    }

    fun getCommuteParameters(): CommuteParameters = model.commuteParams

    // Route
    fun setCommuteRoute(newRoute: MapPath) {
        model = model.copy(route = newRoute)
        // Invalidate cache
        temperatureRange = null
        precipitationRegions = null
    }

    fun getCommuteRoute(): MapPath = model.route

    fun getNextCommuteTimes(minutesResolution: Int? = null): List<LocalDateTime> {
        if (ZonedDateTime.now().offset.id != "+02:00") {
            throw Exception("Can only query in GMT+2 time zone.")
        }
        val timeZoneHourOffset = 2

        val queryTimes =
            getQueryCommuteTimes(
                model.commuteParams.leaveTime,
                model.commuteParams.arriveTime,
                minutesResolution ?: QUERY_TIME_RESOLUTION_MINUTES,
            ).map { it.minusHours(timeZoneHourOffset.toLong()) }
        return queryTimes
    }

    // Returns intersecting regions with commute route and other near precipitation regions.
    suspend fun checkCommutePrecipitation(useCached: Boolean = true): Pair<List<PrecipitationRegion>, List<PrecipitationRegion>>? {
        if (useCached && precipitationRegions != null) {
            return precipitationRegions!!
        }

        val queryTimes = getNextCommuteTimes()
        queryTimes.forEach { println("Querying precipitation at time $it GMT+0") }

        // Compute commute route bounding box
        val (routeMin, routeMax) = getLatLngBoundingBox(model.route, ROUTE_DISTANCE_TOL_IN_KM)
        println("Commute route bounding box $routeMin , $routeMax")

        var closePrecipitationRegions: List<PrecipitationRegion> = listOf()
        var intersectingPrecipitationRegions: List<PrecipitationRegion> = listOf()
        var resultsPrecipitation: List<List<PrecipitationRegion>?> = listOf()
        coroutineScope {
            resultsPrecipitation =
                queryTimes
                    .map {
                        async {
                            var precipitationData: List<PrecipitationRegion>? = null
                            try {
                                precipitationData =
                                    meteoClient.getPrecipitationRadarData(
                                        it.year,
                                        it.monthValue,
                                        it.dayOfMonth,
                                        it.hour,
                                        it.minute,
                                    )
                            } catch (e: Exception) {
                                println("Could not get precipitation radar data.")
                            }
                            precipitationData
                        }
                    }.awaitAll()
        }

        // Ensure all precipitation queries are successful. Otherwise return null.
        if (resultsPrecipitation.find { it == null } != null) {
            return null
        }

        val routePoints = model.route.map { Vec2d(it.latitude, it.longitude) }

        for ((i, queriedPrecipitationRegions) in resultsPrecipitation.withIndex()) {
            val precipitationRegions = queriedPrecipitationRegions!!
            // Filter out regions far away from commute route with simple AABB checks.
            val aabbRegions: List<Triple<Int, Vec2d, Vec2d>> =
                precipitationRegions.mapIndexed { index, it ->
                    val (polyMin, polyMax) = getLatLngBoundingBox(it.polygon)
                    Triple(index, polyMin, polyMax)
                }
            val closeRouteRegions =
                aabbRegions.filter {
                    bboxOverlap(it.second, it.third, routeMin, routeMax)
                }

            // Fine-grained precipitation region intersection with route path.
            val intersectingRouteRegions =
                closeRouteRegions
                    .map {
                        // Get original region polygon and convert to vector coordinates.
                        Pair<Int, List<Vec2d>>(it.first, precipitationRegions[it.first].polygon.map { Vec2d(it.latitude, it.longitude) })
                    }.filter {
                        // Check for each route segment if at least intersects with one precipitation region polygon segment.
                        var intersects = false
                        for (routePoints in routePoints.windowed(2)) {
                            for (regionPoints in it.second.windowed(2)) {
                                // Check if segments intersect.
                                intersects =
                                    intersects ||
                                    linesIntersect(
                                        regionPoints[0],
                                        regionPoints[1],
                                        routePoints[0],
                                        routePoints[1],
                                    )
                            }
                        }
                        // Otherwise, there is also the case where the precipitation region encloses the whole route without intersecting any segments.
                        // In that case, pick first route point and check if it's contained within the polygon.
                        if (!intersects && !routePoints.isEmpty()) {
                            intersects = pointContainedInPolygon(routePoints[0], it.second)
                        }
                        intersects
                    }

            // Resulting close precipitation regions.
            closePrecipitationRegions =
                closeRouteRegions.map {
                    precipitationRegions[it.first]
                }
            // Resulting intersecting precipitation regions.
            intersectingPrecipitationRegions =
                closeRouteRegions
                    .filter {
                        intersectingRouteRegions.find { it2 -> it2.first == it.first } != null
                    }.map {
                        precipitationRegions[it.first]
                    }
            println(
                "T$i) Received ${precipitationRegions.size} regions -> ${closeRouteRegions.size} regions close to route -> ${intersectingRouteRegions.size} intersecting regions.",
            )

            // Already found precipitation regions intersecting commute route. Skip rest of times.
            if (!intersectingPrecipitationRegions.isEmpty()) {
                break
            }
        }

        precipitationRegions = Pair(intersectingPrecipitationRegions, closePrecipitationRegions)
        return precipitationRegions!!
    }

    suspend fun checkCommuteTemperatureRange(
        context: Context? = null,
        useCached: Boolean = true,
    ): Pair<Int, Int>? {
        if (useCached && temperatureRange != null) {
            return temperatureRange
        }

        if (model.route.size < 2 || context == null) {
            return null
        }

        var postalCodes = mutableSetOf<Int>()

        val geocoder = Geocoder(context)
        // Query start and end point of route
        for (routePoint in listOf<LatLng>(model.route.first(), model.route.last())) {
            val addresses = geocoder.getFromLocation(routePoint.latitude, routePoint.longitude, 5)
            if (addresses != null) {
                for (addr in addresses) {
                    if (addr.locality != null && addr.postalCode != null) {
                        postalCodes.add(addr.postalCode.toInt())
                    }
                }
            }
        }
        println("Querying postal codes for temperature ranges: $postalCodes")

        val queryTimes = getNextCommuteTimes()
        var queriedTemperatures = listOf<Int?>()
        coroutineScope {
            var temperatureQueries = mutableListOf<Deferred<Int?>>()
            for (queryTime in listOf(queryTimes.first(), queryTimes.last())) {
                for (postalCode in postalCodes) {
                    temperatureQueries.add(
                        async {
                            var temperature: Int? = null
                            try {
                                temperature =
                                    meteoClient.getTemperature(
                                        postalCode,
                                        queryTime.year,
                                        queryTime.monthValue,
                                        queryTime.dayOfMonth,
                                        queryTime.hour,
                                        queryTime.minute,
                                    )
                            } catch (e: Exception) {
                                println("Could not get temperature for CP $postalCode")
                            }
                            temperature
                        },
                    )
                }
            }
            queriedTemperatures = temperatureQueries.awaitAll()
        }

        val temperatures = queriedTemperatures.filter { it != null }.map { it!! }
        if (temperatures.isEmpty()) {
            return null
        }

        // If any of the ranges are outside of min/max allowed temperatures fail check.
        var minTemperature: Int = 9e6.toInt()
        var maxTemperature: Int = (-9e6).toInt()
        for (minMaxTemperature in temperatures) {
            minTemperature = min(minTemperature, minMaxTemperature)
            maxTemperature = max(maxTemperature, minMaxTemperature)
        }

        temperatureRange = Pair(minTemperature, maxTemperature)
        return temperatureRange!!
    }
}
