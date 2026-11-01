package com.olab.studyqb.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.olab.studyqb.PendingCardPrefill
import com.olab.studyqb.PendingResolvedSession
import com.olab.studyqb.ui.analyze.AnalyzeScreen
import com.olab.studyqb.ui.cardeditor.CardEditorScreen
import com.olab.studyqb.ui.cards.CardSelectionScreen
import com.olab.studyqb.ui.cards.CardsScreen
import com.olab.studyqb.ui.review.PracticeSetupScreen
import com.olab.studyqb.ui.review.buzzin.BuzzInScreen
import com.olab.studyqb.ui.review.classic.ClassicPracticeScreen
import com.olab.studyqb.ui.settings.SettingsScreen
import com.olab.studyqb.ui.stats.CardPerformanceScreen
import com.olab.studyqb.ui.stats.StatsScreen

@Composable
fun StudyQBNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.ANALYZE) {

        composable(Routes.ANALYZE) {
            AnalyzeScreen(
                onCardSaved = {
                    navController.navigate(Routes.cardEditor(cardId = null, usePrefill = true))
                }
            )
        }

        composable(Routes.CARDS) {
            CardsScreen(
                onCardClick = { card -> navController.navigate(Routes.cardEditor(cardId = card.id)) },
                onAddClick = { navController.navigate(Routes.cardEditor(cardId = null)) }
            )
        }

        composable(
            route = Routes.CARD_EDITOR_PATTERN,
            arguments = listOf(
                navArgument("cardId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("usePrefill") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val cardIdArg = backStackEntry.arguments?.getLong("cardId") ?: -1L
            val usePrefill = backStackEntry.arguments?.getBoolean("usePrefill") ?: false
            val prefill = remember { if (usePrefill) PendingCardPrefill.take() else PendingCardPrefill.Prefill() }
            CardEditorScreen(
                cardId = if (cardIdArg == -1L) null else cardIdArg,
                prefillAnswer = prefill.answer,
                prefillDetails = prefill.details,
                prefillCategory = prefill.category,
                onDone = { navController.popBackStack() }
            )
        }

        composable(Routes.CARD_SELECTION) {
            val initialIds = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<LongArray>("initialSelectedIds")
                ?.toSet()
                ?: emptySet()
            CardSelectionScreen(
                initialSelectedIds = initialIds,
                onDone = { selected ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("selectedIds", selected.toLongArray())
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.REVIEW_PATTERN,
            arguments = listOf(navArgument("resetKey") { type = NavType.IntType })
        ) { backStackEntry ->
            val selectedIdsResult by backStackEntry.savedStateHandle
                .getStateFlow<LongArray?>("selectedIds", null)
                .collectAsStateWithLifecycle()

            PracticeSetupScreen(
                onEditManualSelection = { currentIds ->
                    backStackEntry.savedStateHandle["initialSelectedIds"] = currentIds.toLongArray()
                    navController.navigate(Routes.CARD_SELECTION)
                },
                onStartClassic = { session ->
                    PendingResolvedSession.set(session)
                    navController.navigate(Routes.CLASSIC_PRACTICE)
                },
                onStartBuzzIn = { session ->
                    PendingResolvedSession.set(session)
                    navController.navigate(Routes.BUZZ_IN)
                },
                pendingManualSelectionResult = selectedIdsResult
            )
        }

        composable(Routes.CLASSIC_PRACTICE) {
            // `remember` is essential here: PendingResolvedSession.take() consumes a one-shot
            // value. Without remember, every recomposition of this composable (which happens
            // for reasons unrelated to navigation) would call take() again, get null the second
            // time, and immediately pop back out — which is exactly the bug that made "Start
            // Practice" bounce straight back to the setup screen.
            val session = remember { PendingResolvedSession.take() }
            if (session != null) {
                ClassicPracticeScreen(session = session, onDone = { navController.popBackStack() })
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        composable(Routes.BUZZ_IN) {
            val session = remember { PendingResolvedSession.take() }
            if (session != null) {
                BuzzInScreen(session = session, onDone = { navController.popBackStack() })
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        composable(Routes.STATS) {
            StatsScreen(onCardPerformanceClick = { navController.navigate(Routes.CARD_PERFORMANCE) })
        }

        composable(Routes.CARD_PERFORMANCE) {
            CardPerformanceScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen()
        }
    }
}
