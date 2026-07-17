//
//  ContentView.swift
//  Study QB
//
//  Created by Milo on 10/23/25.
//

import SwiftUI

enum Tabs: Hashable {
    case analyze, manage, review, settings
}


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
            Tab("Cards", systemImage: "note.text", value: .manage) {
                GeometryReader { geometry in
                    ManageCardView(geometry: geometry, editingMode: true, manuallySelectedCards: .constant(nil))
                }
            }
            Tab("Review", systemImage: "timer", value: .review) {
                PracticeSetupView()
                    .id(reviewId)
            }
            Tab("Settings", systemImage: "gear", value: .settings) {
                SettingsView()
            }
        }.onChange(of: selectedTab) { oldValue, newValue in
            if newValue == .review {
                // Refresh ReviewView whenever clicking back into it; this will reset manuallySelectedCards, which may contain deleted cards
                reviewId = UUID()
            }
        }
    }
}
