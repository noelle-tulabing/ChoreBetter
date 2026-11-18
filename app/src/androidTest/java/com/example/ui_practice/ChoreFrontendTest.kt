package com.example.ui_practice

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui_practice.data.ChoreState
import com.example.ui_practice.ui.theme.UI_practiceTheme
import org.junit.Rule
import org.junit.Test

class ChoreFrontendTest {
    @get:Rule val compose = createComposeRule()
    private fun launch() {
        compose.setContent {
            var state by remember { mutableStateOf(ChoreState.demo()) }
            UI_practiceTheme { ChoreApp(state) { state = it } }
        }
    }
    @Test fun completeChoreAndRedeemReward() {
        launch()
        compose.onNodeWithTag("complete-trash").performTouchInput { click(center) }
        compose.onNodeWithTag("balance").assertTextEquals("20 points")
        compose.onNodeWithContentDescription("Uncheck Take out the trash").assertExists()
        compose.onNodeWithContentDescription("Rewards").performClick()
        compose.onNodeWithTag("redeem-movie").assertIsNotEnabled()
        compose.onNodeWithTag("redeem-dessert").performClick()
        compose.onNodeWithTag("confirmRedeem").performClick()
        compose.onNodeWithTag("balance").assertTextEquals("0 points")
        compose.onNodeWithText("Your redeemed rewards").assertExists()
        compose.onNodeWithTag("redeem-dessert").assertIsNotEnabled()
    }
    @Test fun mineIsSelectedByDefaultAndEveryoneShowsOtherMembers() {
        launch()
        compose.onNodeWithText("Mine").assertIsSelected()
        compose.onNodeWithText("Do the dishes").assertDoesNotExist()
        val mine = compose.onNodeWithText("Mine").fetchSemanticsNode().boundsInRoot
        val everyone = compose.onNodeWithText("Everyone").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(mine.left < everyone.left)
        compose.onNodeWithText("Everyone").performClick()
        compose.onNodeWithTag("complete-dishes").performTouchInput { click(center) }
        compose.onNodeWithText("Only Noelle can complete this chore").assertExists()
        compose.onNodeWithTag("balance").assertTextEquals("0 points")
    }
    @Test fun futureChoreExplainsRestrictionAndTodayRestoresCompletion() {
        launch()
        compose.onNodeWithContentDescription("Next week").performClick()
        compose.onNodeWithTag("complete-dog").performTouchInput { click(center) }
        compose.onNodeWithText("Choose Today to complete a chore").assertExists()
        compose.onNodeWithTag("balance").assertTextEquals("0 points")
        compose.onNodeWithTag("todayButton").performClick()
        compose.onNodeWithTag("complete-dog").performTouchInput { click(center) }
        compose.onNodeWithTag("balance").assertTextEquals("10 points")
        compose.onNodeWithContentDescription("Uncheck Feed the dog").assertExists()
    }
    @Test fun uncheckReversesPointsAndRecheckingDoesNotDuplicateThem() {
        launch()
        compose.onNodeWithTag("complete-dog").performTouchInput { click(center) }
        compose.onNodeWithTag("balance").assertTextEquals("10 points")
        compose.onNodeWithContentDescription("Uncheck Feed the dog").performTouchInput { click(center) }
        compose.onNodeWithTag("balance").assertTextEquals("0 points")
        compose.onNodeWithContentDescription("Complete Feed the dog").performTouchInput { click(center) }
        compose.onNodeWithTag("balance").assertTextEquals("10 points")
    }
    @Test fun managerEditsChoreAndMemberCannotSeeEditControls() {
        launch()
        compose.onNodeWithTag("edit-dog").performClick()
        compose.onNodeWithTag("entryName").assertTextContains("Feed the dog")
        compose.onNodeWithTag("entryName").performTextReplacement("Walk the dog")
        compose.onNodeWithTag("entryPoints").performTextReplacement("25")
        compose.onNodeWithTag("saveEntry").performClick()
        compose.onNodeWithText("Walk the dog").assertExists()
        compose.onNodeWithTag("complete-dog").performTouchInput { click(center) }
        compose.onNodeWithTag("balance").assertTextEquals("25 points")
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.onNodeWithText("Noelle").performClick()
        compose.onNodeWithTag("edit-dishes").assertDoesNotExist()
    }
    @Test fun cancelEditLeavesChoreUnchanged() {
        launch()
        compose.onNodeWithTag("edit-dog").performClick()
        compose.onNodeWithTag("entryName").performTextReplacement("Changed")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Feed the dog").assertExists()
        compose.onNodeWithText("Changed").assertDoesNotExist()
    }
    @Test fun createChoreValidatesNameAndDisplaysSavedChore() {
        launch()
        compose.onNodeWithTag("addChore").performClick()
        compose.onNodeWithTag("saveEntry").performClick()
        compose.onNodeWithTag("formError").assertExists()
        compose.onNodeWithTag("entryName").performTextInput("Water the plants")
        compose.onNodeWithTag("saveEntry").performClick()
        compose.onNodeWithText("Water the plants").assertExists()
    }
    @Test fun privateListIsHiddenUntilManagerSharesIt() {
        launch()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.onNodeWithText("Create list").performClick()
        compose.onNodeWithTag("entryName").performTextInput("Kitchen")
        compose.onNodeWithTag("saveEntry").performClick()
        compose.onNodeWithText("No chores here").assertExists()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.onNodeWithText("Noelle").performClick()
        compose.onNodeWithText("Household chores ▾").performClick()
        compose.onAllNodesWithText("Kitchen", substring = true).assertCountEquals(0)
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.onNodeWithText("Alex").performClick()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.onNodeWithText("Share current list").performClick()
        compose.onNodeWithText("Noelle — share").performClick()
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.onNodeWithText("Noelle").performClick()
        compose.onNodeWithText("Kitchen ▾").assertExists()
        compose.onNodeWithTag("addChore").assertDoesNotExist()
    }
}
