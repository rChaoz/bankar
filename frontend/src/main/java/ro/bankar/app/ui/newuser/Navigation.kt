package ro.bankar.app.ui.newuser

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.navigation


enum class NewUserNav(val route: String) {
    Welcome("welcome"), SignIn("signIn"), SignUp("signUp");

    companion object {
        const val route = "newUser"
        val Start = Welcome
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.newUserNavigation(controller: NavHostController, onSuccess: () -> Unit) {
    navigation(startDestination = NewUserNav.Start.route, route = NewUserNav.route) {
        composable(NewUserNav.Welcome.route) {
            WelcomeScreen(
                onSignIn = { controller.navigate(NewUserNav.SignIn.route) },
                onSignUp = { controller.navigate(NewUserNav.SignUp.route) },
            )
        }
        composable(NewUserNav.SignIn.route) {
            LoginScreen(
                onSignUp = {
                    controller.navigate(NewUserNav.SignUp.route) { popUpTo(NewUserNav.Welcome.route) }
                },
                onSuccess = onSuccess,
            )
        }
        composable(NewUserNav.SignUp.route) {
            SignUpScreen(
                onSignIn = {
                    controller.navigate(NewUserNav.SignIn.route) { popUpTo(NewUserNav.Welcome.route) }
                },
                onSuccess = onSuccess,
            )
        }
    }
}