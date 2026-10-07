package com.example.govchatbotapp

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.govchatbotapp.ui.screens.LandingScreen
import com.example.govchatbotapp.ui.screens.ChatbotScreen
import com.example.govchatbotapp.ui.screens.TrackComplaintScreen
import com.example.govchatbotapp.ui.screens.LoginScreen
import com.example.govchatbotapp.ui.screens.AdminDashboardScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Login)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Login> {
          LoginScreen(
            onNavigateUser = { backStack.add(Landing) },
            onNavigateAdmin = { backStack.add(AdminDashboard) },
            modifier = Modifier
          )
        }
        entry<Landing> {
          LandingScreen(
            onNavigate = { navKey -> backStack.add(navKey) },
            onLogout = { 
                backStack.clear()
                backStack.add(Login) 
            },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<Chatbot> {
          ChatbotScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<TrackComplaint> {
          TrackComplaintScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<AdminDashboard> {
          AdminDashboardScreen(
            onLogout = { 
                backStack.clear()
                backStack.add(Login) 
            },
            modifier = Modifier.safeDrawingPadding()
          )
        }
      },
  )
}
