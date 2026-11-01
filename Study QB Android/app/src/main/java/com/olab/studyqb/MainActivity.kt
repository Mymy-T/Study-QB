package com.olab.studyqb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.olab.studyqb.navigation.StudyQBBottomNavBar
import com.olab.studyqb.navigation.StudyQBNavGraph
import com.olab.studyqb.ui.theme.StudyQBTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudyQBTheme {
                StudyQBRoot()
            }
        }
    }
}

@Composable
private fun StudyQBRoot() {
    val navController = rememberNavController()
    val reviewResetKey = remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = { StudyQBBottomNavBar(navController, reviewResetKey) }
    ) { innerPadding ->
        // Each screen owns its own Scaffold/topBar; this outer padding only reserves space
        // for the bottom nav bar itself.
        Box(Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
            StudyQBNavGraph(navController = navController)
        }
    }
}
