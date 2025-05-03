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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dvansa.cankom.main.MainScreen
import dvansa.cankom.main.MainViewModel
import dvansa.cankom.maproute.MapRouteScreen
import dvansa.cankom.meteo.MeteoSwissClient
import dvansa.cankom.model.AppController
import dvansa.cankom.model.TimePoint
import dvansa.cankom.useredit.InitialState
import dvansa.cankom.useredit.UserEditScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
@Suppress("ktlint:standard:function-naming")
fun NavigationGraph(
    modifier: Modifier = Modifier,
    appController: AppController = AppController(MeteoSwissClient()),
    navController: NavHostController = rememberNavController(),
    navCorountineScope: CoroutineScope = rememberCoroutineScope(),
    navActions: NavActions = NavActions(navController),
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        navCorountineScope.launch {
            appController.loadModel(context)
            println("Loaded Model")
        }
    }
    NavHost(
        navController = navController,
        startDestination = NavDestinations.MAIN_ROUTE,
        modifier = modifier,
    ) {
        composable(
            NavDestinations.MAIN_ROUTE,
        ) {
            MainScreen(
                onEditButton = { navActions.navigateToUserEdit() },
                onMapRouteButton = { navActions.navigateToMapRoute() },
                onRefresh = { context, useCached ->
                    val startTime = System.currentTimeMillis()
                    appController.checkCommutePrecipitation(useCached = useCached)
                    val precipitationTime = System.currentTimeMillis()
                    appController.checkCommuteTemperatureRange(context = context, useCached = useCached)
                    val temperatureTime = System.currentTimeMillis()
                    println(
                        "Query times. Precipitation = ${precipitationTime - startTime} ms. Temperature = ${temperatureTime - precipitationTime} ms",
                    )
                },
                modifier = modifier,
                viewModel =
                    remember {
                        MainViewModel(
                            getAllowedTemperatureRange = {
                                val commuteParams = appController.getCommuteParameters()
                                Pair(commuteParams.minTemperature, commuteParams.maxTemperature)
                            },
                            getCommuteTemperatureRange = {
                                appController.checkCommuteTemperatureRange()
                            },
                            checkPrecipitationsInRoute = {
                                appController.checkCommutePrecipitation().let {
                                    it?.first?.isNotEmpty()
                                }
                            },
                            checkRouteAvailable = {
                                appController.getCommuteRoute().size > 1
                            },
                            getNextCommuteTime = {
                                // TODO assuming +2 GMT.
                                appController.getNextCommuteTimes(minutesResolution = 1).first().plusHours(2)
                            },
                        )
                    },
            )
        }
        composable(NavDestinations.USER_EDIT_ROUTE) {
            UserEditScreen(
                initialState =
                    {
                        val params = appController.getCommuteParameters()
                        InitialState(
                            minTemperature = params.minTemperature,
                            maxTemperature = params.maxTemperature,
                            leaveTime = params.leaveTime,
                            arriveTime = params.arriveTime,
                        )
                    }(),
                onSaveUserParameters = { minTemperature, maxTemperature, leaveTime, arriveTime ->
                    appController.setCommuteTime(
                        leaveTime = TimePoint(hour = leaveTime.hour, min = leaveTime.min),
                        arriveTime = TimePoint(hour = arriveTime.hour, min = arriveTime.min),
                    )
                    appController.setCommuteTemperatureRange(
                        minTemperature = minTemperature,
                        maxTemperature = maxTemperature,
                    )
                    navCorountineScope.launch {
                        appController.saveModel(context)
                        println("Saved Model")
                    }
                },
                onBack = {
                    navActions.navigateToMain()
                },
                modifier = modifier,
            )
        }

        composable(NavDestinations.MAP_ROUTE_ROUTE) {
            MapRouteScreen(
                initialState = dvansa.cankom.maproute.InitialState(route = appController.getCommuteRoute()),
                onSaveRoute = { newRoute ->
                    appController.setCommuteRoute(newRoute)
                    navCorountineScope.launch {
                        appController.saveModel(context)
                        println("Saved Model")
                    }
                },
                onBack = { navActions.navigateToMain() },
                checkPrecipitationCommute = {
                    appController.checkCommutePrecipitation()
                },
                checkTemperatureCommute = { context ->
                    appController.checkCommuteTemperatureRange(context)
                },
                modifier = modifier,
            )
        }
    }
}
