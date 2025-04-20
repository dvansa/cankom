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

// Constants
const val HTTP_TIMEOUT_IN_MILLISECONDS = 5000L
const val URL_BASE = "https://www.meteoswiss.admin.ch/product/output"
const val URL_SUB_VERSION = "versions.json"
const val PRECIPITATION_PRODUCT_NAME = "inca/precipitation/rate"

// Response JSON data
@Serializable
data class RadarDataCoordinates(
    val system : String,
    val x_min : Float,
    val x_max : Float,
    val x_count : Int,
    val y_min : Float,
    val y_max : Float,
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

class MeteoClient(
    private val httpClient: HttpClient = getHttpClient()
) {
    private suspend fun queryApiProductVersion(): Map<String, String> = httpClient.get {
        url(urlString = "$URL_BASE/$URL_SUB_VERSION")
    }.body()

    private suspend fun queryPrecipitationRadarData(productVersion: String, year: Int, month: Int, day: Int, hour: Int, mins: Int): RadarData = httpClient.get {
        url(urlString = String.format("$URL_BASE/$PRECIPITATION_PRODUCT_NAME/version__$productVersion/rate_%04d%02d%02d_%02d%02d.json", year, month, day, hour, mins))
    }.body()

    suspend fun getPrecipitationRadarData(year: Int, month: Int, day: Int, hour: Int, mins: Int ) : RadarData {
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
        try {
            return queryPrecipitationRadarData(precipitationProductVersion, year, month, day, hour, mins)
        } catch (e: Exception) {
            throw Exception("Error while obtaining meteo precipitation radar data. Error: ${e.message}")
        }
    }
}