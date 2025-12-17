//
//  CardGrid.swift
//  Study QB
//
//  Created by Milo on 11/27/25.
//

import SwiftData
import SwiftUI

struct ManageCardGrid: View {
    @Environment(\.locale) var locale
    @Environment(\.modelContext) var modelContext
    @Query(sort: [SortDescriptor(\Card.dateReviewed)]) var cards: [Card]
    
    let geometry: GeometryProxy
    let columns = [GridItem(.flexible()), GridItem(.flexible())]
    
    @State private var showingDeleteAlert = false
    @State private var cardToDelete: Card?
    
    let searchText: String
    let categories: [Category]
//    let sortParameter: SortParameter
    
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
        let filteredCards = {
            return cards.filter {
                categories.contains($0.category)
            }
        }()
            if searchText.isEmpty {
                return filteredCards
            } else {
                return filteredCards.filter { $0.answer.localizedStandardContains(searchText) }
            }
        }
    
    var body: some View {
       ScrollView {
           if searchResults.count != 0 {
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
                               Text(card.category.rawValue)
                                   .font(.caption)
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
           } else {
               Text("No cards match the selected criteria.")
           }
        }
    }
    
    init(geometry: GeometryProxy, searchText: String, categories: [Category], sortParameter: SortParameter) {
        self.geometry = geometry
        self.searchText = searchText
        self.categories = categories
        switch sortParameter {
        case .reviewDateReverse:
            let sortDescriptor = SortDescriptor(\Card.dateReviewed, order: .reverse)
            _cards = Query(sort: [sortDescriptor])
        case .reviewDateForward:
            let sortDescriptor = SortDescriptor(\Card.dateReviewed, order: .forward)
            _cards = Query(sort: [sortDescriptor])
        case .titleAlphabeticalForward:
            let sortDescriptor = SortDescriptor(\Card.answer, order: .forward)
            _cards = Query(sort: [sortDescriptor])
        case .titleAlphabeticalReverse:
            let sortDescriptor = SortDescriptor(\Card.answer, order: .reverse)
            _cards = Query(sort: [sortDescriptor])
//        case .categoryAlphabeticalForward:
//            let sortDescriptor = SortDescriptor(\Card.category.rawValue, order: .forward)
//            _cards = Query(sort: [sortDescriptor])
//        case .categoryAlphabeticalReverse:
//            let sortDescriptor = SortDescriptor(\Card.category.rawValue, order: .reverse)
//            _cards = Query(sort: [sortDescriptor])
        }
    }
    
    
}
