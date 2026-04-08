//
//  ManageCardView.swift
//  Study QB
//
//  Created by Milo on 11/5/25.
//

import SwiftData
import SwiftUI

struct ManageCardView: View {
    
    let geometry: GeometryProxy
    let editingMode: Bool
    
    @State private var searchText = ""
    @State private var categories: [Category] = []
    @State private var onlyShowMarked = false
    @State private var sortParameter: SortParameter = .reviewDateReverse
    
    @Binding var manuallySelectedCards: [Card]?
    
    var body: some View {
        NavigationStack {
            
            ManageCardGrid(geometry: geometry, searchText: searchText, categories: categories, onlyShowMarked: onlyShowMarked, editingMode: editingMode, sortParameter: sortParameter, manuallySelectedCards: $manuallySelectedCards)
                .searchable(text: $searchText, placement: .navigationBarDrawer(displayMode: .always), prompt: "Search for a card")
                .navigationTitle(editingMode ? "Your Cards" : "\(manuallySelectedCards!.count) selected")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    
                    // MARK: Left toolbar items
                    ToolbarItemGroup(placement: .topBarLeading) {
                        
                        // Only show cards marked as review button
                        Button {
                            onlyShowMarked.toggle()
                        } label: {
                            Image(systemName: onlyShowMarked ? "star.fill" : "star")
                                .foregroundStyle(.yellow)
                        }
                        
                        // Filter Menu
                        CategorySelectionMenu(categories: $categories, labelView: categoryMenuLabel())
                        
                        // Sort Picker
                        Menu {
                            ForEach(SortParameter.allCases) { sortParam in
                                SortSelectionButton(sortParameter: sortParam, selectedSortParameter: $sortParameter)
                            }
                        } label: {
                            Image(systemName: "arrow.up.arrow.down")
                                .foregroundStyle(.blue)
                        }
                    }
                    
                    if editingMode {
                        ToolbarItem(placement: .topBarTrailing) {
                            NavigationLink {
                                GeometryReader { geometry in
                                    CardEditorView(geometry: geometry)
                                }
                            } label: {
                                Image(systemName: "plus")
                            }.tint(.green)
                        }
                    }
                }
        }
    }
    
    private func categoryMenuLabel() -> some View {
        return Image(systemName: "line.horizontal.3.decrease")
            .foregroundStyle(.blue)
    }
    
}

struct SortSelectionButton: View {
    let sortParameter: SortParameter
    @Binding var selectedSortParameter: SortParameter
    
    var body: some View {
        Button {
            selectedSortParameter = sortParameter
        } label: {
            HStack {
                
                Text(sortParameter.rawValue)
                if selectedSortParameter == sortParameter {
                    Image(systemName: "checkmark")
                }
            }
        }
    }
}

enum SortParameter: String, CaseIterable, Identifiable {
    case reviewDateForward = "Oldest Reviewed"
    case reviewDateReverse = "Most Recently Reviewed"
    case titleAlphabeticalForward = "Alphabetical, by Title (A-Z)"
    case titleAlphabeticalReverse = "Alphabetical, by Title (Z-A)"
    
    var id: String {
        self.rawValue
    }
}
