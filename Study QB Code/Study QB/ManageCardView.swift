//
//  ManageCardView.swift
//  Study QB
//
//  Created by Milo on 11/5/25.
//

import SwiftData
import SwiftUI

struct ManageCardView: View {
    
    @Query(sort: [SortDescriptor(\Card.dateReviewed)]) var cards: [Card]
    
    
    let columns = [GridItem(.flexible()), GridItem(.flexible())]
    
    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVGrid(columns: columns) {
                    ForEach(cards) { card in
                        NavigationLink {
                            CardEditorView(existingCard: card)
                        } label: {
                            VStack {
                                Text(card.answer)
                                    .bold()
                                if let cardCategory = card.category {
                                    Text(cardCategory.rawValue)
                                }
                            }
                            .foregroundStyle(.white)
                            .padding()
                                .background(
                                    RoundedRectangle(cornerRadius: 10)
                                        .fill(Color.blue.gradient.opacity(0.5)) // TODO: Add color based on category
                                )
                        }
                        
                    }
                }
            }
        }
    }
}

