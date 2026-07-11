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
    @State private var showClearCacheAlert = false
    
    var body: some View {
        NavigationStack {
            Form {
                Section("Notifications") {
                    Text("Todo")
                }
                Section("Delete") {
                        
                    Button {
                        showClearCacheAlert = true
                    } label: {
                        Label("Delete Cached Tossup Data", systemImage: "trash")
                            .foregroundStyle(.red)
                    }
                    .buttonStyle(.borderless)
                    
                    
                    Button {
                        showDeleteAllAlert = true
                    } label: {
                        Label("DELETE ALL CARDS", systemImage: "trash")
                            .foregroundStyle(.red)
                    }
                    .buttonStyle(.borderless)
                    
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
                .alert("Delete cached tossup data?", isPresented: $showClearCacheAlert) {
                    Button("Cancel", role: .cancel) { }
                    Button("Clear", role: .destructive) {
                        for card in cards {
                            card.cachedTossup = nil
                        }
                    }
                }
        }
    }
}
