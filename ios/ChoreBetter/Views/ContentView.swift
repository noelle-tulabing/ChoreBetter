import SwiftUI

struct ContentView: View {
    @State private var store = DemoStore()
    @State private var date = Calendar.current.startOfDay(for: Date())
    @State private var mine = true
    @State private var allChores = false
    @State private var sheet: ChoreSheet?
    @State private var error: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    header
                    WeekPicker(date: $date) { allChores = false }
                    listControls
                    choreList
                }
                .padding(20)
                .padding(.bottom, 88)
                .frame(maxWidth: 640)
                .frame(maxWidth: .infinity)
            }
            .background(Color(.systemBackground))
            .toolbar(.hidden, for: .navigationBar)
            .overlay(alignment: .bottomTrailing) {
                if store.canManage || (store.activeList == nil && store.person.isManager) {
                    Button {
                        sheet = store.activeList == nil ? .newList : .chore(nil)
                    } label: {
                        Image(systemName: "plus")
                            .font(.title)
                            .frame(width: 60, height: 60)
                            .foregroundStyle(Color(.systemBackground))
                            .background(Color.primary, in: Circle())
                    }
                    .accessibilityLabel(store.activeList == nil ? "Create list" : "Add chore")
                    .accessibilityIdentifier("addChore")
                    .padding(24)
                }
            }
            .sheet(isPresented: Binding(get: { sheet != nil }, set: { if !$0 { sheet = nil } })) {
                NavigationStack {
                    Group {
                        if let sheet { sheetContent(sheet).id(sheet.id) }
                    }
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Close") { sheet = nil }
                        }
                    }
                }
                .tint(.primary)
            }
            .alert("Couldn't update", isPresented: Binding(get: { error != nil }, set: { if !$0 { error = nil } })) {
                Button("OK", role: .cancel) { error = nil }
            } message: { Text(error ?? "") }
        }
        .tint(.primary)
    }

    private var header: some View {
        HStack {
            Button { sheet = .profile } label: {
                Image(systemName: "person.crop.circle")
                    .font(.title2)
                    .frame(width: 48, height: 48)
                    .background(Color(.secondarySystemBackground), in: Circle())
            }
            .accessibilityLabel("Profile")
            Spacer()
            Text("\(store.points) points")
                .font(.subheadline)
                .accessibilityIdentifier("balance")
            NavigationLink {
                RewardsView(store: store)
            } label: {
                Image(systemName: "storefront")
                    .font(.title3)
                    .frame(width: 48, height: 48)
                    .background(Color(.secondarySystemBackground), in: Circle())
            }
            .accessibilityLabel("Rewards")
        }
    }

    private var listControls: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Button { sheet = .lists } label: {
                    HStack(spacing: 6) {
                        Text(store.activeList?.name ?? "Choose a list").fontWeight(.medium)
                        Image(systemName: "chevron.down").font(.caption)
                    }
                }
                .accessibilityIdentifier("listPicker")
                Spacer(minLength: 8)
                if !Calendar.current.isDateInToday(date) {
                    Button("Today") { date = store.today; allChores = false }
                        .buttonStyle(.bordered)
                }
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    filter("Mine", selected: mine) { mine = true }
                    filter("Everyone", selected: !mine) { mine = false }
                    if store.canManage {
                        filter("All chores", selected: allChores) { allChores.toggle() }
                    }
                }
            }
        }
    }

    private func filter(_ title: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline)
                .padding(.horizontal, 16)
                .frame(minHeight: 44)
                .background(selected ? Color.primary : Color(.secondarySystemBackground), in: Capsule())
                .foregroundStyle(selected ? Color(.systemBackground) : Color.primary)
        }
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    private var choreList: some View {
        let chores = store.visibleChores(on: date, mine: mine, all: allChores)
        return LazyVStack(alignment: .leading, spacing: 22) {
            if chores.isEmpty {
                ContentUnavailableView {
                    Label(store.activeList == nil ? "No shared lists yet" : "No chores here", systemImage: "checklist")
                } description: {
                    Text(store.activeList == nil ? "Ask your manager to share a list." :
                         store.canManage ? "Add a chore, or choose another day." : "Try another day or choose Everyone.")
                }
                .padding(.top, 24)
            }
            ForEach(chores) { chore in
                ChoreRow(chore: chore, completion: store.completion(for: chore, on: date),
                         assignee: store.name(for: store.completion(for: chore, on: date)?.personID ?? chore.assigneeID),
                         canEdit: store.canManage) {
                    do { try store.toggle(chore, on: date) } catch { self.error = error.localizedDescription }
                } edit: { sheet = .chore(chore) }
            }
        }
    }

    @ViewBuilder
    private func sheetContent(_ item: ChoreSheet) -> some View {
        switch item {
        case .profile:
            List {
                Section("Switch profile") {
                    ForEach(store.people) { person in
                        Button {
                            store.personID = person.id
                            allChores = false
                            sheet = nil
                        } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(person.name)
                                    Text(person.isManager ? "Manager" : "Member").font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                if person.id == store.personID { Image(systemName: "checkmark") }
                            }
                        }
                    }
                }
                Section {
                    Label("\(store.person.name): \(store.streak)-day streak", systemImage: "flame")
                    if store.canManage { Button("Share current list") { sheet = .share } }
                    if store.person.isManager { Button("Create list") { sheet = .newList } }
                } footer: {
                    Text("Demo profiles. Changes stay in this session and reset when you relaunch the app.")
                }
            }
            .navigationTitle("Household")
        case .lists:
            List {
                Section("Your lists") {
                    if store.visibleLists.isEmpty { Text("No shared lists").foregroundStyle(.secondary) }
                    ForEach(store.visibleLists) { list in
                        Button {
                            store.selectedListID = list.id
                            allChores = false
                            sheet = nil
                        } label: {
                            HStack {
                                Text(list.name)
                                Spacer()
                                if list.id == store.activeList?.id { Image(systemName: "checkmark") }
                            }
                        }
                    }
                }
                Section {
                    if store.person.isManager { Button("Create list", systemImage: "plus") { sheet = .newList } }
                    if store.canManage { Button("Share current list", systemImage: "person.badge.plus") { sheet = .share } }
                }
            }
            .navigationTitle("Your lists")
        case .share:
            List(store.people) { person in
                let shared = store.activeList?.memberIDs.contains(person.id) == true
                Button {
                    do { try store.share(with: person.id) } catch { self.error = error.localizedDescription }
                } label: {
                    HStack {
                        Text(person.name)
                        Spacer()
                        Text(shared ? "Has access" : "Share").foregroundStyle(.secondary)
                        if shared { Image(systemName: "checkmark") }
                    }
                }
                .disabled(shared)
            }
            .navigationTitle("Share list")
        case .newList:
            ListEditor { name in
                try store.createList(name: name)
                allChores = false
                sheet = nil
            }
        case .chore(let chore):
            ChoreEditor(chore: chore, members: store.members, date: date) { title, points, assignee, frequency in
                try store.saveChore(existing: chore, title: title, points: points, assigneeID: assignee, frequency: frequency, date: date)
                if assignee != store.personID { mine = false }
                if frequency == .none || assignee == nil { allChores = true }
                sheet = nil
            }
        }
    }
}

private enum ChoreSheet: Identifiable {
    case profile, lists, share, newList, chore(Chore?)
    var id: String {
        switch self {
        case .profile: "profile"
        case .lists: "lists"
        case .share: "share"
        case .newList: "newList"
        case .chore(let chore): chore?.id.uuidString ?? "newChore"
        }
    }
}

#Preview { ContentView() }
