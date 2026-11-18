import SwiftUI

struct ChoreEditor: View {
    let chore: Chore?
    let members: [Person]
    let date: Date
    let save: (String, Int, String?, Frequency) throws -> Void
    @State private var title: String
    @State private var points: String
    @State private var assigneeID: String
    @State private var frequency: Frequency
    @State private var error: String?

    init(chore: Chore?, members: [Person], date: Date,
         save: @escaping (String, Int, String?, Frequency) throws -> Void) {
        self.chore = chore
        self.members = members
        self.date = date
        self.save = save
        _title = State(initialValue: chore?.title ?? "")
        _points = State(initialValue: String(chore?.points ?? 10))
        _assigneeID = State(initialValue: chore.map { $0.assigneeID ?? "" } ?? members.first?.id ?? "")
        _frequency = State(initialValue: chore?.frequency ?? .daily)
    }

    var body: some View {
        Form {
            Section {
                TextField("Name", text: $title).accessibilityIdentifier("entryName")
                TextField("Points", text: $points).keyboardType(.numberPad).accessibilityIdentifier("entryPoints")
                Picker("Assign to", selection: $assigneeID) {
                    Text("Unassigned").tag("")
                    ForEach(members) { Text($0.name).tag($0.id) }
                }
                Picker("Repeat", selection: $frequency) {
                    ForEach(Frequency.allCases) { Text($0.rawValue).tag($0) }
                }
            } footer: {
                Text("Starts \((chore?.startDate ?? date).formatted(date: .abbreviated, time: .omitted)). Completed chores keep their original points.")
            }
            if let error { Section { Text(error).foregroundStyle(.red).accessibilityIdentifier("formError") } }
        }
        .navigationTitle(chore == nil ? "Add chore" : "Edit chore")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button(chore == nil ? "Add" : "Save") {
                    guard let value = Int(points) else { error = "Enter a whole number for points."; return }
                    do { try save(title, value, assigneeID.isEmpty ? nil : assigneeID, frequency) }
                    catch { self.error = error.localizedDescription }
                }
                .fontWeight(.semibold)
                .accessibilityIdentifier("saveEntry")
            }
        }
    }
}

struct ListEditor: View {
    let save: (String) throws -> Void
    @State private var name = ""
    @State private var error: String?

    var body: some View {
        Form {
            Section {
                TextField("List name", text: $name)
            } footer: { Text("New lists are private until you share them with a household member.") }
            if let error { Text(error).foregroundStyle(.red) }
        }
        .navigationTitle("Create list")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Create") {
                    do { try save(name) } catch { self.error = error.localizedDescription }
                }
                .fontWeight(.semibold)
            }
        }
    }
}

struct RewardEditor: View {
    let save: (String, Int, Int) throws -> Void
    @State private var title = ""
    @State private var cost = "20"
    @State private var minutes = "0"
    @State private var error: String?

    var body: some View {
        Form {
            Section {
                TextField("Name", text: $title)
                TextField("Point cost", text: $cost).keyboardType(.numberPad)
            }
            Section {
                TextField("Screen-time minutes", text: $minutes).keyboardType(.numberPad)
            } footer: {
                Text("Use 0 for other rewards. Screen-time rewards stay unavailable until a screen-time service is connected.")
            }
            if let error { Text(error).foregroundStyle(.red) }
        }
        .navigationTitle("Add reward")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Add") {
                    guard let cost = Int(cost), let minutes = Int(minutes) else {
                        error = "Enter whole numbers for cost and minutes."; return
                    }
                    do { try save(title, cost, minutes) } catch { self.error = error.localizedDescription }
                }
                .fontWeight(.semibold)
            }
        }
    }
}
