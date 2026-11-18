import Foundation

// Run with the swiftc command in README.md; kept outside the app target.
@main
struct DemoStoreChecks {
    @MainActor static func main() throws {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "America/Los_Angeles")!
        var clock = calendar.date(from: DateComponents(year: 2026, month: 3, day: 7, hour: 12))!
        let store = DemoStore(calendar: calendar, now: { clock })
        let trash = store.chores[0]
        let dog = store.chores[1]
        let dishes = store.chores[2]
        let home = store.activeList!.id
        assert(store.visibleChores(on: store.today, mine: true, all: false).count == 2)
        assert(store.visibleChores(on: store.today, mine: false, all: false).count == 3)
        try mustFail { try store.toggle(dishes, on: store.today) }
        try mustFail { try store.toggle(trash, on: calendar.date(byAdding: .day, value: 1, to: store.today)!) }
        try mustFail { try store.redeem(store.rewards[0]) }
        try store.toggle(trash, on: store.today)
        assert(store.points == 20 && store.streak == 1)
        try store.saveChore(existing: trash, title: "Take out recycling", points: 50, assigneeID: "noelle", frequency: .weekly, date: store.today)
        assert(store.points == 20, "Editing must preserve the original completion award")
        assert(store.visibleChores(on: store.today, mine: true, all: false).contains { $0.id == trash.id })
        try store.toggle(store.chores[0], on: store.today)
        assert(store.points == 0, "Original completer can undo even after reassignment")
        try store.toggle(dog, on: store.today)
        clock = calendar.date(byAdding: .day, value: 1, to: clock)!
        try store.toggle(dog, on: store.today)
        assert(store.points == 25 && store.streak == 2, "Consecutive days add one five-point bonus")
        try store.saveChore(existing: nil, title: "Water plants", points: 10, assigneeID: "alex", frequency: .daily, date: store.today)
        let plants = store.chores.last!
        try store.toggle(plants, on: store.today)
        assert(store.points == 35, "Only one bonus per day")
        try store.toggle(dog, on: store.today)
        assert(store.points == 25 && store.streak == 2, "Other completions preserve the day's bonus")
        try store.toggle(plants, on: store.today)
        assert(store.points == 10 && store.streak == 1)
        try store.toggle(dog, on: store.today)
        try store.toggle(plants, on: store.today)
        let previousBalance = store.points
        try mustFail { try store.redeem(store.rewards[2]) }
        assert(store.points == previousBalance && store.history.isEmpty)
        try store.redeem(store.rewards[0])
        assert(store.points == 5 && store.history.count == 1)
        try mustFail { try store.toggle(plants, on: store.today) }
        assert(store.points == 5 && store.completion(for: plants, on: store.today) != nil)
        clock = calendar.date(byAdding: .day, value: 2, to: clock)!
        assert(store.streak == 0)
        assert(trash.isScheduled(on: calendar.date(byAdding: .day, value: 7, to: trash.startDate)!, calendar: calendar), "Weekly recurrence crosses daylight saving correctly")
        assert(!trash.isScheduled(on: calendar.date(byAdding: .day, value: -7, to: trash.startDate)!, calendar: calendar))

        try store.createList(name: "  Kitchen  ")
        let kitchen = store.activeList!.id
        assert(store.activeList!.name == "Kitchen")
        assert(store.visibleChores(on: store.today, mine: false, all: true).isEmpty)
        try mustFail { try store.saveChore(existing: nil, title: "Wipe counter", points: 10, assigneeID: "noelle", frequency: .daily, date: store.today) }
        try store.share(with: "noelle")
        try mustFail { try store.share(with: "unknown") }
        try store.saveChore(existing: nil, title: "Plan groceries", points: 10, assigneeID: nil, frequency: .none, date: store.today)
        assert(store.visibleChores(on: store.today, mine: false, all: false).isEmpty)
        assert(store.visibleChores(on: store.today, mine: false, all: true).count == 1)
        try mustFail { try store.saveChore(existing: nil, title: " ", points: 10, assigneeID: nil, frequency: .daily, date: store.today) }
        try mustFail { try store.addReward(title: "Snack", cost: 0, minutes: 0) }
        try mustFail { try store.addReward(title: "Snack", cost: 10, minutes: -1) }
        try store.addReward(title: "Pick lunch", cost: 10, minutes: 0)
        assert(store.currentRewards.count == 1)
        try mustFail { try store.redeem(store.rewards[0]) }
        store.personID = "noelle"
        assert(store.activeList?.id == kitchen && !store.canManage)
        assert(store.visibleChores(on: store.today, mine: false, all: true).isEmpty, "Members cannot use manager-only all-chores mode")
        try mustFail { try store.createList(name: "Private") }
        try mustFail { try store.share(with: "sam") }
        try mustFail { try store.addReward(title: "Snack", cost: 10, minutes: 0) }
        store.personID = "sam"
        assert(store.activeList?.id == home && store.visibleLists.count == 1, "Unshared lists stay hidden after profile changes")
        print("DemoStore checks passed: scheduling, roles, sharing, edits, completions, streaks, validation, redemption, and point reversal.")
    }

    @MainActor private static func mustFail(_ action: () throws -> Void) throws {
        do {
            try action()
            fatalError("Expected a validation failure")
        } catch is DemoError { }
    }
}
