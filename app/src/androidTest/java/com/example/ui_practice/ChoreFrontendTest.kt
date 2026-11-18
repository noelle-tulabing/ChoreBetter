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
        compose.onNodeWithTag("complete-trash").performClick()
        compose.onNodeWithTag("balance").assertTextEquals("20 points")
        compose.onNodeWithTag("complete-trash").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Rewards").performClick()
        compose.onNodeWithTag("redeem-movie").assertIsNotEnabled()
        compose.onNodeWithTag("redeem-dessert").performClick()
        compose.onNodeWithTag("confirmRedeem").performClick()
        compose.onNodeWithTag("balance").assertTextEquals("0 points")
        compose.onNodeWithText("Your redeemed rewards").assertExists()
        compose.onNodeWithTag("redeem-dessert").assertIsNotEnabled()
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
