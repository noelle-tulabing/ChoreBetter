package com.example.ui_practice

import com.example.ui_practice.data.*
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class ChoreRequirementsTest {
    private val day = 20000L
    private fun demo() = ChoreState.demo(day)
    private fun rejected(action: () -> Unit) { assertThrows(IllegalArgumentException::class.java) { action() } }

    @Test fun FR01_newListRemainsEmpty() {
        val state = demo().createList("alex", "Kitchen")
        assertEquals("Kitchen", state.lists.last().name)
        assertTrue(state.visibleChores("alex", state.lists.last().id).isEmpty())
    }
    @Test fun FR02_addedChoreRetainsFrequencyPointsAndAssignee() {
        val state = demo().addChore("alex", "home", "Sweep", "noelle", Frequency.WEEKLY, 25, day)
        val chore = state.chores.last()
        assertEquals("Sweep", chore.title); assertEquals("noelle", chore.assignee)
        assertEquals(Frequency.WEEKLY, chore.frequency); assertEquals(25, chore.points)
    }
    @Test fun FR03_FR06_sharingMakesSameListVisibleToExistingMember() {
        var state = demo().createList("alex", "Private")
        val id = state.lists.last().id
        assertFalse(state.canView("sam", id))
        state = state.share("alex", id, "sam")
        assertTrue(state.canView("sam", id))
        assertEquals(state.visibleChores("alex", id), state.visibleChores("sam", id))
    }
    @Test fun NFR01_unsharedListsCannotBeReadEvenById() {
        val state = demo().createList("alex", "Private")
        rejected { state.visibleChores("noelle", state.lists.last().id) }
        rejected { state.scheduled("noelle", state.lists.last().id, day) }
    }
    @Test fun FR06_unknownMemberCannotBeSharedWith() { rejected { demo().share("alex", "home", "missing") } }
    @Test fun memberCannotManageListsOrRewards() {
        rejected { demo().createList("sam", "Other") }
        rejected { demo().share("sam", "home", "riley") }
        rejected { demo().addChore("sam", "home", "Sweep", "sam", Frequency.DAILY, 10, day) }
        rejected { demo().addReward("sam", "home", "Prize", 10) }
    }
    @Test fun FR04_FR07_NFR02_completionAwardsPointsInSameStateUpdate() {
        val before = demo()
        val after = before.complete("alex", "dog", day, day)
        assertTrue(after.isDone("dog", day)); assertEquals(10, after.person("alex").points)
        assertEquals(0, before.person("alex").points)
        assertTrue(after.scheduled("noelle", "home", day).any { it.id == "dog" && after.isDone(it.id, day) })
        assertFalse(after.isDone("trash", day))
    }
    @Test fun repeatedCompletionCannotAwardPointsTwice() {
        val once = demo().complete("alex", "dog", day, day)
        assertEquals(once, once.complete("alex", "dog", day, day))
    }
    @Test fun cannotCompleteSomeoneElsesChoreOrAnotherDay() {
        rejected { demo().complete("sam", "dog", day, day) }
        rejected { demo().complete("alex", "dog", day + 1, day) }
        rejected { demo().complete("alex", "dog", day - 1, day) }
    }
    @Test fun FR05_streakContinuesOncePerDayAndAwardsFiveBonusPoints() {
        var state = demo().complete("alex", "dog", day, day)
        state = state.complete("alex", "trash", day, day)
        assertEquals(1, state.person("alex").streak); assertEquals(30, state.person("alex").points)
        state = state.complete("alex", "dog", day + 1, day + 1)
        assertEquals(2, state.person("alex").streak); assertEquals(45, state.person("alex").points)
        state = state.addChore("alex", "home", "Sweep", "alex", Frequency.DAILY, 10, day + 1)
        state = state.complete("alex", state.chores.last().id, day + 1, day + 1)
        assertEquals(2, state.person("alex").streak); assertEquals(55, state.person("alex").points)
    }
    @Test fun FR05_missedDayResetsStreakWithoutBonus() {
        val state = demo().complete("alex", "dog", day, day).complete("alex", "dog", day + 2, day + 2)
        assertEquals(1, state.person("alex").streak); assertEquals(20, state.person("alex").points)
    }
    @Test fun US04_dailyAndWeeklySchedulesRespectStartDate() {
        val state = demo()
        assertTrue(state.scheduled("alex", "home", day - 1).isEmpty())
        assertTrue(state.scheduled("alex", "home", day + 1).any { it.id == "dog" })
        assertFalse(state.scheduled("alex", "home", day + 1).any { it.id == "trash" })
        assertTrue(state.scheduled("alex", "home", day + 7).any { it.id == "trash" })
    }
    @Test fun recurringChoreCanBeCompletedOnNextOccurrence() {
        val state = demo().complete("alex", "dog", day, day)
        assertFalse(state.isDone("dog", day + 1))
        assertTrue(state.complete("alex", "dog", day + 1, day + 1).isDone("dog", day + 1))
    }
    @Test fun NFR03_noFrequencyOrAssigneeMeansNoCalendarAssignment() {
        val state = demo().addChore("alex", "home", "Someday", "alex", Frequency.NONE, 10, day)
            .addChore("alex", "home", "Unassigned", null, Frequency.DAILY, 10, day)
        assertEquals(5, state.visibleChores("alex", "home").size)
        assertEquals(3, state.scheduled("alex", "home", day).size)
        rejected { state.complete("alex", state.chores[state.chores.lastIndex - 1].id, day, day) }
    }
    @Test fun FR08_managerCreatesRewardWithPointCost() {
        val state = demo().addReward("alex", "home", "Ice cream", 40)
        assertEquals("Ice cream", state.rewards.last().title); assertEquals(40, state.rewards.last().cost)
    }
    @Test fun FR09_redemptionDeductsExactCostAndRecordsReward() {
        val state = demo().complete("alex", "trash", day, day).redeem("alex", "dessert")
        assertEquals(0, state.person("alex").points)
        assertEquals("Pick dessert", state.redemptions.single().title)
    }
    @Test fun NFR05_insufficientPointsCannotRedeemOrGoNegative() {
        val state = demo().complete("alex", "dog", day, day)
        rejected { state.redeem("alex", "dessert") }
        assertEquals(10, state.person("alex").points); assertTrue(state.redemptions.isEmpty())
    }
    @Test fun NFR01_unsharedRewardsCannotBeRedeemed() {
        var state = demo().createList("alex", "Private")
        state = state.addReward("alex", state.lists.last().id, "Secret", 1)
        rejected { state.redeem("noelle", state.rewards.last().id) }
    }
    @Test fun FR10_unconnectedScreenTimeDoesNotChargePointsOrPretendToUnlockDevice() {
        var state = demo()
        repeat(4) { state = state.complete("alex", "dog", day + it, day + it) }
        rejected { state.redeem("alex", "screen") }
        assertEquals(55, state.person("alex").points); assertTrue(state.redemptions.isEmpty())
    }
    @Test fun invalidNamesAmountsAndAssignmentsAreRejected() {
        rejected { demo().createList("alex", " ") }
        rejected { demo().addChore("alex", "home", " ", "alex", Frequency.DAILY, 10, day) }
        rejected { demo().addChore("alex", "home", "Chore", "alex", Frequency.DAILY, -1, day) }
        rejected { demo().addReward("alex", "home", "Prize", 0) }
        rejected { demo().addReward("alex", "home", "Prize", 10, -1) }
        val state = demo().createList("alex", "Private")
        rejected { state.addChore("alex", state.lists.last().id, "Chore", "sam", Frequency.DAILY, 10, day) }
    }
    @Test fun uncheckRemovesCompletionPointsAndStreak() {
        val original = demo()
        val undone = original.complete("alex", "dog", day, day).uncomplete("alex", "dog", day, day)
        assertEquals(original, undone)
        assertEquals(undone, undone.uncomplete("alex", "dog", day, day))
        assertEquals(10, undone.complete("alex", "dog", day, day).person("alex").points)
    }
    @Test fun uncheckTransfersBonusWhileAnotherChoreKeepsDayActive() {
        var state = demo().complete("alex", "dog", day, day)
        state = state.addChore("alex", "home", "Sweep", "alex", Frequency.DAILY, 20, day + 1)
        val sweep = state.chores.last().id
        state = state.complete("alex", "dog", day + 1, day + 1).complete("alex", sweep, day + 1, day + 1)
        state = state.uncomplete("alex", "dog", day + 1, day + 1)
        assertEquals(35, state.person("alex").points)
        assertEquals(2, state.person("alex").streak)
        assertEquals(25, state.completions.last().earned)
        state = state.complete("alex", "dog", day + 1, day + 1)
        assertEquals(45, state.person("alex").points)
        state = state.uncomplete("alex", sweep, day + 1, day + 1).uncomplete("alex", "dog", day + 1, day + 1)
        assertEquals(10, state.person("alex").points)
        assertEquals(1, state.person("alex").streak)
        assertEquals(day, state.person("alex").lastActive)
    }
    @Test fun uncheckDoesNotChangeOtherMembersOrOtherOccurrences() {
        val state = demo().complete("alex", "dog", day, day).complete("noelle", "dishes", day, day)
        rejected { state.uncomplete("noelle", "dog", day, day) }
        rejected { state.uncomplete("alex", "dog", day, day + 1) }
        val undone = state.uncomplete("alex", "dog", day, day)
        assertEquals(state.person("noelle"), undone.person("noelle"))
        assertTrue(undone.isDone("dishes", day))
    }
    @Test fun uncheckCannotEraseAlreadySpentPoints() {
        val state = demo().complete("alex", "trash", day, day).redeem("alex", "dessert")
        rejected { state.uncomplete("alex", "trash", day, day) }
        assertEquals(0, state.person("alex").points)
        assertTrue(state.isDone("trash", day))
    }
    @Test fun managerCanEditAllChoreFieldsWithoutChangingIdentity() {
        val before = demo()
        val after = before.editChore("alex", "dog", "Walk the dog", "sam", Frequency.WEEKLY, 30)
        val chore = after.chores.first { it.id == "dog" }
        assertEquals(before.chores.size, after.chores.size)
        assertEquals("Walk the dog", chore.title)
        assertEquals("sam", chore.assignee)
        assertEquals(30, chore.points)
        assertEquals(day, chore.startDay)
        assertEquals(Frequency.WEEKLY, chore.frequency)
        assertFalse(after.scheduled("sam", "home", day + 1).any { it.id == "dog" })
        assertTrue(after.scheduled("sam", "home", day + 7).any { it.id == "dog" })
    }
    @Test fun editsRequireManagerAndValidFields() {
        rejected { demo().editChore("sam", "dog", "Walk", "sam", Frequency.DAILY, 10) }
        rejected { demo().editChore("alex", "dog", " ", "alex", Frequency.DAILY, 10) }
        rejected { demo().editChore("alex", "dog", "Walk", "alex", Frequency.DAILY, 0) }
        rejected { demo().editChore("alex", "dog", "Walk", "missing", Frequency.DAILY, 10) }
    }
    @Test fun editedPointsDoNotChangePreviouslyEarnedPointsOrUndoAmount() {
        var state = demo().complete("alex", "dog", day, day)
        state = state.editChore("alex", "dog", "Walk", "sam", Frequency.NONE, 90)
        assertEquals(10, state.person("alex").points)
        assertTrue(state.scheduled("alex", "home", day).any { it.id == "dog" })
        rejected { state.uncomplete("sam", "dog", day, day) }
        state = state.uncomplete("alex", "dog", day, day)
        assertEquals(0, state.person("alex").points)
        assertFalse(state.scheduled("alex", "home", day).any { it.id == "dog" })
    }
    @Test fun editingCompletedChorePreservesBonusReversalIncludingOlderSavedData() {
        var state = demo().complete("alex", "dog", day, day).complete("alex", "dog", day + 1, day + 1)
        state = state.copy(completions = state.completions.map { it.copy(bonus = null) })
        state = state.editChore("alex", "dog", "Walk", "alex", Frequency.DAILY, 100)
        state = state.uncomplete("alex", "dog", day + 1, day + 1)
        assertEquals(10, state.person("alex").points)
        assertEquals(1, state.person("alex").streak)
    }
    @Test fun localStorageRoundTripPreservesSharingCompletionAndWallet() {
        var state = demo().createList("alex", "Kitchen")
        state = state.share("alex", state.lists.last().id, "sam").complete("alex", "trash", day, day).redeem("alex", "dessert")
        assertEquals(state, Gson().fromJson(Gson().toJson(state), ChoreState::class.java))
    }
}
