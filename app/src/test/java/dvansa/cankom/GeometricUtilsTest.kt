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
package dvansa.cankom

import dvansa.cankom.model.LatLng
import dvansa.cankom.model.bboxOverlap
import dvansa.cankom.model.getLatLngBoundingBox
import dvansa.cankom.model.linesIntersect
import dvansa.cankom.model.pointContainedInPolygon
import glm_.vec2.Vec2d
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometricUtilsTest {
    @Test
    fun bounding_box_overlap() {
        // 1x1 bounding box overlaps with another box offsetted by (0.5, 0.5)
        assertTrue(bboxOverlap(Vec2d(0.0, 0.0), Vec2d(1.0, 1.0), Vec2d(0.5, 0.5), Vec2d(1.5, 1.5)))
        // 1x1 bounding box doesn't overlap with another box offsetted by (1.0, 1.0)
        assertFalse(bboxOverlap(Vec2d(0.0, 0.0), Vec2d(1.0, 1.0), Vec2d(1.0, 1.0), Vec2d(2.0, 2.0)))
        // 1x1 bounding box overlaps when enclosed by a 2x2 box (without any edge intersection)
        assertTrue(bboxOverlap(Vec2d(-0.5, -0.5), Vec2d(0.5, 0.5), Vec2d(-1.0, -1.0), Vec2d(1.0, 1.0)))
        // Zero-area bounding box overlaps with 1x1 box.
        assertTrue(bboxOverlap(Vec2d(0.0, 0.0), Vec2d(0.0, 0.0), Vec2d(-0.5, -0.5), Vec2d(0.5, 0.5)))
    }

    @Test
    fun bounding_box_from_latitude_and_longitude() {
        val baseLatitude = 45.0
        val latLng = listOf(LatLng(baseLatitude, 0.0), LatLng(baseLatitude - 2.0, 2.0), LatLng(baseLatitude + 3.0, 1.0))

        // Base case
        val (bbMin, bbMax) = getLatLngBoundingBox(latLng)
        assertEquals(bbMin, Vec2d(43.0, 0.0))
        assertEquals(bbMax, Vec2d(48.0, 2.0))

        // Larger bounding box with 1km margin
        val (bbMinMargin, _) = getLatLngBoundingBox(latLng, marginInKm = 1.0)
        assertTrue(bbMinMargin.x < bbMin.x)
        assertTrue(bbMinMargin.y < bbMin.y)
    }

    @Test
    fun line_intersection() {
        // [0,0]->[1,1] horizontal segment does not intersect vertical segment [2,-1]->[2,1]
        assertFalse(linesIntersect(Vec2d(0.0, 0.0), Vec2d(1.0, 0.0), Vec2d(2.0, -1.0), Vec2d(2.0, 1.0)))
        // [0,0]->[3,1] horizontal segment intersects with vertical segment [2,-1]->[2,1]
        assertTrue(linesIntersect(Vec2d(0.0, 0.0), Vec2d(3.0, 0.0), Vec2d(2.0, -1.0), Vec2d(2.0, 1.0)))
    }

    @Test
    fun point_to_polygon_intersection() {
        // Check intersections with polygon:
        //  [0,2]-------------------[3,2]
        //   |                        |
        //   |      [1,1]---[2,1]     |
        //   |        |       |       |
        //  [0,0]---[1,0]   [2,0]---[3,0]
        val polygon =
            listOf(
                Vec2d(0.0, 0.0),
                Vec2d(0.0, 2.0),
                Vec2d(3.0, 2.0),
                Vec2d(3.0, 0.0),
                Vec2d(2.0, 0.0),
                Vec2d(2.0, 1.0),
                Vec2d(1.0, 1.0),
                Vec2d(1.0, 0.0),
            )
        // Check also that polygon winding order does not affect
        val reversePolygon = polygon.reversed()

        // Check points inside
        assertTrue(pointContainedInPolygon(Vec2d(0.5, 0.5), polygon))
        assertTrue(pointContainedInPolygon(Vec2d(0.5, 0.5), reversePolygon))

        assertTrue(pointContainedInPolygon(Vec2d(1.5, 1.5), polygon))
        assertTrue(pointContainedInPolygon(Vec2d(1.5, 1.5), reversePolygon))

        assertTrue(pointContainedInPolygon(Vec2d(2.1, 0.1), polygon))
        assertTrue(pointContainedInPolygon(Vec2d(2.1, 0.1), reversePolygon))

        // Check points outside
        assertFalse(pointContainedInPolygon(Vec2d(-0.5, 0.5), polygon))
        assertFalse(pointContainedInPolygon(Vec2d(-0.5, 0.5), reversePolygon))

        assertFalse(pointContainedInPolygon(Vec2d(1.5, 0.5), polygon))
        assertFalse(pointContainedInPolygon(Vec2d(1.5, 0.5), reversePolygon))

        assertFalse(pointContainedInPolygon(Vec2d(2.0, 2.1), polygon))
        assertFalse(pointContainedInPolygon(Vec2d(2.0, 2.1), reversePolygon))
    }
}
