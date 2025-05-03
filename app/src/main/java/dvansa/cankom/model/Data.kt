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

import androidx.datastore.core.Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class LatLng(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
)

typealias MapPath = List<LatLng>

@Serializable
data class TimePoint(
    val hour: Int = 0,
    val min: Int = 0,
)

@Serializable
data class CommuteParameters(
    val minTemperature: Int = 0,
    val maxTemperature: Int = 30,
    val leaveTime: TimePoint = TimePoint(hour = 22, min = 0),
    val arriveTime: TimePoint = TimePoint(hour = 22, min = 30),
)

@Serializable
data class DataModel(
    val commuteParams: CommuteParameters = CommuteParameters(),
    val route: MapPath = listOf(),
)

object DataModelSerializer : Serializer<DataModel> {
    override val defaultValue = DataModel()

    override suspend fun readFrom(input: InputStream): DataModel =
        Json.decodeFromString(
            DataModel.serializer(),
            input.readBytes().decodeToString(),
        )

    override suspend fun writeTo(
        dataModel: DataModel,
        output: OutputStream,
    ) {
        output.write(
            Json.encodeToString(DataModel.serializer(), dataModel).encodeToByteArray(),
        )
    }
}
