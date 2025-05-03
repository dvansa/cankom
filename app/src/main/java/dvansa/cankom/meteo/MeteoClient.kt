package dvansa.cankom.meteo

import dvansa.cankom.model.MapPath

data class PrecipitationRegion(
    val polygon: MapPath,
    val intensity: Int = 0,
)

interface MeteoClient {
    // Input time in GMT.
    suspend fun getPrecipitationRadarData(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        mins: Int,
    ): List<PrecipitationRegion>

    // Input time in GMT.
    suspend fun getTemperature(
        postalCode: Int,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        mins: Int,
    ): Int
}
