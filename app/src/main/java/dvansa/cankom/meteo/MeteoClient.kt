package dvansa.cankom.meteo

import dvansa.cankom.model.MapPath

data class PrecipitationRegion (
    val polygon : MapPath,
    val intensity : Int = 0
)

interface MeteoClient {
    suspend fun getPrecipitationRadarData(year: Int, month: Int, day: Int, hour: Int, mins: Int ) : List<PrecipitationRegion>
}