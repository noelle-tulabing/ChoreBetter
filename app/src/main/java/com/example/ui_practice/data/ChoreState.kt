package com.example.ui_practice.data

import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

fun today(): Long {
    val local = Calendar.getInstance()
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    utc.clear()
    utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    return utc.timeInMillis / 86_400_000L
}

enum class Frequency(val label: String, val days: Int) { NONE("Not scheduled", 0), DAILY("Daily", 1), WEEKLY("Weekly", 7) }
data class Person(val id: String, val name: String, val manager: Boolean = false, val points: Int = 0, val streak: Int = 0, val lastActive: Long? = null)
data class ChoreList(val id: String, val name: String, val owner: String, val members: Set<String>)
data class Chore(val id: String, val listId: String, val title: String, val assignee: String?, val frequency: Frequency, val points: Int, val startDay: Long)
data class Completion(val choreId: String, val day: Long, val userId: String, val earned: Int, val bonus: Int? = null)
data class Reward(val id: String, val listId: String, val title: String, val cost: Int, val screenMinutes: Int = 0)
data class Redemption(val id: String, val rewardId: String, val userId: String, val title: String, val cost: Int, val screenMinutes: Int)
data class ChoreState(
    val people: List<Person>, val lists: List<ChoreList> = emptyList(), val chores: List<Chore> = emptyList(),
    val completions: List<Completion> = emptyList(), val rewards: List<Reward> = emptyList(), val redemptions: List<Redemption> = emptyList()
) {
    fun person(id: String) = people.first { it.id == id }
    fun visibleLists(user: String) = lists.filter { user == it.owner || user in it.members }
    fun canView(user: String, listId: String) = visibleLists(user).any { it.id == listId }
    fun canManage(user: String, listId: String) = person(user).manager && lists.any { it.id == listId && it.owner == user }
    fun visibleChores(user: String, listId: String): List<Chore> {
        require(canView(user, listId)) { "This list isn't shared with you." }
        return chores.filter { it.listId == listId }
    }
    fun scheduled(user: String, listId: String, day: Long) = visibleChores(user, listId).filter {
        isDone(it.id, day) || (it.frequency != Frequency.NONE && it.assignee != null && day >= it.startDay && (day - it.startDay) % it.frequency.days == 0L)
    }
    fun isDone(choreId: String, day: Long) = completions.any { it.choreId == choreId && it.day == day }
    fun createList(user: String, name: String): ChoreState {
        require(person(user).manager) { "Only managers can create lists." }
        require(name.trim().length in 1..80) { "Enter a list name (1–80 characters)." }
        return copy(lists = lists + ChoreList(UUID.randomUUID().toString(), name.trim(), user, setOf(user)))
    }
    fun share(user: String, listId: String, member: String): ChoreState {
        require(canManage(user, listId)) { "Only the list manager can share it." }
        require(people.any { it.id == member }) { "That member doesn't exist." }
        return copy(lists = lists.map { if (it.id == listId) it.copy(members = it.members + member) else it })
    }
    private fun validateChore(user: String, listId: String, title: String, assignee: String?, points: Int) {
        require(canManage(user, listId)) { "Only the list manager can add chores." }
        require(title.trim().length in 1..80) { "Enter a chore name (1–80 characters)." }
        require(points in 1..10000) { "Points must be between 1 and 10,000." }
        require(assignee == null || (people.any { it.id == assignee } && canView(assignee, listId))) { "Share the list with this member first." }
    }
    fun addChore(user: String, listId: String, title: String, assignee: String?, frequency: Frequency, points: Int, day: Long): ChoreState {
        validateChore(user, listId, title, assignee, points)
        return copy(chores = chores + Chore(UUID.randomUUID().toString(), listId, title.trim(), assignee, frequency, points, day))
    }
    fun editChore(user: String, choreId: String, title: String, assignee: String?, frequency: Frequency, points: Int): ChoreState {
        val chore = chores.firstOrNull { it.id == choreId } ?: error("Chore not found.")
        validateChore(user, chore.listId, title, assignee, points)
        return copy(chores = chores.map {
            if (it.id == choreId) it.copy(title = title.trim(), assignee = assignee, frequency = frequency, points = points) else it
        })
    }
    fun complete(user: String, choreId: String, day: Long, currentDay: Long): ChoreState {
        val chore = chores.firstOrNull { it.id == choreId } ?: error("Chore not found.")
        require(canView(user, chore.listId) && chore.assignee == user) { "You can only complete your own chores." }
        require(day == currentDay) { "Complete chores on their scheduled day." }
        require(scheduled(user, chore.listId, day).any { it.id == choreId }) { "This chore isn't scheduled today." }
        if (isDone(choreId, day)) return this
        val p = person(user)
        val continued = p.lastActive == day - 1
        val streak = when { p.lastActive == day -> p.streak; continued -> p.streak + 1; else -> 1 }
        val earned = chore.points + if (continued) 5 else 0
        return copy(
            people = people.map { if (it.id == user) it.copy(points = Math.addExact(it.points, earned), streak = streak, lastActive = day) else it },
            completions = completions + Completion(choreId, day, user, earned, if (continued) 5 else 0)
        )
    }
    private fun completionBonus(completion: Completion): Int = completion.bonus ?: run {
        // Older saved completions predate the explicit bonus field.
        if (completions.firstOrNull { it.userId == completion.userId && it.day == completion.day } == completion &&
            completions.any { it.userId == completion.userId && it.day == completion.day - 1 }) 5 else 0
    }
    fun uncomplete(user: String, choreId: String, day: Long, currentDay: Long): ChoreState {
        val chore = chores.firstOrNull { it.id == choreId } ?: error("Chore not found.")
        require(canView(user, chore.listId)) { "This list is not shared with you." }
        require(completions.none { it.choreId == choreId && it.day == day && it.userId != user }) { "You can only uncheck your own chores." }
        require(day == currentDay) { "Only today's chores can be unchecked." }
        val removed = completions.firstOrNull { it.choreId == choreId && it.day == day && it.userId == user } ?: return this
        var remaining = completions - removed
        // Keep the day's bonus on another completion if the day is still active.
        val recipient = remaining.firstOrNull { it.userId == user && it.day == day }
        val bonus = completionBonus(removed)
        val transferred = if (recipient != null) bonus else 0
        if (recipient != null && transferred > 0) {
            remaining = remaining.map { if (it == recipient) it.copy(earned = it.earned + transferred, bonus = completionBonus(it) + transferred) else it }
        }
        val deduction = removed.earned - transferred
        require(person(user).points >= deduction) { "Can't uncheck: these points have already been spent." }
        val activeDays = remaining.filter { it.userId == user }.map { it.day }.toSet()
        val lastActive = activeDays.maxOrNull()
        var streak = 0
        if (lastActive != null) while (lastActive - streak in activeDays) streak++
        return copy(completions = remaining, people = people.map {
            if (it.id == user) it.copy(points = it.points - deduction, streak = streak, lastActive = lastActive) else it
        })
    }
    fun addReward(user: String, listId: String, title: String, cost: Int, minutes: Int = 0): ChoreState {
        require(canManage(user, listId)) { "Only the list manager can add rewards." }
        require(title.trim().length in 1..80) { "Enter a reward name (1–80 characters)." }
        require(cost in 1..10000) { "Cost must be between 1 and 10,000 points." }
        require(minutes in 0..1440) { "Screen time must be between 0 and 1,440 minutes." }
        return copy(rewards = rewards + Reward(UUID.randomUUID().toString(), listId, title.trim(), cost, minutes))
    }
    fun redeem(user: String, rewardId: String): ChoreState {
        val reward = rewards.firstOrNull { it.id == rewardId } ?: error("Reward not found.")
        require(canView(user, reward.listId)) { "This reward isn't shared with you." }
        require(reward.screenMinutes == 0) { "Screen-time rewards need a connected service. No points were spent." }
        require(person(user).points >= reward.cost) { "You don't have enough points yet." }
        return copy(people = people.map { if (it.id == user) it.copy(points = it.points - reward.cost) else it },
            redemptions = redemptions + Redemption(UUID.randomUUID().toString(), reward.id, user, reward.title, reward.cost, reward.screenMinutes))
    }
    companion object {
        fun demo(day: Long = today()) = ChoreState(
            people = listOf(Person("alex", "Alex", true), Person("noelle", "Noelle"), Person("sam", "Sam"), Person("riley", "Riley")),
            lists = listOf(ChoreList("home", "Household chores", "alex", setOf("alex", "noelle", "sam", "riley"))),
            chores = listOf(Chore("trash", "home", "Take out the trash", "alex", Frequency.WEEKLY, 20, day), Chore("dog", "home", "Feed the dog", "alex", Frequency.DAILY, 10, day), Chore("dishes", "home", "Do the dishes", "noelle", Frequency.DAILY, 15, day)),
            rewards = listOf(Reward("movie", "home", "Choose movie night", 30), Reward("dessert", "home", "Pick dessert", 20), Reward("screen", "home", "30 minutes of screen time", 40, 30))
        )
    }
}
