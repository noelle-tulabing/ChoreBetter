import SwiftUI

struct ChoreRow: View {
    let chore: Chore
    let completion: Completion?
    let assignee: String
    let canEdit: Bool
    let toggle: () -> Void
    let edit: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Button(action: toggle) {
                Image(systemName: completion == nil ? "circle" : "checkmark.circle.fill")
                    .font(.system(size: 26, weight: .light))
                    .foregroundStyle(completion == nil ? Color.secondary : Color.primary)
                    .frame(width: 44, height: 48)
            }
            .accessibilityLabel("\(completion == nil ? "Complete" : "Uncheck") \(chore.title)")
            .accessibilityValue(completion == nil ? "Not completed" : "Completed")
            VStack(alignment: .leading, spacing: 6) {
                Text(chore.title)
                    .font(.body)
                    .strikethrough(completion != nil)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(Color(.systemGray5), in: RoundedRectangle(cornerRadius: 24))
                Text("\(assignee) · \(chore.frequency.rawValue) · \(completion?.points ?? chore.points) pts\(completion == nil ? "" : " · Done")")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .padding(.leading, 12)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if canEdit {
                Button("Edit", action: edit)
                    .font(.subheadline)
                    .frame(minWidth: 44, minHeight: 44)
                    .accessibilityLabel("Edit \(chore.title)")
            }
        }
    }
}
