import SwiftUI

struct WeekPicker: View {
    @Binding var date: Date
    var onSelect: () -> Void
    private let calendar = Calendar.current

    private var days: [Date] {
        let weekday = calendar.component(.weekday, from: date)
        let start = calendar.date(byAdding: .day, value: -(weekday - 1), to: date)!
        return (0..<7).map { calendar.date(byAdding: .day, value: $0, to: start)! }
    }

    var body: some View {
        VStack(spacing: 14) {
            HStack {
                Button { moveWeek(-1) } label: {
                    Image(systemName: "chevron.left").frame(width: 44, height: 44)
                }
                .accessibilityLabel("Previous week")
                Spacer()
                Text(date.formatted(.dateTime.month(.wide).year())).font(.subheadline)
                Spacer()
                Button { moveWeek(1) } label: {
                    Image(systemName: "chevron.right").frame(width: 44, height: 44)
                }
                .accessibilityLabel("Next week")
            }
            HStack(spacing: 0) {
                ForEach(days, id: \.self) { day in
                    let selected = calendar.isDate(day, inSameDayAs: date)
                    Button {
                        date = day
                        onSelect()
                    } label: {
                        VStack(spacing: 8) {
                            Text(day.formatted(.dateTime.weekday(.abbreviated)))
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                            Text(day.formatted(.dateTime.day()))
                                .font(.subheadline)
                                .frame(maxWidth: .infinity, minHeight: 40)
                                .background(selected ? Color.primary : Color(.secondarySystemBackground), in: Circle())
                                .foregroundStyle(selected ? Color(.systemBackground) : Color.primary)
                        }
                        .frame(maxWidth: .infinity)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(day.formatted(date: .complete, time: .omitted))
                    .accessibilityAddTraits(selected ? .isSelected : [])
                }
            }
        }
    }

    private func moveWeek(_ offset: Int) {
        date = calendar.date(byAdding: .day, value: offset * 7, to: date)!
        onSelect()
    }
}
