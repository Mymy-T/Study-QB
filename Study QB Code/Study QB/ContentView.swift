//
//  ContentView.swift
//  Study QB
//
//  Created by Milo on 10/23/25.
//

import SwiftUI

enum Tabs: Hashable {
    case analyze, manage, review
}

// TODO: add option to flag difficult cards for review; make sure existing cards don't break upon datamodel change
// TODO: complete migration to min version of ios 18
// TODO: Add freq lists
// TODO: Add widgets/notifications to remind user to study a few cards for a short duration
// TODO: Remove shuffle cards option and ensure that manually selected cards are also randomized


struct ContentView: View {
    
    @State private var selectedTab: Tabs = .analyze
    @State private var reviewId = UUID()
    
    var body: some View {
        TabView(selection: $selectedTab) {
            Tab("Analyze", systemImage: "magnifyingglass", value: .analyze) {
                GeometryReader { geometry in
                    AnalyzeView(geometry: geometry)
                }
            }
            Tab("Manage Cards", systemImage: "note.text", value: .manage) {
                GeometryReader { geometry in
                    ManageCardView(geometry: geometry, editingMode: true, manuallySelectedCards: .constant(nil))
                }
            }
            Tab("Review Cards", systemImage: "timer", value: .review) {
                PracticeSetupView()
                    .id(reviewId)
            }
        }.onChange(of: selectedTab) { oldValue, newValue in
            if newValue == .review {
                // Refresh ReviewView whenever clicking back into it; this will reset manuallySelectedCards, which may contain deleted cards
                reviewId = UUID()
            }
        }
    }
}
