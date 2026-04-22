package com.pce.itassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pce.itassistant.ui.screens.AdminChatbotFilesScreen
import com.pce.itassistant.ui.screens.AdminScreen
import com.pce.itassistant.ui.screens.AdminTnpCompanyDetailScreen
import com.pce.itassistant.ui.screens.AdminTnpScreen
import com.pce.itassistant.ui.screens.AdminUsersScreen
import com.pce.itassistant.ui.screens.ChatScreen
import com.pce.itassistant.ui.screens.HomeScreen
import com.pce.itassistant.ui.screens.LoginScreen
import com.pce.itassistant.ui.screens.MockTestResultScreen
import com.pce.itassistant.ui.screens.MockTestScreen
import com.pce.itassistant.ui.screens.RegistrationScreen
import com.pce.itassistant.ui.screens.StudentSheetScreen
import com.pce.itassistant.ui.screens.StudentTnpCompanyDetailScreen
import com.pce.itassistant.ui.screens.StudentTnpScreen
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

                    val savedStudent = sessionManager.getStudent()
                    val startDestination = when {
                        !sessionManager.isLoggedIn() -> "login"
                        savedStudent?.role.equals("admin", ignoreCase = true) -> "admin_screen"
                        else -> "home"
                    }

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

                        composable("admin_screen") {
                            AdminScreen(navController)
                        }

                        composable("admin_users") {
                            AdminUsersScreen(navController)
                        }

                        composable("admin_student_sheet") {
                            StudentSheetScreen(navController = navController)
                        }

                        composable("admin_tnp") {
                            AdminTnpScreen(navController)
                        }

                        composable(
                            route = "admin_tnp_detail/{companyId}",
                            arguments = listOf(
                                navArgument("companyId") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val companyId =
                                backStackEntry.arguments?.getInt("companyId") ?: return@composable

                            AdminTnpCompanyDetailScreen(
                                navController = navController,
                                companyId = companyId
                            )
                        }

                        composable("admin_chatbot_files") {
                            AdminChatbotFilesScreen(navController)
                        }

                        composable("student_tnp") {
                            StudentTnpScreen(navController)
                        }

                        composable(
                            route = "student_tnp_detail/{companyId}",
                            arguments = listOf(
                                navArgument("companyId") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val companyId =
                                backStackEntry.arguments?.getInt("companyId") ?: return@composable

                            StudentTnpCompanyDetailScreen(
                                navController = navController,
                                companyId = companyId
                            )
                        }

                        composable(
                            route = "mock_test/{mockTestId}",
                            arguments = listOf(
                                navArgument("mockTestId") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val mockTestId =
                                backStackEntry.arguments?.getInt("mockTestId") ?: return@composable

                            MockTestScreen(
                                navController = navController,
                                mockTestId = mockTestId
                            )
                        }

                        composable(
                            route = "mock_test_result/{mockTestId}/{score}/{totalMarks}/{correctAnswers}/{wrongAnswers}/{attemptedQuestions}/{totalQuestions}",
                            arguments = listOf(
                                navArgument("mockTestId") { type = NavType.IntType },
                                navArgument("score") { type = NavType.IntType },
                                navArgument("totalMarks") { type = NavType.IntType },
                                navArgument("correctAnswers") { type = NavType.IntType },
                                navArgument("wrongAnswers") { type = NavType.IntType },
                                navArgument("attemptedQuestions") { type = NavType.IntType },
                                navArgument("totalQuestions") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val mockTestId = backStackEntry.arguments?.getInt("mockTestId") ?: return@composable
                            val score = backStackEntry.arguments?.getInt("score") ?: return@composable
                            val totalMarks = backStackEntry.arguments?.getInt("totalMarks") ?: return@composable
                            val correctAnswers = backStackEntry.arguments?.getInt("correctAnswers") ?: return@composable
                            val wrongAnswers = backStackEntry.arguments?.getInt("wrongAnswers") ?: return@composable
                            val attemptedQuestions = backStackEntry.arguments?.getInt("attemptedQuestions") ?: return@composable
                            val totalQuestions = backStackEntry.arguments?.getInt("totalQuestions") ?: return@composable

                            MockTestResultScreen(
                                navController = navController,
                                mockTestId = mockTestId,
                                score = score,
                                totalMarks = totalMarks,
                                correctAnswers = correctAnswers,
                                wrongAnswers = wrongAnswers,
                                attemptedQuestions = attemptedQuestions,
                                totalQuestions = totalQuestions
                            )
                        }
                    }
                }
            }
        }
    }
}
