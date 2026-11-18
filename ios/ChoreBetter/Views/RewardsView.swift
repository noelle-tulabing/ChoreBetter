import SwiftUI

struct RewardsView: View {
    let store: DemoStore
    @Environment(\.dismiss) private var dismiss
    @State private var adding = false
    @State private var redeeming: Reward?
    @State private var error: String?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                HStack {
                    Button { dismiss() } label: { Label("Back", systemImage: "chevron.left") }
                        .frame(minHeight: 44)
                    Spacer()
                    Text("\(store.points) points").accessibilityIdentifier("balance")
                }
                VStack(alignment: .leading, spacing: 6) {
                    Text("Rewards").font(.largeTitle).fontWeight(.semibold)
                    Text(store.activeList?.name ?? "Choose a list to see rewards").foregroundStyle(.secondary)
                }
                if store.currentRewards.isEmpty {
                    ContentUnavailableView("No rewards yet", systemImage: "gift", description: Text("Your list manager can add rewards."))
                }
                ForEach(store.currentRewards) { reward in
                    VStack(alignment: .leading, spacing: 12) {
                        Text(reward.title).font(.headline)
                        Text("\(reward.cost) points")
                        if reward.screenMinutes > 0 {
                            Label("Screen time not connected", systemImage: "clock.badge.exclamationmark")
                                .font(.caption).foregroundStyle(.secondary)
                        }
                        HStack {
                            Spacer()
                            Button(reward.screenMinutes > 0 ? "Unavailable" :
                                   store.points < reward.cost ? "Need \(reward.cost - store.points) more" : "Redeem") {
                                redeeming = reward
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(.primary)
                            .disabled(reward.screenMinutes > 0 || store.points < reward.cost)
                            .accessibilityLabel("\(reward.screenMinutes > 0 ? "Unavailable" : "Redeem"): \(reward.title)")
                        }
                    }
                    .padding(20)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 16))
                }
                if !store.history.isEmpty {
                    Text("Your redeemed rewards").font(.headline).padding(.top, 8)
                    ForEach(store.history) { redemption in
                        HStack(alignment: .top) {
                            Image(systemName: "checkmark.circle")
                            Text(redemption.title)
                            Spacer()
                            Text("\(redemption.cost) pts").foregroundStyle(.secondary)
                        }
                    }
                }
            }
            .padding(20)
            .padding(.bottom, 88)
            .frame(maxWidth: 640)
            .frame(maxWidth: .infinity)
        }
        .toolbar(.hidden, for: .navigationBar)
        .overlay(alignment: .bottomTrailing) {
            if store.canManage {
                Button("Add reward", systemImage: "plus") { adding = true }
                    .buttonStyle(.borderedProminent)
                    .tint(.primary)
                    .controlSize(.large)
                    .padding(24)
            }
        }
        .sheet(isPresented: $adding) {
            NavigationStack {
                RewardEditor { title, cost, minutes in
                    try store.addReward(title: title, cost: cost, minutes: minutes)
                    adding = false
                }
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) { Button("Cancel") { adding = false } }
                }
            }
            .tint(.primary)
        }
        .alert("Redeem \(redeeming?.title ?? "reward")?", isPresented: Binding(
            get: { redeeming != nil }, set: { if !$0 { redeeming = nil } }
        ), presenting: redeeming) { reward in
            Button("Cancel", role: .cancel) { redeeming = nil }
            Button("Redeem") {
                do { try store.redeem(reward) } catch { self.error = error.localizedDescription }
                redeeming = nil
            }
        } message: { reward in
            Text("Spend \(reward.cost) points? Your remaining balance will be \(store.points - reward.cost) points.")
        }
        .alert("Couldn't redeem", isPresented: Binding(get: { error != nil }, set: { if !$0 { error = nil } })) {
            Button("OK", role: .cancel) { error = nil }
        } message: { Text(error ?? "") }
    }
}
