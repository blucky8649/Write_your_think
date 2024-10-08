package com.multimedia.writeyourthink.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.multimedia.writeyourthink.R
import com.multimedia.writeyourthink.ui.calendar.CalendarScreen
import com.multimedia.writeyourthink.ui.calendar.ROUTE_CALENDAR
import com.multimedia.writeyourthink.ui.calendar.navigateToCalendar
import com.multimedia.writeyourthink.ui.diaryadd.DiaryAddScreen
import com.multimedia.writeyourthink.ui.diarylist.DiaryListScreen
import com.multimedia.writeyourthink.ui.diaryadd.ROUTE_ADD_DIARY
import com.multimedia.writeyourthink.ui.diarylist.ROUTE_DIARY_LIST
import com.multimedia.writeyourthink.ui.diarylist.navigateToDiaryList
import com.multimedia.writeyourthink.ui.login.LoginScreen
import com.multimedia.writeyourthink.ui.login.ROUTE_LOGIN
import com.multimedia.writeyourthink.ui.login.navigateToLogin
import com.multimedia.writeyourthink.viewmodels.DiaryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
            val navController = rememberNavController()
            val viewModel = hiltViewModel<DiaryViewModel>()
            val uiState by viewModel.uiState.collectAsState()

            WytTheme {
                Scaffold(
                    bottomBar = {
                        val currentEntry by navController.currentBackStackEntryAsState()
                        val isLoginScreen = currentEntry?.destination?.route == ROUTE_LOGIN
                        if (isLoginScreen) return@Scaffold
                        BottomBar(selectedIndex) { item, index ->
                            navController.navigate(item.route)
                            selectedIndex = index
                        }
                    }
                ) { paddingValues ->
                    Surface(modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)) {
                        NavHost(
                            navController,
                            startDestination = ROUTE_LOGIN
                        ) {
                            composable(ROUTE_LOGIN) {
                                LoginScreen(
                                    onNavigateToDiary = navController::navigateToDiaryList
                                )
                            }

                            composable(ROUTE_DIARY_LIST) {
                                DiaryListScreen(onAddDiaryClick = {
                                    navController.navigate(ROUTE_ADD_DIARY)
                                })
                            }

                            composable(ROUTE_ADD_DIARY) {
                                DiaryAddScreen(
                                    onNavigateBack = navController::navigateUp,
                                    onAddButtonClicked = {

                                    }
                                )
                            }

                            composable(ROUTE_CALENDAR) {
                                CalendarScreen(
                                    countDate = uiState.countMap,
                                    onNavigateToLoginScreen = navController::navigateToLogin,
                                    userName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Undefined"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomBar(
    selectedIndex: Int,
    onItemClicked: (item: BottomNavItem, index: Int) -> Unit
) {
    NavigationBar {
        BottomNavItem.entries.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = index == selectedIndex,
                label = { BasicText(stringResource(item.titleResId)) },
                icon = { Icon(item.icon, contentDescription = stringResource(item.titleResId)) },
                onClick = { onItemClicked(item, index) }
            )
        }
    }
}

enum class BottomNavItem(
    val titleResId: Int, val icon: ImageVector, val route: String
) {
    List(R.string.list, Icons.AutoMirrored.Filled.List, ROUTE_DIARY_LIST),
    Calendar(R.string.calendar, Icons.Filled.CalendarMonth, ROUTE_CALENDAR)
}

@Composable
@Preview
fun GreetingPreview() {
    Text("Hello, World!!")
}