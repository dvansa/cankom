package dvansa.cankom

import androidx.navigation.NavHostController
import dvansa.cankom.NavScreens.MAIN_SCREEN
import dvansa.cankom.NavScreens.USER_EDIT_SCREEN

private object NavScreens {
    const val MAIN_SCREEN = "main"
    const val USER_EDIT_SCREEN = "edit"
}

object NavDestinations {
    const val MAIN_ROUTE = MAIN_SCREEN
    const val USER_EDIT_ROUTE = USER_EDIT_SCREEN
}

class NavActions(private val navController: NavHostController) {
    fun navigateToMain() {
        navController.navigate(NavDestinations.MAIN_ROUTE)
    }

    fun navigateToUserEdit() {
        navController.navigate(NavDestinations.USER_EDIT_ROUTE)
    }
}