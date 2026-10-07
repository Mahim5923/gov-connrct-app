package com.example.govchatbotapp

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Login : NavKey
@Serializable data object Landing : NavKey
@Serializable data object Chatbot : NavKey
@Serializable data object TrackComplaint : NavKey
@Serializable data object AdminDashboard : NavKey
