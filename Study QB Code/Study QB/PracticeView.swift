//
//  PracticeView.swift
//  Study QB
//
//  Created by Milo on 12/22/25.
//

import SwiftData
import SwiftUI

struct PracticeView: View {
    
    let reviewCards: [any CardLike]
    // Various modes
    let sequentialClues: Bool
    let singleClue: Bool
    @State var currentCard: any CardLike = Card(dateReviewed: Date.now, answer: "Placeholder", details: "Placeholder", category: .lit, personalNotes: "Placeholder", rgba: convertColorToRGBA(Color.black))
    @State private var answerRevealed = false
    @State private var clues: [String] = [] // Only used if sequentialClues is true
    @State private var clueIndex = 0 // Only used if sequentialClues is true
    @State private var maxClueIndex = 0 // Only used if sequentialClues is true
    @State private var question = ""
    @State private var answer = ""
    
    
    
    var body: some View {
        GeometryReader { geometry in
            VStack(alignment: .center) {
                CardView(geometry: geometry, sequentialClues: sequentialClues, answerRevealed: $answerRevealed, question: $question, answer: $answer)
                
                if sequentialClues {
                    Text("\(clueIndex + 1) out of \(maxClueIndex + 1) clues revealed.")
                }
                
                VStack {
                    HStack {
                        Button {
                            answerRevealed.toggle()
                            if answerRevealed {
                                if !singleClue {
                                    (currentCard as! Card).dateReviewed = .now
                                } else {
                                    (currentCard as! Pseudocard).parentCard.dateReviewed = .now
                                }
                            }
                        } label: {
                            Text(answerRevealed ? "Hide Answer" : "Show Answer")
                                .foregroundStyle(.white)
                                .bold()
                                .padding()
                                .background(
                                    RoundedRectangle(cornerRadius: 10)
                                        .fill(Color.orange.gradient)
                                )
                        }
                        
                        if sequentialClues {
                            // MARK: Next Clue Button
                            Button {
                                clueIndex += 1
                                question += ("\n" + clues[clueIndex])
                            } label: {
                                Text("Next Clue")
                                    .foregroundStyle(.white)
                                    .bold()
                                    .padding()
                                    .background(
                                        RoundedRectangle(cornerRadius: 10)
                                            .fill(Color.blue.gradient)
                                    )
                            }.disabled(clueIndex == maxClueIndex)
                                .opacity(clueIndex == maxClueIndex ? 0.5 : 1)
                        }
                        
                        
                    }
                    
                    Spacer()
                    
                    // MARK: Next Card Button
                    Button {
                        answerRevealed = false
                        currentCard = getNextCard(current: currentCard, singleClueMode: singleClue)
                        if sequentialClues {
                            clues = currentCard.personalNotes.split(whereSeparator: \.isNewline).map{ String($0) }
                            clues.reverse()
                            maxClueIndex = clues.count - 1
                            clueIndex = 0
                            question = clues[clueIndex]
                        } else {
                            question = currentCard.personalNotes
                        }
                        answer = currentCard.answer
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
                if sequentialClues {
                    clues = currentCard.personalNotes.split(whereSeparator: \.isNewline).map{ String($0) }
                    clues.reverse()
                    maxClueIndex = clues.count - 1
                    clueIndex = 0
                    question = clues[clueIndex]
                    answer = currentCard.answer
                } else {
                    question = currentCard.personalNotes
                    answer = currentCard.answer
                }
            }
            .frame(width: geometry.size.width * 0.9, height: geometry.size.height * 0.9)
            .position(x: geometry.size.width * 0.5, y: geometry.size.height * 0.5)
            
        }
        
        
        
    }
    
    // MARK: getNextCard
    private func getNextCard(current: any CardLike, singleClueMode: Bool) -> any CardLike {
//        if shuffle {
//            if reviewCards.count > 1 {
//                var newCard = current
//                while newCard.persistentModelID == current.persistentModelID {
//                    guard let card = reviewCards.randomElement() else {
//                        print("Error: unable to get random element in reviewCards")
//                        return current
//                    }
//                    newCard = card
//                }
//                return newCard
//            } else {
//                return current
//            }
//        } else {
        if !singleClueMode {
            
            let cards = reviewCards as! [Card]
            let currentCard = current as! Card
            
            guard let currentIndex = cards.firstIndex(of: currentCard) else {
                print("Error: currentCard not in reviewCards")
                return current
            }
            if currentIndex + 1 < cards.count {
                return cards[currentIndex + 1]
                
            } else {
                return cards[0]
            }
        } else {
            let cards = reviewCards as! [Pseudocard]
            let currentCard = current as! Pseudocard
            
            guard let currentIndex = cards.firstIndex(of: currentCard) else {
                print("Error: currentCard not in reviewCards")
                return current
            }
            if currentIndex + 1 < cards.count {
                return cards[currentIndex + 1]
                
            } else {
                return cards[0]
            }
        }
//        }
    }
    
    
    
}

// MARK: CardView
struct CardView: View {
    let geometry: GeometryProxy
    let sequentialClues: Bool
    @Binding var answerRevealed: Bool
    @Binding var question: String
    @Binding var answer: String
    
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
            .background(
                RoundedRectangle(cornerRadius: 10)
                    .fill(Color.blue.opacity(0.3).gradient)
            )
                
            Text(answer)
                .padding()
                .background(
                    RoundedRectangle(cornerRadius: 10)
                        .fill(Color.green.opacity(0.3).gradient)
                )
                .opacity(answerRevealed ? 1 : 0)
        }
    }
}
