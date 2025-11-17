//
//  ManageCardView.swift
//  Study QB
//
//  Created by Milo on 11/5/25.
//

import SwiftData
import SwiftUI

struct ManageCardView: View {
    
    @Environment(\.locale) var locale
    @Environment(\.modelContext) var modelContext
    
    @Query(sort: [SortDescriptor(\Card.dateReviewed)]) var cards: [Card]
    
    let geometry: GeometryProxy
    
    @State private var showingDeleteAlert = false
    @State private var cardToDelete: Card?
    @State private var searchText = ""
    
    let columns = [GridItem(.flexible()), GridItem(.flexible())]
    
    var dateStrings: [String] {
        var strings: [String] = []
        for card in cards {
            let dateFormatter = DateFormatter()
            dateFormatter.locale = locale
            dateFormatter.dateStyle = .medium
            dateFormatter.timeStyle = .none
            strings.append(dateFormatter.string(from: card.dateReviewed))
        }
        return strings
    }
    
    var searchResults: [Card] {
            if searchText.isEmpty {
                return cards
            } else {
                return cards.filter { $0.answer.localizedStandardContains(searchText) }
            }
        }
    
    
    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVGrid(columns: columns) {
                    ForEach(searchResults.enumerated(), id: \.offset) { index, card in
                        NavigationLink {
                            GeometryReader { geometry in
                                CardEditorView(geometry: geometry, existingCard: card)
                            }
                        } label: {
                            VStack {
                                Text(card.answer)
                                    .font(.title)
                                    .minimumScaleFactor(0.33)
                                    .bold()
                                VStack {
                                    Text("Reviewed on")
                                    Text(dateStrings[index])
                                        .bold()
                                }
                                if let cardCategory = card.category {
                                    Text(cardCategory.rawValue)
                                        .font(.caption)
                                }
                            }
                            .foregroundStyle(.black)
                            .padding()
                            .frame(maxWidth: .infinity, maxHeight: geometry.size.height/4)
                                .background(
                                    RoundedRectangle(cornerRadius: 10)
                                        .fill(convertRGBAToColor(card.rgba).gradient)
                                )
                                .onLongPressGesture(minimumDuration: 1) {
                                    showingDeleteAlert = true
                                    cardToDelete = card
                                }
                                .alert("Delete Card?", isPresented: $showingDeleteAlert) {
                                    Button("No", role: .cancel) { }
                                    Button("Yes", role: .destructive) {
                                        guard let card = cardToDelete else {
                                            print("Failed to delete card")
                                            return
                                        }
                                        modelContext.delete(card)
                                        cardToDelete = nil
                                    }
                                }
                        }
                        
                    }
                }.padding()
            }.searchable(text: $searchText, prompt: "Search for a card")
                .navigationTitle("Your Cards")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        NavigationLink {
                            GeometryReader { geometry in
                                CardEditorView(geometry: geometry, existingCard: nil)
                            }
                        } label: {
                            Image(systemName: "plus")
                        }.tint(.green)
                    }
                }
        }
    }
    
    private func getDateString(date: Date) -> String {
        let dateString: String = {
            let dateFormatter = DateFormatter()
            dateFormatter.locale = locale
            dateFormatter.dateStyle = .medium
            dateFormatter.timeStyle = .short
            return dateFormatter.string(from: date)
        }()
        return dateString
    }
    
}

