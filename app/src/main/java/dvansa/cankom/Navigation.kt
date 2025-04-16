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

import androidx.navigation.NavHostController
import dvansa.cankom.NavScreens.MAIN_SCREEN
import dvansa.cankom.NavScreens.MAP_ROUTE_SCREEN
import dvansa.cankom.NavScreens.TIME_EDIT_SCREEN
import dvansa.cankom.NavScreens.USER_EDIT_SCREEN

private object NavScreens {
    const val MAIN_SCREEN = "main"
    const val USER_EDIT_SCREEN = "edit"
    const val TIME_EDIT_SCREEN= "time_edit"
    const val MAP_ROUTE_SCREEN = "map_route"
}

object NavDestinations {
    const val MAIN_ROUTE = MAIN_SCREEN
    const val USER_EDIT_ROUTE = USER_EDIT_SCREEN
    const val TIME_EDIT_ROUTE = TIME_EDIT_SCREEN
    const val MAP_ROUTE_ROUTE = MAP_ROUTE_SCREEN
}

class NavActions(private val navController: NavHostController) {
    fun navigateToMain() {
        navController.navigate(NavDestinations.MAIN_ROUTE)
    }

    fun navigateToUserEdit() {
        navController.navigate(NavDestinations.USER_EDIT_ROUTE)
    }

    fun navigateToTimeEdit() {
        navController.navigate(NavDestinations.TIME_EDIT_ROUTE)
    }

    fun navigateToMapRoute() {
        navController.navigate(NavDestinations.MAP_ROUTE_ROUTE)
    }
}