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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dvansa.cankom.main.MainScreen
import dvansa.cankom.maproute.MapRouteScreen
import dvansa.cankom.model.AppController
import dvansa.cankom.model.TimePoint
import dvansa.cankom.useredit.InitialState
import dvansa.cankom.useredit.UserEditScreen
import kotlinx.coroutines.CoroutineScope


@Composable
fun NavigationGraph(
    modifier: Modifier = Modifier,
    appController : AppController = AppController(),
    navController: NavHostController = rememberNavController(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navActions : NavActions = NavActions(navController),
) {
    NavHost(
        navController = navController,
        startDestination = NavDestinations.MAIN_ROUTE,
        modifier = modifier
    ) {
        composable(
            NavDestinations.MAIN_ROUTE,
        ) {
            MainScreen(
                onEditButton = { navActions.navigateToUserEdit() },
                onMapRouteButton = { navActions.navigateToMapRoute()},
                modifier = modifier
            )
        }
        composable(NavDestinations.USER_EDIT_ROUTE) {
            UserEditScreen(
                initialState = {
                    val params = appController.getCommuteParameters()
                    InitialState(
                        minTemperature=params.minTemperature,
                        maxTemperature=params.maxTemperature,
                        leaveTime=params.leaveTime,
                        arriveTime=params.arriveTime
                    )
                }(),
                onSaveUserParameters = {
                    minTemperature, maxTemperature, leaveTime, arriveTime ->
                        appController.setCommuteTime(
                            leaveTime=TimePoint(hour=leaveTime.hour, min= leaveTime.min),
                            arriveTime=TimePoint(hour=arriveTime.hour, min= arriveTime.min)
                        )
                        appController.setCommuteTemperatureRange(
                            minTemperature=minTemperature,
                            maxTemperature=maxTemperature
                        )
                },
                onBack = {
                    navActions.navigateToMain()
                 },
                modifier = modifier)
        }
        composable(NavDestinations.MAP_ROUTE_ROUTE) {
            MapRouteScreen(
                initialState = dvansa.cankom.maproute.InitialState(route=appController.getCommuteRoute()),
                onSaveRoute = {newRoute -> appController.setCommuteRoute(newRoute)},
                onBack = { navActions.navigateToMain() },
                modifier = modifier
            )
        }
    }
}