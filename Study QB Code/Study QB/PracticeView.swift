//
//  PracticeView.swift
//  Study QB
//
//  Created by Milo on 12/22/25.
//

import SwiftData
import SwiftUI

struct PracticeView: View {
    
    let reviewCards: [Card]
    let shuffle: Bool
    @State var currentCard = Card(dateReviewed: Date.now, answer: "Placeholder", details: "Placeholder", category: .lit, personalNotes: "Placeholder", rgba: convertColorToRGBA(Color.black))
    @State private var questionText = ""
    @State private var answerText = ""
    @State private var answerRevealed = false
    
    
    var body: some View {
        GeometryReader { geometry in
            VStack(alignment: .center) {
                CardView(geometry: geometry, answerRevealed: $answerRevealed, currentCard: $currentCard)
                HStack {
                    Button {
                        answerRevealed.toggle()
                    } label: {
                        Text(answerRevealed ? "Hide Answer" : "Show Answer")
                            .foregroundStyle(.white)
                            .bold()
                            .padding()
                            .background(
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(Color.blue.gradient)
                            )
                    }
                    
                    Button {
                        answerRevealed = false
                        currentCard = getNextCard(current: currentCard, shuffle: shuffle)
                    } label: {
                        Text("Next Card")
                            .foregroundStyle(.white)
                            .bold()
                            .padding()
                            .background(
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(Color.green.gradient)
                            )
                    }
                    
                }
            }.onAppear {
                currentCard = reviewCards[0]
            }
            .frame(width: geometry.size.width * 0.9, height: geometry.size.height * 0.9)
            .position(x: geometry.size.width * 0.5, y: geometry.size.height * 0.5)
            
        }
        
        
        
    }
    
    private func getNextCard(current: Card, shuffle: Bool) -> Card {
        if shuffle {
            if reviewCards.count > 1 {
                var newCard = current
                while newCard.persistentModelID == current.persistentModelID {
                    guard let card = reviewCards.randomElement() else {
                        print("Error: unable to get random element in reviewCards")
                        return current
                    }
                    newCard = card
                }
                return newCard
            } else {
                return current
            }
        } else {
            guard let currentIndex = reviewCards.firstIndex(of: current) else {
                print("Error: currentCard not in reviewCards")
                return current
            }
            if currentIndex < reviewCards.count - 1 {
                return reviewCards[currentIndex + 1]
            } else {
                return reviewCards[0]
            }
        }
    }
    
    
}


struct CardView: View {
    
    let geometry: GeometryProxy
    @Binding var answerRevealed: Bool
    @Binding var currentCard: Card
    
    var question: String {
        currentCard.personalNotes
    }
    
    var answer: String {
        currentCard.answer
    }
    
    var body: some View {
        VStack {
            ScrollView {
                Text(question)
                    .padding()
                    .scrollTransition(axis: .vertical) { content, phase in
                        content.opacity(phase.isIdentity ? 1 : 0)
                    }
            }
            .frame(width: geometry.size.width * 0.8, height: geometry.size.height * 0.5)
                .border(Color.blue.opacity(0.5).gradient, width: 5)
            Text(answer)
                .padding()
                .border(Color.green.opacity(0.5).gradient, width: 5)
                .opacity(answerRevealed ? 1 : 0)
        }
    }
}
