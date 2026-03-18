package jafproductions.jaflist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import jafproductions.jaflist.ui.AuthScreen
import jafproductions.jaflist.ui.FolderScreen
import jafproductions.jaflist.ui.MainScreen
import jafproductions.jaflist.ui.RestoreBackupScreen
import jafproductions.jaflist.ui.theme.JAFListTheme
import jafproductions.jaflist.viewmodels.AppViewModel
import jafproductions.jaflist.viewmodels.AuthViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JAFListTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isSignedIn by authViewModel.isSignedIn.collectAsState()

                    LaunchedEffect(isSignedIn) {
                        if (isSignedIn) {
                            appViewModel.initializeCloud()
                        }
                    }

                    if (isSignedIn) {
                        AppNavigation(
                            appViewModel = appViewModel,
                            authViewModel = authViewModel
                        )
                    } else {
                        AuthScreen(authViewModel = authViewModel)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (authViewModel.isSignedIn.value) {
            appViewModel.initializeCloud()
        }
    }
}

@Composable
fun AppNavigation(
    appViewModel: AppViewModel,
    authViewModel: AuthViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                navController = navController,
                appViewModel = appViewModel,
                authViewModel = authViewModel
            )
        }
        composable("restore") {
            RestoreBackupScreen(
                appViewModel = appViewModel,
                navController = navController
            )
        }
        composable(
            route = "folder/{folderId}",
            arguments = listOf(
                navArgument("folderId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId") ?: return@composable
            FolderScreen(
                folderId = folderId,
                appViewModel = appViewModel,
                navController = navController
            )
        }
        composable(
            route = "subfolder/{parentFolderId}/{folderId}",
            arguments = listOf(
                navArgument("parentFolderId") { type = NavType.StringType },
                navArgument("folderId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val parentFolderId = backStackEntry.arguments?.getString("parentFolderId") ?: return@composable
            val folderId = backStackEntry.arguments?.getString("folderId") ?: return@composable
            FolderScreen(
                folderId = folderId,
                parentFolderId = parentFolderId,
                appViewModel = appViewModel,
                navController = navController
            )
        }
    }
}
