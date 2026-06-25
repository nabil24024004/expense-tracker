package com.example.ui.screens

import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseInOutCubic
import com.example.MainViewModel

@Composable
fun AppNavigator(viewModel: MainViewModel, activity: FragmentActivity) {
    val navController = rememberNavController()
    
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()
    val biometricsEnabled by viewModel.biometricsEnabled.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()


    val startDestination = remember(isFirstLaunch, biometricsEnabled, isAuthenticated) {
        when {
            isFirstLaunch -> "onboarding"
            biometricsEnabled && !isAuthenticated -> "auth"
            else -> "home"
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(500, easing = EaseInOutCubic)
            ) + fadeIn(animationSpec = tween(500))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(500, easing = EaseInOutCubic)
            ) + fadeOut(animationSpec = tween(500))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(500, easing = EaseInOutCubic)
            ) + fadeIn(animationSpec = tween(500))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(500, easing = EaseInOutCubic)
            ) + fadeOut(animationSpec = tween(500))
        }
    ) {
        composable("onboarding") {
            OnboardingScreen(
                viewModel = viewModel,
                onOnboardingComplete = {
                    val target = if (viewModel.biometricsEnabled.value) "auth" else "home"
                    navController.navigate(target) {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("auth") {
            AuthScreen(
                activity = activity,
                onAuthenticated = {
                    viewModel.authenticate()
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }
        
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                activity = activity
            )
        }
    }
}

