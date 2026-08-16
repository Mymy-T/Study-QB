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

    @State private var searchText = ""

    var filteredStats: [CardStat] {
        if searchText.isEmpty {
            return cardStats
        }
        return cardStats.filter { $0.answer.localizedStandardContains(searchText) }
    }

    var body: some View {
        List {
            if cardStats.isEmpty {
                Text("No cards reviewed yet.")
                    .foregroundStyle(.secondary)
            } else if filteredStats.isEmpty {
                Text("No matching cards.")
                    .foregroundStyle(.secondary)
            } else {
                ForEach(filteredStats) { stat in
                    let buzzScore = stat.averageBuzzPositionPercent / 100
                    VStack(alignment: .leading, spacing: 4) {
                        Text(stat.answer)
                            .font(.headline)
                        Text(stat.category.rawValue)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        HStack(spacing: 4) {
                            Text("Seen \(stat.timesSeen)x — \(stat.timesCorrect) correct — avg buzz")
                                .font(.subheadline)
                            Text(String(format: "%.2f", buzzScore))
                                .font(.subheadline)
                                .fontWeight(.semibold)
                                .foregroundStyle(colorForNormalizedScore(buzzScore))
                        }
                    }
                }
            }
        }
        .searchable(text: $searchText, placement: .navigationBarDrawer(displayMode: .always), prompt: "Search for a card")
        .navigationTitle("Card Performance")
    }
}
