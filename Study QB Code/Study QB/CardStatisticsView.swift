//
//  CardStatisticsView.swift
//  Study QB
//
//  Created by Milo on 8/7/26.
//

import SwiftData
import SwiftUI

struct CardStatisticsView: View {

    @Query(sort: \CardStat.answer) var cardStats: [CardStat]

    var body: some View {
        List {
            if cardStats.isEmpty {
                Text("No cards reviewed yet.")
                    .foregroundStyle(.secondary)
            } else {
                ForEach(cardStats) { stat in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(stat.answer)
                            .font(.headline)
                        Text(stat.category.rawValue)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Text("Seen \(stat.timesSeen)x — \(stat.timesCorrect) correct — avg buzz \(Int(stat.averageBuzzPositionPercent.rounded()))%")
                            .font(.subheadline)
                    }
                }
            }
        }
        .navigationTitle("Card Performance")
    }
}
