package com.pce.itassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pce.itassistant.ui.screens.ChatScreen
import com.pce.itassistant.ui.screens.HomeScreen
import com.pce.itassistant.ui.screens.LoginScreen
import com.pce.itassistant.ui.screens.RegistrationScreen
import com.pce.itassistant.ui.theme.PCEITAssistantTheme
import com.pce.itassistant.utils.SessionManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PCEITAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val sessionManager = remember { SessionManager(this@MainActivity) }
                    val startDestination = if (sessionManager.isLoggedIn()) "home" else "login"

                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("login") {
                            LoginScreen(navController)
                        }
                        composable("register") {
                            RegistrationScreen(navController)
                        }
                        composable("home") {
                            HomeScreen(navController)
                        }
                        composable("chatbot") {
                            ChatScreen(navController)
                        }
                    }
                }
            }
        }
    }
}

