package dvansa.cankom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dvansa.cankom.main.MainScreen
import dvansa.cankom.useredit.UserEditScreen
import kotlinx.coroutines.CoroutineScope


@Composable
fun NavigationGraph(
    modifier: Modifier = Modifier,
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
            MainScreen(onEditButton = { navActions.navigateToUserEdit() }, modifier = modifier)
        }
        composable(NavDestinations.USER_EDIT_ROUTE) {
            UserEditScreen(onSaveUserParameters = {}, onBack = { navActions.navigateToMain() }, modifier = modifier)
        }
    }
}