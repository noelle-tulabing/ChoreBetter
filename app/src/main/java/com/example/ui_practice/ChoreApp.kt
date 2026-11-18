package com.example.ui_practice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui_practice.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val SoftGray = Color(0xFFF0F0F0)
private fun dateLabel(day: Long, pattern: String): String = SimpleDateFormat(pattern, Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(day * 86_400_000L))

@Composable
fun ChoreApp(state: ChoreState, save: (ChoreState) -> Unit) {
    var user by rememberSaveable { mutableStateOf("alex") }
    var listId by rememberSaveable { mutableStateOf("home") }
    var day by rememberSaveable { mutableLongStateOf(today()) }
    var page by rememberSaveable { mutableStateOf("tasks") }
    var dialog by rememberSaveable { mutableStateOf("") }
    var mine by rememberSaveable { mutableStateOf(true) }
    var allChores by rememberSaveable { mutableStateOf(false) }
    var redeemId by rememberSaveable { mutableStateOf("") }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val person = state.person(user)
    val lists = state.visibleLists(user)
    val active = lists.firstOrNull { it.id == listId } ?: lists.firstOrNull()
    val manager = active?.let { state.canManage(user, it.id) } == true
    fun message(text: String) { scope.launch { snack.showSnackbar(text) } }
    fun change(action: () -> ChoreState): String? = try { save(action()); null } catch (e: IllegalArgumentException) { e.message ?: "Check your entries." } catch (e: IllegalStateException) { e.message ?: "Couldn't save changes." }
    BackHandler(page != "tasks") { page = "tasks" }
    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snack) },
        floatingActionButton = {
            if (page == "tasks" && (manager || active == null && person.manager)) FloatingActionButton(onClick = { dialog = if (active == null) "list" else "chore" }, shape = CircleShape, containerColor = Color.Black, contentColor = Color.White, modifier = Modifier.testTag("addChore")) {
                Text("+", fontSize = 32.sp, modifier = Modifier.semantics { contentDescription = if (active == null) "Create list" else "Add chore" })
            }
            if (page == "rewards" && manager) ExtendedFloatingActionButton(onClick = { dialog = "reward" }, containerColor = Color.Black, contentColor = Color.White) { Text("Add reward") }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().widthIn(max = 640.dp).padding(horizontal = 20.dp)) {
            if (page == "tasks") {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundIcon("Profile", "profile") { dialog = "profile" }
                    Spacer(Modifier.weight(1f))
                    Text("${person.points} points", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("balance"))
                    Spacer(Modifier.width(10.dp))
                    RoundIcon("Rewards", "shop") { page = "rewards" }
                }
                val weekStart = day - Math.floorMod(day + 4, 7)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { day -= 7 }, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(48.dp).semantics { contentDescription = "Previous week" }) { Text("‹", fontSize = 25.sp) }
                    Text(dateLabel(day, "MMMM yyyy"), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { day += 7 }, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(48.dp).semantics { contentDescription = "Next week" }) { Text("›", fontSize = 25.sp) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    (0..6).forEach { offset ->
                        val d = weekStart + offset
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(dateLabel(d, "EEE"), style = MaterialTheme.typography.labelSmall)
                            TextButton(onClick = { day = d; allChores = false }, shape = CircleShape, colors = ButtonDefaults.textButtonColors(containerColor = if (day == d) Color.Black else SoftGray, contentColor = if (day == d) Color.White else Color.Black), contentPadding = PaddingValues(0.dp), modifier = Modifier.padding(top = 6.dp).size(46.dp).semantics { contentDescription = dateLabel(d, "EEEE, MMMM d") }) { Text(dateLabel(d, "d")) }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { dialog = "lists" }, contentPadding = PaddingValues(horizontal = 0.dp)) { Text((active?.name ?: "Choose a list") + " ▾", fontWeight = FontWeight.Medium) }
                    Spacer(Modifier.weight(1f))
                    if (day != today()) OutlinedButton(onClick = { day = today(); allChores = false }, modifier = Modifier.testTag("todayButton")) { Text("Today") }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mine, onClick = { mine = true }, label = { Text("Mine") })
                    FilterChip(selected = !mine, onClick = { mine = false }, label = { Text("Everyone") })
                    if (manager) FilterChip(selected = allChores, onClick = { allChores = !allChores }, label = { Text("All chores") })
                }
                val chores = active?.let { if (allChores) state.visibleChores(user, it.id) else state.scheduled(user, it.id, day) }.orEmpty().filter { !mine || it.assignee == user }
                LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("choreList"), contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    if (chores.isEmpty()) item {
                        Column(Modifier.padding(vertical = 24.dp)) {
                            Text(if (active == null) "No shared lists yet" else "No chores here", style = MaterialTheme.typography.titleMedium)
                            Text(if (active == null) "Ask your manager to share a list." else if (manager) "Add a chore, or choose another day." else "Try another day.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    items(chores, key = { it.id }) { chore ->
                        val done = state.isDone(chore.id, day)
                        val scheduled = chore.frequency != Frequency.NONE && chore.assignee != null && day >= chore.startDay && (day - chore.startDay) % chore.frequency.days == 0L
                        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                            IconButton(onClick = {
                                if (chore.assignee != user) {
                                    message("Only ${chore.assignee?.let { state.person(it).name } ?: "the assigned member"} can complete this chore")
                                    return@IconButton
                                }
                                if (day != today()) {
                                    message("Choose Today to complete a chore")
                                    return@IconButton
                                }
                                if (!scheduled) {
                                    message("This chore is not scheduled today")
                                    return@IconButton
                                }
                                var earned = 0
                                val error = change {
                                    val next = state.complete(user, chore.id, day, today())
                                    earned = next.person(user).points - person.points
                                    next
                                }
                                message(error ?: "Completed. +$earned points")
                            }, enabled = !done, modifier = Modifier.size(48.dp).testTag("complete-${chore.id}").semantics { contentDescription = if (done) "${chore.title}, completed" else "Complete ${chore.title}" }) {
                                Canvas(Modifier.size(25.dp)) {
                                    drawCircle(if (done) Color.Black else Color(0xFFBEBEBE), style = if (done) androidx.compose.ui.graphics.drawscope.Fill else Stroke(3.dp.toPx()))
                                    if (done) { val p = Path(); p.moveTo(size.width * .26f, size.height * .5f); p.lineTo(size.width * .44f, size.height * .68f); p.lineTo(size.width * .75f, size.height * .33f); drawPath(p, Color.White, style = Stroke(2.dp.toPx())) }
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(chore.title, fontSize = 18.sp, modifier = Modifier.background(if (done) Color(0xFFE9E9E9) else Color(0xFFDDDDDD), RoundedCornerShape(28.dp)).padding(horizontal = 16.dp, vertical = 10.dp))
                                val who = chore.assignee?.let { state.person(it).name } ?: "Unassigned"
                                Text("$who · ${chore.frequency.label} · ${chore.points} pts" + if (done) " · Done" else "", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp, top = 6.dp))
                            }
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { page = "tasks" }) { Text("‹ Back") }
                    Spacer(Modifier.weight(1f))
                    Text("${person.points} points", modifier = Modifier.testTag("balance"))
                }
                Text("Rewards", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp))
                Text(active?.name ?: "Choose a list to see rewards", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val rewards = state.rewards.filter { it.listId == active?.id }
                    if (rewards.isEmpty()) item { Text("No rewards yet") }
                    items(rewards, key = { it.id }) { reward ->
                        Column(Modifier.fillMaxWidth().background(SoftGray, RoundedCornerShape(16.dp)).padding(18.dp)) {
                            Text(reward.title, style = MaterialTheme.typography.titleMedium)
                            Text("${reward.cost} points", modifier = Modifier.padding(top = 6.dp))
                            if (reward.screenMinutes > 0) Text("Screen time not connected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                            Button(onClick = { redeemId = reward.id }, enabled = person.points >= reward.cost && reward.screenMinutes == 0, modifier = Modifier.align(Alignment.End).testTag("redeem-${reward.id}")) { Text(if (reward.screenMinutes > 0) "Unavailable" else if (person.points < reward.cost) "Need ${reward.cost - person.points} more" else "Redeem") }
                        }
                    }
                    val history = state.redemptions.filter { it.userId == user }
                    if (history.isNotEmpty()) {
                        item { Text("Your redeemed rewards", style = MaterialTheme.typography.titleMedium) }
                        items(history, key = { it.id }) { Text("${it.title}  •  ${it.cost} points") }
                    }
                }
            }
        }
    }
    if (redeemId.isNotEmpty()) {
        val reward = state.rewards.first { it.id == redeemId }
        AlertDialog(onDismissRequest = { redeemId = "" }, title = { Text("Redeem ${reward.title}?") }, text = { Text("Spend ${reward.cost} points? Your remaining balance will be ${person.points - reward.cost} points.") }, confirmButton = { TextButton(onClick = { val error = change { state.redeem(user, reward.id) }; redeemId = ""; message(error ?: "Reward redeemed") }, modifier = Modifier.testTag("confirmRedeem")) { Text("Redeem") } }, dismissButton = { TextButton(onClick = { redeemId = "" }) { Text("Cancel") } })
    }
    when (dialog) {
        "profile" -> AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("Household") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Switch profile", style = MaterialTheme.typography.bodySmall)
                state.people.forEach { member ->
                    Row(Modifier.fillMaxWidth().clickable { user = member.id; allChores = false; dialog = "" }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = user == member.id, onClick = { user = member.id; allChores = false; dialog = "" })
                        Column { Text(member.name); Text(if (member.manager) "Manager" else "Member", style = MaterialTheme.typography.bodySmall) }
                    }
                }
                val streak = if ((person.lastActive ?: Long.MIN_VALUE) >= today() - 1) person.streak else 0
                Text("${person.name}: $streak-day streak", modifier = Modifier.padding(top = 12.dp))
                if (manager) TextButton(onClick = { dialog = "share" }) { Text("Share current list") }
                if (person.manager) TextButton(onClick = { dialog = "list" }) { Text("Create list") }
            }
        }, confirmButton = { TextButton(onClick = { dialog = "" }) { Text("Done") } })
        "lists" -> AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("Your lists") }, text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            lists.forEach { list -> TextButton(onClick = { listId = list.id; dialog = ""; allChores = false }) { Text(list.name + if (list.id == active?.id) " ✓" else "") } }
            if (lists.isEmpty()) Text("No shared lists")
            if (person.manager) TextButton(onClick = { dialog = "list" }) { Text("+ Create list") }
            if (manager) TextButton(onClick = { dialog = "share" }) { Text("Share ${active?.name}") }
        } }, confirmButton = { TextButton(onClick = { dialog = "" }) { Text("Done") } })
        "list" -> EntryDialog("Create list", "Create", onDismiss = { dialog = "" }) { name, _, _, _, _ ->
            val next = runCatching { state.createList(user, name) }
            val error = next.exceptionOrNull()?.message ?: change { next.getOrThrow() }
            if (error == null) { listId = next.getOrThrow().lists.last().id; dialog = "" }
            error
        }
        "chore" -> if (active != null) EntryDialog("Add chore", "Add chore", chore = true, members = state.people.filter { state.canView(it.id, active.id) }, onDismiss = { dialog = "" }) { name, points, assignee, freq, _ ->
            val error = change { state.addChore(user, active.id, name, assignee, freq, points, day) }
            if (error == null) { dialog = ""; if (assignee != user) mine = false; if (freq == Frequency.NONE || assignee == null) allChores = true; message("Chore added") }; error
        }
        "reward" -> if (active != null) EntryDialog("Add reward", "Add reward", reward = true, onDismiss = { dialog = "" }) { name, points, _, _, minutes ->
            val error = change { state.addReward(user, active.id, name, points, minutes) }; if (error == null) dialog = ""; error
        }
        "share" -> if (active != null) AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("Share ${active.name}") }, text = { Column {
            state.people.forEach { member ->
                val shared = state.canView(member.id, active.id)
                TextButton(onClick = { val error = change { state.share(user, active.id, member.id) }; if (error != null) message(error) }, enabled = !shared) { Text(member.name + if (shared) " — has access" else " — share") }
            }
        } }, confirmButton = { TextButton(onClick = { dialog = "" }) { Text("Done") } })
    }
}

@Composable
private fun RoundIcon(label: String, kind: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp).background(SoftGray, CircleShape).semantics { contentDescription = label }) {
        Canvas(Modifier.size(24.dp)) {
            val s = size.width; val stroke = Stroke(1.6.dp.toPx())
            if (kind == "profile") {
                drawCircle(Color.Black, s * .14f, Offset(s * .5f, s * .29f), style = stroke)
                drawRoundRect(Color.Black, Offset(s * .22f, s * .52f), androidx.compose.ui.geometry.Size(s * .56f, s * .34f), androidx.compose.ui.geometry.CornerRadius(s * .15f), style = stroke)
            } else {
                val p = Path(); p.moveTo(s * .15f, s * .42f); p.lineTo(s * .25f, s * .18f); p.lineTo(s * .75f, s * .18f); p.lineTo(s * .85f, s * .42f); p.close(); drawPath(p, Color.Black, style = stroke)
                drawRect(Color.Black, Offset(s * .23f, s * .43f), androidx.compose.ui.geometry.Size(s * .54f, s * .4f), style = stroke)
                drawRect(Color.Black, Offset(s * .43f, s * .6f), androidx.compose.ui.geometry.Size(s * .17f, s * .23f), style = stroke)
            }
        }
    }
}

@Composable
private fun EntryDialog(title: String, action: String, chore: Boolean = false, reward: Boolean = false, members: List<Person> = emptyList(), onDismiss: () -> Unit, onSave: (String, Int, String?, Frequency, Int) -> String?) {
    var name by rememberSaveable { mutableStateOf("") }
    var points by rememberSaveable { mutableStateOf(if (reward) "20" else "10") }
    var assignee by rememberSaveable { mutableStateOf(members.firstOrNull()?.id) }
    var frequency by rememberSaveable { mutableStateOf(Frequency.DAILY) }
    var minutes by rememberSaveable { mutableStateOf("0") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var memberMenu by remember { mutableStateOf(false) }
    var frequencyMenu by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("entryName"))
            if (chore || reward) OutlinedTextField(value = points, onValueChange = { points = it }, label = { Text(if (reward) "Point cost" else "Points") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth().testTag("entryPoints"))
            if (chore) {
                Box {
                    OutlinedButton(onClick = { memberMenu = true }, modifier = Modifier.fillMaxWidth()) { Text("Assign to: ${members.firstOrNull { it.id == assignee }?.name ?: "Unassigned"}") }
                    DropdownMenu(expanded = memberMenu, onDismissRequest = { memberMenu = false }) {
                        members.forEach { member -> DropdownMenuItem(text = { Text(member.name) }, onClick = { assignee = member.id; memberMenu = false }) }
                        DropdownMenuItem(text = { Text("Unassigned") }, onClick = { assignee = null; memberMenu = false })
                    }
                }
                Box {
                    OutlinedButton(onClick = { frequencyMenu = true }, modifier = Modifier.fillMaxWidth()) { Text("Repeat: ${frequency.label}") }
                    DropdownMenu(expanded = frequencyMenu, onDismissRequest = { frequencyMenu = false }) { Frequency.entries.forEach { f -> DropdownMenuItem(text = { Text(f.label) }, onClick = { frequency = f; frequencyMenu = false }) } }
                }
            }
            if (reward) {
                OutlinedTextField(value = minutes, onValueChange = { minutes = it }, label = { Text("Screen-time minutes (0 for other rewards)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("formError")) }
        }
    }, confirmButton = { TextButton(onClick = {
        val p = points.toIntOrNull(); val m = minutes.toIntOrNull()
        error = if ((chore || reward) && p == null) "Enter a whole number for points." else if (reward && m == null) "Enter a whole number for minutes." else onSave(name, p ?: 10, assignee, frequency, m ?: 0)
    }, modifier = Modifier.testTag("saveEntry")) { Text(action) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
