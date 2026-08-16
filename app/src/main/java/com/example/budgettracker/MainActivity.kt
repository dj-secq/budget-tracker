package com.example.budgettracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.ui.add.AddTransactionScreen
import com.example.budgettracker.ui.add.AddTransactionViewModel
import com.example.budgettracker.ui.analytics.AnalyticsScreen
import com.example.budgettracker.ui.analytics.AnalyticsViewModel
import com.example.budgettracker.ui.assign.AssignBudgetScreen
import com.example.budgettracker.ui.assign.AssignBudgetViewModel
import com.example.budgettracker.ui.debt.DebtTrackerScreen
import com.example.budgettracker.ui.debt.DebtTrackerViewModel
import com.example.budgettracker.ui.home.HomeScreen
import com.example.budgettracker.ui.home.HomeViewModel
import com.example.budgettracker.di.AppContainer
import com.example.budgettracker.ui.settings.CategoryManagementScreen
import com.example.budgettracker.ui.settings.CategoryManagementViewModel
import com.example.budgettracker.ui.settings.SettingsScreen
import com.example.budgettracker.ui.settings.SettingsViewModel
import com.example.budgettracker.ui.settings.WalletManagementScreen
import com.example.budgettracker.ui.settings.WalletManagementViewModel
import com.example.budgettracker.ui.settings.RecurringTransactionsScreen
import com.example.budgettracker.ui.settings.RecurringTransactionsViewModel
import com.example.budgettracker.ui.theme.BudgetTrackerTheme
import com.example.budgettracker.ui.transactions.TransactionsScreen
import com.example.budgettracker.ui.transactions.TransactionsViewModel
import com.example.budgettracker.ui.goals.GoalsScreen
import com.example.budgettracker.ui.goals.GoalsViewModel
import com.example.budgettracker.ui.edit.EditTransactionScreen
import com.example.budgettracker.ui.edit.EditTransactionViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        val appContainer = (application as BudgetTrackerApplication).container
        
        setContent {
            val userPreferences = appContainer.userPreferencesRepository.budgetRulePreferencesFlow
                .collectAsState(initial = null).value
                
            BudgetTrackerTheme(themeMode = userPreferences?.themeMode ?: com.example.budgettracker.data.repository.ThemeMode.SYSTEM) {
                BudgetApp(appContainer)
            }
        }
    }
}

@Composable
fun BudgetApp(appContainer: com.example.budgettracker.di.AppContainer) {
    val navController = rememberNavController()
    
    // Provide ViewModel factory
    val factory = remember {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                    return HomeViewModel(appContainer.budgetRepository, appContainer.userPreferencesRepository) as T
                }
                if (modelClass.isAssignableFrom(AddTransactionViewModel::class.java)) {
                    return AddTransactionViewModel(appContainer.budgetRepository, appContainer.userPreferencesRepository) as T
                }
                if (modelClass.isAssignableFrom(EditTransactionViewModel::class.java)) {
                    return EditTransactionViewModel(appContainer.budgetRepository, appContainer.userPreferencesRepository) as T
                }
                if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
                    return AnalyticsViewModel(appContainer.budgetRepository, appContainer.userPreferencesRepository) as T
                }
                if (modelClass.isAssignableFrom(AssignBudgetViewModel::class.java)) {
                    return AssignBudgetViewModel(appContainer.budgetRepository, appContainer.userPreferencesRepository) as T
                }
                if (modelClass.isAssignableFrom(DebtTrackerViewModel::class.java)) {
                    return DebtTrackerViewModel(appContainer.budgetRepository) as T
                }
                if (modelClass.isAssignableFrom(TransactionsViewModel::class.java)) {
                    return TransactionsViewModel(appContainer.budgetRepository) as T
                }
                if (modelClass.isAssignableFrom(GoalsViewModel::class.java)) {
                    return GoalsViewModel(appContainer.budgetRepository) as T
                }
                if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                    return SettingsViewModel(appContainer.userPreferencesRepository, appContainer.budgetRepository) as T
                }
                if (modelClass.isAssignableFrom(WalletManagementViewModel::class.java)) {
                    return WalletManagementViewModel(appContainer.budgetRepository) as T
                }
                if (modelClass.isAssignableFrom(CategoryManagementViewModel::class.java)) {
                    return CategoryManagementViewModel(appContainer.budgetRepository) as T
                }
                if (modelClass.isAssignableFrom(RecurringTransactionsViewModel::class.java)) {
                    return RecurringTransactionsViewModel(appContainer.budgetRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
    

    val routeOrder = listOf("home", "transactions", "analytics", "goals")
    fun isMainTab(route: String?) = routeOrder.contains(route?.substringBefore("/"))
    fun getRouteIndex(route: String?) = routeOrder.indexOf(route?.substringBefore("/")).let { if (it == -1) 0 else it }

    Scaffold(
        bottomBar = {
            // ... (bottom bar code remains same)
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.tab_home)) },
                    label = { Text(stringResource(R.string.tab_home)) },
                    selected = currentDestination?.hierarchy?.any { it.route == "home" } == true,
                    onClick = {
                        if (currentDestination?.route != "home") {
                            if (!isMainTab(currentDestination?.route)) {
                                navController.popBackStack("home", inclusive = false)
                            } else {
                                navController.navigate("home") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.List, contentDescription = stringResource(R.string.tab_transactions)) },
                    label = { Text(stringResource(R.string.tab_transactions)) },
                    selected = currentDestination?.hierarchy?.any { it.route == "transactions" } == true,
                    onClick = {
                        navController.navigate("transactions") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.PieChart, contentDescription = stringResource(R.string.tab_analytics)) },
                    label = { Text(stringResource(R.string.tab_analytics)) },
                    selected = currentDestination?.hierarchy?.any { it.route == "analytics" } == true,
                    onClick = {
                        navController.navigate("analytics") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.tab_goals)) },
                    label = { Text(stringResource(R.string.tab_goals)) },
                    selected = currentDestination?.hierarchy?.any { it.route == "goals" } == true,
                    onClick = {
                        navController.navigate("goals") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
            enterTransition = {
                if (isMainTab(initialState.destination.route) && isMainTab(targetState.destination.route)) {
                    val initialIndex = getRouteIndex(initialState.destination.route)
                    val targetIndex = getRouteIndex(targetState.destination.route)
                    val direction = if (targetIndex > initialIndex) AnimatedContentTransitionScope.SlideDirection.Left else AnimatedContentTransitionScope.SlideDirection.Right
                    slideIntoContainer(towards = direction, animationSpec = tween(300))
                } else {
                    slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300))
                }
            },
            exitTransition = {
                if (isMainTab(initialState.destination.route) && isMainTab(targetState.destination.route)) {
                    val initialIndex = getRouteIndex(initialState.destination.route)
                    val targetIndex = getRouteIndex(targetState.destination.route)
                    val direction = if (targetIndex > initialIndex) AnimatedContentTransitionScope.SlideDirection.Left else AnimatedContentTransitionScope.SlideDirection.Right
                    slideOutOfContainer(towards = direction, animationSpec = tween(300))
                } else {
                    slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300))
                }
            },
            popEnterTransition = {
                slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300))
            }
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel(factory = factory),
                    onNavigateToAddTransaction = { navController.navigate("add_transaction") },
                    onNavigateToAssignBudget = { navController.navigate("assign_budget") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToDebtTracker = { navController.navigate("debt_tracker") }
                )
            }
            composable("transactions") {
                val transactionsViewModel: TransactionsViewModel = viewModel(factory = factory)
                TransactionsScreen(
                    viewModel = transactionsViewModel,
                    onEditTransaction = { id -> navController.navigate("edit_transaction/$id") }
                )
            }
            composable("analytics") {
                val analyticsViewModel: AnalyticsViewModel = viewModel(factory = factory)
                AnalyticsScreen(viewModel = analyticsViewModel)
            }
            composable("add_transaction") {
                val addTransactionViewModel: AddTransactionViewModel = viewModel(factory = factory)
                AddTransactionScreen(
                    viewModel = addTransactionViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "edit_transaction/{transactionId}",
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: return@composable
                val editTransactionViewModel: EditTransactionViewModel = viewModel(factory = factory)
                EditTransactionScreen(
                    transactionId = transactionId,
                    viewModel = editTransactionViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("assign_budget") {
                val assignBudgetViewModel: AssignBudgetViewModel = viewModel(factory = factory)
                AssignBudgetScreen(
                    viewModel = assignBudgetViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("goals") {
                val goalsViewModel: GoalsViewModel = viewModel(factory = factory)
                GoalsScreen(viewModel = goalsViewModel)
            }
            composable("settings") {
                val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToWallets = { navController.navigate("wallet_management") },
                    onNavigateToCategories = { navController.navigate("category_management") },
                    onNavigateToRecurring = { navController.navigate("recurring_transactions") }
                )
            }
            composable("wallet_management") {
                val walletViewModel: WalletManagementViewModel = viewModel(factory = factory)
                WalletManagementScreen(
                    viewModel = walletViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("category_management") {
                val categoryViewModel: CategoryManagementViewModel = viewModel(factory = factory)
                CategoryManagementScreen(
                    viewModel = categoryViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("recurring_transactions") {
                val recurringViewModel: RecurringTransactionsViewModel = viewModel(factory = factory)
                RecurringTransactionsScreen(
                    viewModel = recurringViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("debt_tracker") {
                val viewModel: DebtTrackerViewModel = viewModel(factory = factory)
                DebtTrackerScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}