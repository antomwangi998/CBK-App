package com.helasacco.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.helasacco.app.di.SessionManager
import com.helasacco.app.ui.admin.*
import com.helasacco.app.ui.dashboard.DashboardScreen
import com.helasacco.app.ui.investments.InvestmentsScreen
import com.helasacco.app.ui.loans.*
import com.helasacco.app.ui.login.LoginScreen
import com.helasacco.app.ui.register.CustomerRegisterScreen
import com.helasacco.app.ui.members.*
import com.helasacco.app.ui.navigation.HelaBottomBar
import com.helasacco.app.ui.navigation.Routes
import com.helasacco.app.ui.reports.ReportsScreen
import com.helasacco.app.ui.theme.HelaSaccoTheme
import com.helasacco.app.ui.transactions.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by sessionManager.theme.collectAsStateWithLifecycle(initialValue = "system")
            val isLoggedIn by sessionManager.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)
            val darkTheme = when (theme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            HelaSaccoTheme(darkTheme = darkTheme) {
                HelaApp(isLoggedIn = isLoggedIn)
            }
        }
    }
}

private val bottomNavRoutes = setOf(
    Routes.DASHBOARD, Routes.MEMBER_LIST, Routes.LOAN_LIST, Routes.REPORTS, Routes.SETTINGS,
)

@Composable
fun HelaApp(isLoggedIn: Boolean) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { if (currentRoute in bottomNavRoutes) HelaBottomBar(navController) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) Routes.DASHBOARD else Routes.LOGIN,
            modifier = Modifier.padding(padding),
        ) {

            // ── Auth ──────────────────────────────────────────────────────────
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.DASHBOARD) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    },
                    onRegister = { navController.navigate(Routes.CUSTOMER_REGISTER) },
                )
            }

            composable(Routes.CUSTOMER_REGISTER) {
                CustomerRegisterScreen(
                    onBack = { navController.popBackStack() },
                    onSuccess = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.CUSTOMER_REGISTER) { inclusive = true }
                        }
                    },
                )
            }

            // ── Dashboard ─────────────────────────────────────────────────────
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onNavigate = { navController.navigate(it) },
                    onLogout = { navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } } },
                )
            }

            // ── Members ───────────────────────────────────────────────────────
            composable(Routes.MEMBER_LIST) {
                MemberListScreen(
                    onMemberClick = { navController.navigate(Routes.memberDetail(it)) },
                    onNewMember = { navController.navigate(Routes.MEMBER_NEW) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.MEMBER_NEW) {
                MemberRegistrationScreen(
                    onSuccess = { memberId ->
                        navController.navigate(Routes.memberDetail(memberId)) {
                            popUpTo(Routes.MEMBER_NEW) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                Routes.MEMBER_DETAIL,
                arguments = listOf(navArgument("memberId") { type = NavType.StringType }),
            ) { backStackEntry ->
                MemberDetailScreen(
                    memberId = backStackEntry.arguments?.getString("memberId") ?: "",
                    onEdit = { navController.navigate(Routes.memberEdit(it)) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                Routes.MEMBER_EDIT,
                arguments = listOf(navArgument("memberId") { type = NavType.StringType }),
            ) { backStackEntry ->
                // Edit reuses registration VM pre-populated — same composable, future enhancement
                MemberRegistrationScreen(
                    onSuccess = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }

            // ── Transactions ──────────────────────────────────────────────────
            composable(Routes.TRANSACTION_DEPOSIT) {
                DepositScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.TRANSACTION_WITHDRAWAL) {
                WithdrawalScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.TRANSACTION_TRANSFER) {
                TransferScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.TRANSACTION_LIST) {
                // List reuses member detail transaction history — placeholder for full list
                Surface(Modifier.fillMaxSize()) {
                    Text("All Transactions — use member profile for now", Modifier.padding(16.dp))
                }
            }
            composable(
                Routes.TRANSACTION_DETAIL,
                arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
            ) {
                Surface(Modifier.fillMaxSize()) { Text("Transaction Detail") }
            }

            // ── Loans ─────────────────────────────────────────────────────────
            composable(Routes.LOAN_LIST) {
                LoanListScreen(
                    onLoanClick = { navController.navigate(Routes.loanDetail(it)) },
                    onNewLoan = { navController.navigate(Routes.LOAN_NEW) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.LOAN_NEW) {
                LoanApplicationScreen(
                    memberId = null,
                    onSuccess = { loanId ->
                        navController.navigate(Routes.loanDetail(loanId)) {
                            popUpTo(Routes.LOAN_NEW) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                Routes.LOAN_DETAIL,
                arguments = listOf(navArgument("loanId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val loanId = backStackEntry.arguments?.getString("loanId") ?: ""
                LoanDetailScreen(
                    loanId = loanId,
                    onRepay = { navController.navigate(Routes.loanRepayment(it)) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                Routes.LOAN_REPAYMENT,
                arguments = listOf(navArgument("loanId") { type = NavType.StringType }),
            ) {
                // Repayment is a deposit with loan account pre-selected
                DepositScreen(onBack = { navController.popBackStack() })
            }

            // ── Admin ─────────────────────────────────────────────────────────
            composable(Routes.KYC_APPROVAL) {
                KycApprovalScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(onBack = { navController.popBackStack() })
            }

            // ── Settings ──────────────────────────────────────────────────────
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onLogout = { navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } } },
                    onBack = { navController.popBackStack() },
                )
            }

            // ── Reports ───────────────────────────────────────────────────────
            composable(Routes.REPORTS) {
                ReportsScreen(onBack = { navController.popBackStack() })
            }

            // ── AI Assistant ──────────────────────────────────────────────────
            composable(Routes.AI_ASSISTANT) {
                Surface(Modifier.fillMaxSize()) { Text("AI Assistant - Coming Soon", Modifier.padding(16.dp)) }
            }

            // ── Investments ───────────────────────────────────────────────────
            composable(Routes.INVESTMENTS) {
                InvestmentsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
