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
    let editingMode: Bool
    @Binding var manuallySelectedCards: [Card]?
    
    @State private var showingDeleteAlert = false
    @State private var cardToDelete: Card?
    
    let searchText: String
    let categories: [Category]
    let onlyShowMarked: Bool
    
    var searchResults: [Card] {
        let filteredCards = {
            
            var cardPool: [Card] = []
            if onlyShowMarked {
                cardPool = cards.filter {
                    $0.markedForReview == true
                }
            } else {
                cardPool = cards
            }
            
            return cardPool.filter {
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
                   ForEach(searchResults) { card in
                       if editingMode {
                           // Presented when the "Manage Cards" tab is clicked
                           NavigationLink {
                               GeometryReader { geometry in
                                   CardEditorView(geometry: geometry, existingCard: card)
                               }
                           } label: {
                               cardLabel(card: card)
                                   .onLongPressGesture(minimumDuration: 1) {
                                       cardToDelete = card
                                       showingDeleteAlert = true
                                   }
                           }
                       } else {
                           // Presented when user is manually selecting cards to review
                           // manuallySelectedCards will exist in this case
                           Button {
                               if manuallySelectedCards!.contains(card) {
                                   manuallySelectedCards!.removeAll {
                                       $0.persistentModelID == card.persistentModelID
                                   }
                               } else {
                                   manuallySelectedCards?.append(card)
                               }
                           } label: {
                               cardLabel(card: card)
                                   .border(.blue.opacity(0.4), width: manuallySelectedCards!.contains(card) ? 4 : 0)
                           }
                       }
                   }
               }.padding()
           } else {
               Text("No cards match the selected criteria.")
           }
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
    
    init(geometry: GeometryProxy, searchText: String, categories: [Category], onlyShowMarked: Bool, editingMode: Bool, sortParameter: SortParameter, manuallySelectedCards: Binding<[Card]?>) {
        self.geometry = geometry
        self.searchText = searchText
        self.categories = categories
        self.onlyShowMarked = onlyShowMarked
        self.editingMode = editingMode
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
        }
        self._manuallySelectedCards = manuallySelectedCards
    }
    
    
    private func cardLabel(card: Card) -> some View {
        return VStack {
            Text(card.answer)
                .font(.title)
                .minimumScaleFactor(0.33)
                .bold()
            VStack {
                Text("Reviewed on")
                Text(dateStringFromCard(card: card))
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
    }
    
    private func dateStringFromCard(card: Card) -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.locale = locale
        dateFormatter.dateStyle = .medium
        dateFormatter.timeStyle = .none
        return dateFormatter.string(from: card.dateReviewed)
    }
    
}
