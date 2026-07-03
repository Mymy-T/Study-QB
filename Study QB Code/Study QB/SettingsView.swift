//
//  SettingsView.swift
//  Study QB
//
//  Created by Milo on 6/26/26.
//

import SwiftData
import SwiftUI

struct SettingsView: View {
    @Environment(\.modelContext) var modelContext
    @Query var cards: [Card]
    
    @State private var showDeleteAllAlert = false
    
    var body: some View {
        NavigationStack {
            Form {
                Section("Notifications") {
                    Text("Todo")
                }
                Section("Delete All Cards") {
                    Button {
                        showDeleteAllAlert = true
                    } label: {
                        Image(systemName: "trash")
                    }.tint(.red)
                }
            }.navigationTitle("Settings")
                .alert("ARE YOU SURE?", isPresented: $showDeleteAllAlert) {
                    Button("Cancel", role: .cancel) { }
                    Button("Delete", role: .destructive) {
                        for card in cards {
                            modelContext.delete(card)
                        }
                    }
                } message: {
                    Text("Are you sure you want to delete ALL of your cards? This action is irreversible.")
                }
        }
    }
}
