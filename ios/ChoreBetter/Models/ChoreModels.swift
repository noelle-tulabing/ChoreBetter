import Foundation

// UI-facing values; Noelle will map Tyler's backend records to these types.
struct Person: Identifiable {
    let id: String
    let name: String
    let isManager: Bool
}

struct ChoreList: Identifiable {
    let id: UUID
    var name: String
    let ownerID: String
    var memberIDs: Set<String>
}

enum Frequency: String, CaseIterable, Identifiable {
    case none = "Not scheduled", daily = "Daily", weekly = "Weekly"
    var id: Self { self }
}

struct Chore: Identifiable {
    let id: UUID
    let listID: UUID
    var title: String
    var assigneeID: String?
    var frequency: Frequency
    var points: Int
    let startDate: Date

    func isScheduled(on date: Date, calendar: Calendar = .current) -> Bool {
        guard assigneeID != nil, frequency != .none,
              let days = calendar.dateComponents([.day], from: calendar.startOfDay(for: startDate),
                                                 to: calendar.startOfDay(for: date)).day,
              days >= 0 else { return false }
        return frequency == .daily || days % 7 == 0
    }
}

struct Completion {
    let choreID: UUID
    let date: Date
    let personID: String
    let points: Int
}

struct Reward: Identifiable {
    let id: UUID
    let listID: UUID
    let title: String
    let cost: Int
    let screenMinutes: Int
}

struct Redemption: Identifiable {
    let id = UUID()
    let personID: String
    let title: String
    let cost: Int
}
