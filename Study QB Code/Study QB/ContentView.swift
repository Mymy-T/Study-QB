//
//  ContentView.swift
//  Study QB
//
//  Created by Milo on 10/23/25.
//

import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            Tab("Analyze", systemImage: "magnifyingglass") {
                GeometryReader { geometry in
                    AnalyzeView(geometry: geometry)
                }
            }
            Tab("Manage Cards", systemImage: "note.text") {
                GeometryReader { geometry in
                    ManageCardView(geometry: geometry, editingMode: true, manuallySelectedCards: .constant(nil))
                }
            }
            Tab("Review Cards", systemImage: "timer") {
                ReviewView()
            }
        }
    }
}
