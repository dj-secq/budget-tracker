package com.example.budgettracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.budgettracker.ui.analytics.WrappedScreen
import com.example.budgettracker.ui.analytics.WrappedViewModel
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
import com.example.budgettracker.widget.NextBillWidget
import androidx.navigation.NavType
import androidx.navigation.navArgument
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.budgettracker.ui.lock.AppLockScreen
import com.example.budgettracker.ui.lock.DeviceLock

class MainActivity : FragmentActivity() {
    private var unlocked by mutableStateOf(false)
    private var awaitingCredential = false
    private var biometricPrompt: BiometricPrompt? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission result handled
    }

    private val credentialLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        awaitingCredential = false
        if (result.resultCode == RESULT_OK) unlocked = true
    }

    override fun onResume() {
        super.onResume()
        NextBillWidget.refresh(this)
    }

    override fun onStop() {
        super.onStop()
        NextBillWidget.refresh(this)
        if (!awaitingCredential) unlocked = false
    }

    private fun requestUnlock() {
        if (awaitingCredential) return
        if (!DeviceLock.canLock(this)) {
            unlocked = true
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            awaitingCredential = true
            biometricPrompt = DeviceLock.prompt(
                activity = this,
                onSuccess = {
                    awaitingCredential = false
                    unlocked = true
                },
                onError = { awaitingCredential = false }
            )
        } else {
            val intent = DeviceLock.credentialIntent(this)
            if (intent == null) {
                unlocked = true
                return
            }
            awaitingCredential = true
            credentialLauncher.launch(intent)
        }
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
            val generalPreferences = appContainer.userPreferencesRepository.generalPreferencesFlow
                .collectAsState(initial = null).value
            val resources = androidx.compose.ui.platform.LocalContext.current.resources

            BudgetTrackerTheme(
                themeMode = userPreferences?.themeMode ?: com.example.budgettracker.data.repository.ThemeMode.SYSTEM,
                accent = userPreferences?.accent ?: com.example.budgettracker.data.repository.Accent.EMERALD,
                dynamicColor = userPreferences?.dynamicColor == true
            ) {
                when {
                    generalPreferences == null -> Unit
                    generalPreferences.appLockEnabled && !unlocked -> {
                        AppLockScreen(onUnlock = { requestUnlock() })
                        LaunchedEffect(Unit) { requestUnlock() }
                    }
                    else -> BudgetApp(appContainer, resources)
                }
            }
        }
    }
}

@Composable
fun BudgetApp(
    appContainer: com.example.budgettracker.di.AppContainer,
    resources: android.content.res.Resources
) {
    val navController = rememberNavController()
    
    // Provide ViewModel factory
    val factory = remember(resources) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                    return HomeViewModel(appContainer.budgetRepository, appContainer.userPreferencesRepository, resources) as T
                }
                if (modelClass.isAssignableFrom(AddTransactionViewModel::class.java)) {
                    return AddTransactionViewModel(appContainer.budgetRepository, appContainer.userPreferencesRepository) as T
                }
                if (modelClass.isAssignableFrom(EditTransactionViewModel::class.java)) {
                    return EditTransactionViewModel(appContainer.budgetRepository, appContainer.userPreferencesRepository) as T
                }
                if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
                    return AnalyticsViewModel(appContainer.budgetRepository, appContainer.userPreferencesRepository, resources) as T
                }
                if (modelClass.isAssignableFrom(WrappedViewModel::class.java)) {
                    return WrappedViewModel(
                        appContainer.budgetRepository,
                        appContainer.userPreferencesRepository,
                        resources
                    ) as T
                }
                if (modelClass.isAssignableFrom(AssignBudgetViewModel::class.java)) {
                    return AssignBudgetViewModel(appContainer.budgetRepository, appContainer.userPreferencesRepository) as T
                }
                if (modelClass.isAssignableFrom(DebtTrackerViewModel::class.java)) {
                    return DebtTrackerViewModel(
                        appContainer.budgetRepository,
                        appContainer.userPreferencesRepository
                    ) as T
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
    fun routeBase(route: String?) = route?.substringBefore("?")?.substringBefore("/")
    fun isMainTab(route: String?) = routeOrder.contains(routeBase(route))
    fun getRouteIndex(route: String?) = routeOrder.indexOf(routeBase(route)).let { if (it == -1) 0 else it }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = isMainTab(currentDestination?.route)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.tab_home)) },
                    label = { Text(stringResource(R.string.tab_home)) },
                    selected = routeBase(currentDestination?.route) == "home",
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
                    selected = routeBase(currentDestination?.route) == "transactions",
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
                    selected = routeBase(currentDestination?.route) == "analytics",
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
                    selected = routeBase(currentDestination?.route) == "goals",
                    onClick = {
                        navController.navigate("goals") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
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
                    onNavigateToDebtTracker = { navController.navigate("debt_tracker") },
                    onNavigateToRecurring = { navController.navigate("recurring_transactions") },
                    onOpenWallet = { accountId ->
                        navController.navigate("transactions?accountId=$accountId") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(
                route = "transactions?accountId={accountId}",
                arguments = listOf(
                    navArgument("accountId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val transactionsViewModel: TransactionsViewModel = viewModel(factory = factory)
                val accountId = backStackEntry.arguments?.getLong("accountId") ?: -1L
                androidx.compose.runtime.LaunchedEffect(accountId) {
                    if (accountId > 0L) transactionsViewModel.focusAccount(accountId)
                }
                TransactionsScreen(
                    viewModel = transactionsViewModel,
                    onEditTransaction = { id -> navController.navigate("edit_transaction/$id") },
                    onAddTransaction = { navController.navigate("add_transaction") }
                )
            }
            composable("analytics") {
                val analyticsViewModel: AnalyticsViewModel = viewModel(factory = factory)
                AnalyticsScreen(
                    viewModel = analyticsViewModel,
                    onNavigateToWrapped = { month, year -> navController.navigate("wrapped/$month/$year") }
                )
            }
            composable(
                route = "wrapped/{month}/{year}",
                arguments = listOf(
                    navArgument("month") { type = NavType.IntType },
                    navArgument("year") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val month = backStackEntry.arguments?.getInt("month") ?: 1
                val year = backStackEntry.arguments?.getInt("year") ?: 2026
                val wrappedViewModel: WrappedViewModel = viewModel(factory = factory)
                WrappedScreen(
                    month = month,
                    year = year,
                    viewModel = wrappedViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
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