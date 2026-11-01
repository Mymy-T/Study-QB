//
//  StatisticsView.swift
//  Study QB
//
//  Created by Milo on 8/7/26.
//

import SwiftData
import SwiftUI

struct StatisticsView: View {

    @Query(sort: \BuzzGameStat.date, order: .reverse) var games: [BuzzGameStat]

    var totalPoints: Int { games.reduce(0) { $0 + $1.points } }
    var totalCorrect: Int { games.reduce(0) { $0 + $1.numberCorrect } }
    var totalIncorrect: Int { games.reduce(0) { $0 + $1.numberIncorrect } }

    var body: some View {
        NavigationStack {
            List {
                Section("Lifetime") {
                    LabeledContent("Total Points", value: "\(totalPoints)")
                    LabeledContent("Correct", value: "\(totalCorrect)")
                    LabeledContent("Incorrect", value: "\(totalIncorrect)")
                }
                Section {
                    NavigationLink("Card Performance") {
                        CardStatisticsView()
                    }
                }
                Section("Games") {
                    if games.isEmpty {
                        Text("No buzz-in games played yet.")
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(games) { game in
                            VStack(alignment: .leading, spacing: 4) {
                                Text(game.date.formatted(date: .abbreviated, time: .shortened))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                Text("\(game.points) pts — \(game.numberCorrect) correct, \(game.numberIncorrect) incorrect")
                            }
                        }
                    }
                }
            }
            .navigationTitle("Statistics")
        }
    }
}
