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
package dvansa.cankom.meteo

import dvansa.cankom.model.LatLng
import dvansa.cankom.model.MapPath
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.pow

// Constants
const val HTTP_TIMEOUT_IN_MILLISECONDS = 5000L
const val URL_BASE = "https://www.meteoswiss.admin.ch/product/output"
const val URL_SUB_VERSION = "versions.json"
const val PRECIPITATION_PRODUCT_NAME = "inca/precipitation/rate"
const val FORECAST_CHART_PRODUCT_NAME = "forecast-chart"

val MAP_COLOR_TO_INTENSITY : Map<String, Int> = mapOf(
    "9a7e95" to 0,
    "0001fc" to 1,
    "058c2d" to 2,
    "05ff05" to 3,
    "feff01" to 4,
    "ffc703" to 5,
    "ff7d01" to 6,
    // TODO missing values for last 2 levels.
    "<RED>" to 7,
    "<PURPLE>" to 8
)

// Response JSON data -- Precipitation
@Serializable
data class RadarDataCoordinates(
    val system : String,
    val x_min : Double,
    val x_max : Double,
    val x_count : Int,
    val y_min : Double,
    val y_max : Double,
    val y_count : Int
)

@Serializable
data class RadarDataShape(
    val i : Int,
    val j: Int,
    val l: Int,
    val d : String,
    val o : String
)

@Serializable
data class RadarDataArea(
    val color : String,
    val shapes : List<List<RadarDataShape>>
)

@Serializable
data class RadarData(
    val coords : RadarDataCoordinates,
    val areas : List<RadarDataArea>
)

// Response JSON data -- Forecast chart

@Serializable
data class ForecastSymbol(
    val weather_symbol_id : Int,
    val timestamp : Long
)

@Serializable
data class ForecastWindGustPeak(
    val data : List<List<Double>>,
)

@Serializable
data class ForecastWindSymbol(
    val symbol_id : String,
    val timestamp: Long
)

@Serializable
data class ForecastWind(
    val data : List<List<Double>>,
    val symbols : List<ForecastWindSymbol>
)

@Serializable
data class ForecastChart(
    val day_string : String,
    val min_date : Long,
    val max_date : Long,
    val sunrise : Long,
    val sunset : Long,
    val current_time : Long?, // null?
    val current_time_string : String?, // null?

    val wind : ForecastWind,
    val wind_gust_peak : ForecastWindGustPeak,

    val wind_speed_variance : List<List<Double>>,
    // val wind_gust_variance : List<List<Double>>,
    val wind_gust_speed_variance : List<List<Double>>,
    val rainfall : List<List<Double>>,
    val sunshine : List<List<Double>>,
    val variance_rain : List<List<Double>>,
    val variance_range : List<List<Double>>,
    val temperature : List<List<Double>>,

    val symbol_day: ForecastSymbol,
    val symbols : List<ForecastSymbol>
)

fun convertFromLV0395ToCH(x : Double, y : Double) : Pair<Double, Double> {
    return Pair(if (x >= 2e6) x - 2e6 else x, if (y >= 1e6) y - 1e6 else y)
}

fun convertFromCHToWGS(x: Double, y: Double) : Pair<Double, Double> {
    val (x, y) = convertFromLV0395ToCH(x, y)
    val ux = (x - 6e5) / 1e6
    val uy = (y - 2e5) / 1e6

    val nx = 2.6779094 + 4.728982 * ux + 0.791484 * ux * uy + 0.1306 * ux * uy.pow(2) - 0.0436 * ux.pow(3)
    val ny = 16.9023892 + 3.238272 * uy - 0.270978 * ux.pow(2) - 0.002528 * uy.pow(2) - 0.0447 * ux.pow(2) * uy - 0.014 * uy.pow(3)

    return Pair(100.0 *  nx / 36.0, 100.0 * ny / 36.0)
}

fun convertShapeToPolygonPath(shape : RadarDataShape, coords: RadarDataCoordinates) : List<LatLng> {
    val points : MutableList<LatLng> = mutableListOf()
    var i = shape.i
    var j = shape.j

    for (s in 0 until shape.o.length) {
        var u = 0.0
        var v = 0.0
        val z = shape.o[s].digitToInt() / 10.0 + 0.05
        if (i % 2 == 0) {
            u = coords.x_min + (coords.x_max - coords.x_min) *  (i / 2).toDouble() / coords.x_count.toDouble()
            v = coords.y_min + (coords.y_max - coords.y_min) *  ((j - 1).toDouble() / 2.0 + z) / coords.y_count.toDouble()
        } else {
            u = coords.x_min + (coords.x_max - coords.x_min) *  ((i - 1).toDouble() / 2.0 + z) / coords.x_count.toDouble()
            v = coords.y_min + (coords.y_max - coords.y_min) *  (j / 2.0).toDouble() / coords.y_count.toDouble()
        }

        val (lng, lat) = convertFromCHToWGS(1e3 * u, 1e3 * v)
        points.add(LatLng(lat, lng))

        if (2 * s < shape.d.length) {
            i += shape.d[2 * s].code - 77
            j += shape.d[2 * s + 1].code - 77
        }
    }

    return points.toList()
}

// Http client builder
fun getHttpClient() : HttpClient {
    return HttpClient(engineFactory = Android) {
        install(plugin = ContentNegotiation) {
            json(json = Json {
                ignoreUnknownKeys = true
            })
        }

        install(plugin = HttpTimeout) {
            requestTimeoutMillis = HTTP_TIMEOUT_IN_MILLISECONDS
            connectTimeoutMillis = HTTP_TIMEOUT_IN_MILLISECONDS
            socketTimeoutMillis = HTTP_TIMEOUT_IN_MILLISECONDS
        }
    }
}

class MeteoSwissClient(
    private val httpClient: HttpClient = getHttpClient()
) : MeteoClient {
    private suspend fun queryApiProductVersion(): Map<String, String> = httpClient.get {
        url(urlString = "$URL_BASE/$URL_SUB_VERSION")
    }.body()

    private suspend fun queryPrecipitationRadarData(productVersion: String, year: Int, month: Int, day: Int, hour: Int, mins: Int): RadarData = httpClient.get {
        url(urlString = String.format("$URL_BASE/$PRECIPITATION_PRODUCT_NAME/version__$productVersion/rate_%04d%02d%02d_%02d%02d.json", year, month, day, hour, mins))
    }.body()

    private suspend fun queryForecastChart(productVersion: String, postalCode: Int) : List<ForecastChart> = httpClient.get {
        val postalCodeFormatted = String.format("%-6s", postalCode).replace(' ', '0')
        println("Querying $URL_BASE/$FORECAST_CHART_PRODUCT_NAME/version__$productVersion/de/$postalCodeFormatted.json")
        url(urlString = "$URL_BASE/$FORECAST_CHART_PRODUCT_NAME/version__$productVersion/de/$postalCodeFormatted.json")
    }.body()

    private fun localDateTimeFromEpoch(timeSinceEpochMillis : Long) : LocalDateTime {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timeSinceEpochMillis),
                ZoneId.of("UTC")
        )
    }

    override suspend fun getPrecipitationRadarData(year: Int, month: Int, day: Int, hour: Int, mins: Int ) : List<PrecipitationRegion> {
        check(mins % 5 == 0 && mins >= 0 && mins <= 55) {
            "Invalid minutes input. Must be a multiple of 5 and between 0 and 55."
        }
        check(hour >= 0 && hour <= 23) {
            "Invalid hour input. Must be between 0 and 23."
        }

        // Query precipitation product version
        var precipitationProductVersion : String? = null
        try {
            precipitationProductVersion = queryApiProductVersion().get(PRECIPITATION_PRODUCT_NAME)
        } catch(e:Exception) {
            throw Exception("Error while obtaining meteo api versions. Error: ${e.message}.")
        }

        if (precipitationProductVersion == null) {
            throw Exception("Error while obtaining meteo precipitation api version. Error: precipitation product not found.")
        }

        // Query radar data at the specified date-time
        var radarData : RadarData? = null
        try {
            radarData = queryPrecipitationRadarData(precipitationProductVersion, year, month, day, hour, mins)
        } catch (e: Exception) {
            throw Exception("Error while obtaining meteo precipitation radar data. Error: ${e.message}")
        }

        val coordSystem = radarData.coords.system
        if(coordSystem != "LV95") {
            throw Exception("Error while obtaining meteo precipitation radar data. Error: only LV95 corod system is supported (received $coordSystem)")
        }

        // Extract precipitation regions
        val precipitationRegions : MutableList<PrecipitationRegion> = mutableListOf()
        for (area in radarData.areas) {
            val intensity = MAP_COLOR_TO_INTENSITY.get(area.color).let {
                if (it == null) {
                    println("Unrecognised color (#${area.color}) for precipitation intensity level")
                    -1
                } else it
            }

            for (shape in area.shapes) {
                // TODO handle polygons with interior holes (ie. shape size > 1). At the moment assuming filled polygon.
                check(shape.isNotEmpty())
                precipitationRegions.add(PrecipitationRegion(polygon = convertShapeToPolygonPath(shape[0], radarData.coords), intensity = intensity))
            }
        }

        return precipitationRegions.toList()
    }

    override suspend fun getTemperature(
        postalCode: Int,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        mins: Int
    ): Int {
        // Query forecast chart product version
        var forecastChartProductVersion : String? = null
        try {
            forecastChartProductVersion = queryApiProductVersion().get(FORECAST_CHART_PRODUCT_NAME)
        } catch(e:Exception) {
            throw Exception("Error while obtaining meteo api versions. Error: ${e.message}.")
        }

        if (forecastChartProductVersion == null) {
            throw Exception("Error while obtaining meteo forecast chart api version. Error: forecast chart product not found.")
        }

        // Query forecast chart at the specified postal code
        var forecastChart : List<ForecastChart>? = null
        try {
            forecastChart = queryForecastChart(forecastChartProductVersion, postalCode)
        } catch (e: Exception) {
            throw Exception("Error while obtaining meteo forecast chart (CP $postalCode). Error: ${e.message}")
        }

        val queryTime = LocalDateTime.of(year, month, day, hour, mins)

        // Check if queried time is within forecast
        val dayForecast = forecastChart.find{forecastChart ->
            val minTime = localDateTimeFromEpoch(forecastChart.min_date).minusSeconds(1)
            val maxTime = localDateTimeFromEpoch(forecastChart.max_date).plusSeconds(1)
            queryTime.isAfter(minTime) && queryTime.isBefore(maxTime) }
        if(dayForecast == null) {
            throw Exception("Error while obtaining meteo forecast chart temperature (CP $postalCode). Error: no forecast available for queried time.")
        }

        // Find temperature interval.
        val temperatureInterval = dayForecast.temperature.windowed(2).find {
            temperatureInterval ->
            val minTime = localDateTimeFromEpoch(temperatureInterval.get(0).get(0).toLong())
            val maxTime = localDateTimeFromEpoch(temperatureInterval.get(1).get(0).toLong())
            queryTime.isAfter(minTime.minusSeconds(1)) && queryTime.isBefore(maxTime.plusSeconds(1))
        }

        if(temperatureInterval == null) {
            throw Exception("Error while obtaining meteo forecast chart temperature. Error: could not find day interval temperature for queried time.")
        }

        // TODO interpolate instead of average.
        val temperature = (temperatureInterval.get(0).get(1) + temperatureInterval.get(1).get(1)) / 2.0
        println("Forecast chart temperature at $queryTime -> $temperature C")

        return temperature.toInt()
    }
}