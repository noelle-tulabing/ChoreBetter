import Foundation
import Observation

struct DemoError: LocalizedError {
    let message: String
    var errorDescription: String? { message }
}

// In-memory UI demo only. Replace this state/actions with the backend integration.
@MainActor @Observable
final class DemoStore {
    let people = [Person(id: "alex", name: "Alex", isManager: true),
                  Person(id: "noelle", name: "Noelle", isManager: false),
                  Person(id: "sam", name: "Sam", isManager: false),
                  Person(id: "riley", name: "Riley", isManager: false)]
    private(set) var lists: [ChoreList]
    private(set) var chores: [Chore]
    private(set) var rewards: [Reward]
    private(set) var completions: [Completion] = []
    private(set) var redemptions: [Redemption] = []
    var personID = "alex"
    var selectedListID: UUID
    let calendar: Calendar
    private let now: () -> Date

    init(calendar: Calendar = .current, now: @escaping () -> Date = { Date() }) {
        self.calendar = calendar
        self.now = now
        let homeID = UUID()
        selectedListID = homeID
        lists = [ChoreList(id: homeID, name: "Household chores", ownerID: "alex",
                           memberIDs: ["alex", "noelle", "sam", "riley"])]
        let today = calendar.startOfDay(for: now())
        chores = [Chore(id: UUID(), listID: homeID, title: "Take out the trash", assigneeID: "alex", frequency: .weekly, points: 20, startDate: today),
                  Chore(id: UUID(), listID: homeID, title: "Feed the dog", assigneeID: "alex", frequency: .daily, points: 10, startDate: today),
                  Chore(id: UUID(), listID: homeID, title: "Do the dishes", assigneeID: "noelle", frequency: .daily, points: 15, startDate: today)]
        rewards = [Reward(id: UUID(), listID: homeID, title: "Choose movie night", cost: 30, screenMinutes: 0),
                   Reward(id: UUID(), listID: homeID, title: "Pick dessert", cost: 20, screenMinutes: 0),
                   Reward(id: UUID(), listID: homeID, title: "30 minutes of screen time", cost: 40, screenMinutes: 30)]
    }

    var today: Date { calendar.startOfDay(for: now()) }
    var person: Person { people.first { $0.id == personID }! }
    var visibleLists: [ChoreList] { lists.filter { $0.memberIDs.contains(personID) } }
    var activeList: ChoreList? { visibleLists.first { $0.id == selectedListID } ?? visibleLists.first }
    var canManage: Bool { activeList.map { canManage($0.id) } ?? false }
    var members: [Person] { people.filter { activeList?.memberIDs.contains($0.id) == true } }
    var currentRewards: [Reward] { rewards.filter { $0.listID == activeList?.id } }
    var history: [Redemption] { redemptions.filter { $0.personID == personID }.reversed() }
    var points: Int { balance(for: personID, completions: completions) }

    func name(for id: String?) -> String { people.first { $0.id == id }?.name ?? "Unassigned" }
    func canManage(_ listID: UUID) -> Bool {
        person.isManager && lists.contains { $0.id == listID && $0.ownerID == personID }
    }
    func completion(for chore: Chore, on date: Date) -> Completion? {
        completions.first { $0.choreID == chore.id && calendar.isDate($0.date, inSameDayAs: date) }
    }
    func visibleChores(on date: Date, mine: Bool, all: Bool) -> [Chore] {
        chores.filter { chore in
            let done = completion(for: chore, on: date)
            return chore.listID == activeList?.id
                && ((all && canManage) || done != nil || chore.isScheduled(on: date, calendar: calendar))
                && (!mine || (done?.personID ?? chore.assigneeID) == personID)
        }
    }

    private func activeDays(for id: String, in records: [Completion]) -> Set<Date> {
        Set(records.filter { $0.personID == id }.map { $0.date })
    }
    private func balance(for id: String, completions records: [Completion]) -> Int {
        let days = activeDays(for: id, in: records)
        let bonusDays = days.filter { days.contains(calendar.date(byAdding: .day, value: -1, to: $0)!) }
        return records.filter { $0.personID == id }.reduce(0) { $0 + $1.points }
            + bonusDays.count * 5 - redemptions.filter { $0.personID == id }.reduce(0) { $0 + $1.cost }
    }
    var streak: Int {
        let days = activeDays(for: personID, in: completions)
        var date = days.contains(today) ? today : calendar.date(byAdding: .day, value: -1, to: today)!
        var count = 0
        while days.contains(date) {
            count += 1
            date = calendar.date(byAdding: .day, value: -1, to: date)!
        }
        return count
    }

    func toggle(_ chore: Chore, on date: Date) throws {
        try require(activeList?.id == chore.listID, "This list isn't shared with you.")
        let done = completion(for: chore, on: date)
        try require((done?.personID ?? chore.assigneeID) == personID, "You can only complete your own chores.")
        try require(calendar.isDate(date, inSameDayAs: today), "Choose Today to complete or uncheck a chore.")
        if done != nil {
            let remaining = completions.filter { !($0.choreID == chore.id && $0.date == today) }
            try require(balance(for: personID, completions: remaining) >= 0,
                        "Can't uncheck: these points have already been spent.")
            completions = remaining
        } else {
            try require(chore.isScheduled(on: date, calendar: calendar), "This chore isn't scheduled today.")
            completions.append(Completion(choreID: chore.id, date: today, personID: personID, points: chore.points))
        }
    }

    func createList(name: String) throws {
        try require(person.isManager, "Only managers can create lists.")
        let name = try validatedName(name)
        let list = ChoreList(id: UUID(), name: name, ownerID: personID, memberIDs: [personID])
        lists.append(list)
        selectedListID = list.id
    }
    func share(with memberID: String) throws {
        guard let list = activeList else { throw DemoError(message: "Choose a list first.") }
        try require(canManage, "Only the list manager can share it.")
        try require(people.contains { $0.id == memberID }, "That member doesn't exist.")
        guard let index = lists.firstIndex(where: { $0.id == list.id }) else { return }
        lists[index].memberIDs.insert(memberID)
    }
    func saveChore(existing: Chore?, title: String, points: Int, assigneeID: String?, frequency: Frequency, date: Date) throws {
        guard let list = activeList else { throw DemoError(message: "Choose a list first.") }
        try require(canManage, "Only the list manager can change chores.")
        let title = try validatedName(title)
        try require((1...10_000).contains(points), "Points must be between 1 and 10,000.")
        try require(assigneeID == nil || members.contains { $0.id == assigneeID }, "Share the list with this member first.")
        if let existing {
            try require(existing.listID == list.id, "Choose this chore's list first.")
            guard let index = chores.firstIndex(where: { $0.id == existing.id }) else {
                throw DemoError(message: "Chore not found.")
            }
            chores[index].title = title
            chores[index].points = points
            chores[index].assigneeID = assigneeID
            chores[index].frequency = frequency
        } else {
            chores.append(Chore(id: UUID(), listID: list.id, title: title, assigneeID: assigneeID,
                                frequency: frequency, points: points, startDate: calendar.startOfDay(for: date)))
        }
    }
    func addReward(title: String, cost: Int, minutes: Int) throws {
        guard let list = activeList else { throw DemoError(message: "Choose a list first.") }
        try require(canManage, "Only the list manager can add rewards.")
        let title = try validatedName(title)
        try require((1...10_000).contains(cost), "Cost must be between 1 and 10,000 points.")
        try require((0...1_440).contains(minutes), "Screen time must be between 0 and 1,440 minutes.")
        rewards.append(Reward(id: UUID(), listID: list.id, title: title, cost: cost, screenMinutes: minutes))
    }
    func redeem(_ reward: Reward) throws {
        try require(currentRewards.contains { $0.id == reward.id }, "This reward isn't shared with you.")
        try require(reward.screenMinutes == 0, "Screen-time rewards aren't connected. No points were spent.")
        try require(points >= reward.cost, "You don't have enough points yet.")
        redemptions.append(Redemption(personID: personID, title: reward.title, cost: reward.cost))
    }
    private func validatedName(_ name: String) throws -> String {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        try require((1...80).contains(trimmed.count), "Enter a name (1–80 characters).")
        return trimmed
    }
    private func require(_ condition: Bool, _ message: String) throws {
        if !condition { throw DemoError(message: message) }
    }
}
