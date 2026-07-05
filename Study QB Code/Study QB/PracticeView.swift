//
//  PracticeView.swift
//  Study QB
//
//  Created by Milo on 12/22/25.
//

import SwiftData
import SwiftFSRS
import SwiftUI

struct PracticeView: View {
    
    @Environment(\.dismiss) var dismiss
    @Environment(\.locale) var locale
    @Environment(\.modelContext) var modelContext
    
    let reviewCards: [any CardLike]
    // Various modes
    let sequentialClues: Bool
    let singleClue: Bool
    let spacedRepetition: Bool
    @State var currentCard: any CardLike = Card(dateReviewed: Date.now, answer: "Placeholder", details: "Placeholder", category: .lit, personalNotes: "Placeholder", rgba: convertColorToRGBA(Color.black))
    @State private var answerRevealed = false
    @State private var clues: [String] = [] // Only used if sequentialClues is true
    @State private var clueIndex = 0 // Only used if sequentialClues is true
    @State private var maxClueIndex = 0 // Only used if sequentialClues is true
    @State private var question = ""
    @State private var answer = ""
    @State private var showingDetails = false
    
//    @State private var spacedRepetitionOld: FSRSCard
    @State private var spacedRepetitionRating: Rating? = nil
    @State private var finishedReviewing = false
    
    var markedForReview: Bool {
        if let card = currentCard as? Card {
            return card.markedForReview
        } else {
            let card = currentCard as! Pseudocard
            return card.parentCard.markedForReview
        }
    }
    
    var projectedDueDateString: String {
        if spacedRepetition {
            let scheduler = LongTermScheduler()
            let card = currentCard as! Card
            guard let rating = spacedRepetitionRating else {
                 return ""
            }
            
            let review = scheduler.schedule(
                card: card.fsrsCard!,
                algorithm: .v5,
                reviewRating: rating, // or .easy, .hard, .again
                reviewTime: Date()
            )
            let dateFormatter = DateFormatter()
            dateFormatter.locale = locale
            dateFormatter.dateStyle = .medium
            dateFormatter.timeStyle = .none
            return dateFormatter.string(from: review.postReviewCard.due)
            
        } else {
            return ""
        }
    }
    
    var body: some View {
        GeometryReader { geometry in
            
            VStack(alignment: .center) {
                
                HStack {
                    
                    Spacer()
                    
                    // MARK: Mark for review button
                    Button {
                        if let card = currentCard as? Card {
                            card.markedForReview.toggle()
                        } else {
                            let card = currentCard as! Pseudocard
                            card.parentCard.markedForReview.toggle()
                        }
                    } label: {
                        VStack {
                            Image(systemName: markedForReview ? "star.fill" : "star")
                                .resizable()
                                .scaledToFit()
                                .foregroundStyle(.yellow)
                            Text("Mark For Review")
                        }.frame(height: geometry.size.height/10)
                    }
                }.frame(width: geometry.size.width * 0.8)
                
                // MARK: CardView
                CardView(geometry: geometry, sequentialClues: sequentialClues, answerRevealed: $answerRevealed, question: $question, answer: $answer, onRevealAnswer: {
                    withAnimation(.easeInOut(duration: 0.25)) { answerRevealed.toggle() }
                }, onNextClue: {
                    guard clueIndex < maxClueIndex else { return }
                    withAnimation {
                        clueIndex += 1
                        question += "\n" + clues[clueIndex]
                    }
                })
                    .id(answer)
                    .transition(.opacity)
                
                if sequentialClues {
                    VStack(spacing: 4) {
                        ProgressView(value: Double(clueIndex + 1), total: Double(maxClueIndex + 1))
                        .animation(.spring(duration: 0.3), value: clueIndex)
                        .padding(.horizontal)
                        Text("\(clueIndex + 1) of \(maxClueIndex + 1) clues revealed")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                
                VStack {
                    HStack {
                        Button {
                            withAnimation(.easeInOut(duration: 0.25)) {
                                answerRevealed.toggle()
                            }
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
                                        .fill(Color.purple.gradient)
                                )
                        }
                        
                        if sequentialClues {
                            // MARK: Next Clue Button
                            Button {
                                withAnimation(.easeIn(duration: 0.2)) {
                                    clueIndex += 1
                                    question += ("\n" + clues[clueIndex])
                                }
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
                    
                    // MARK: Spaced Repetition Buttons
                    if spacedRepetition {
                        VStack {
                            Text("**\(projectedDueDateString)**")
                            HStack {
                                Button {
                                    spacedRepetitionRating = .again
                                } label: {
                                    Text("Again")
                                        .foregroundStyle(.white)
                                        .bold()
                                        .padding()
                                        .background(
                                            RoundedRectangle(cornerRadius: 10)
                                                .fill(Color.red.gradient)
                                        )
                                        .border(.blue, width: spacedRepetitionRating == Rating.again ? 4 : 0)
                                }
                                
                                Button {
                                    spacedRepetitionRating = .hard
                                } label: {
                                    Text("Hard")
                                        .foregroundStyle(.white)
                                        .bold()
                                        .padding()
                                        .background(
                                            RoundedRectangle(cornerRadius: 10)
                                                .fill(Color.orange.gradient)
                                        )
                                        .border(.blue, width: spacedRepetitionRating == Rating.hard ? 4 : 0)
                                }
                                
                                Button {
                                    spacedRepetitionRating = .good
                                } label: {
                                    Text("Good")
                                        .foregroundStyle(.white)
                                        .bold()
                                        .padding()
                                        .background(
                                            RoundedRectangle(cornerRadius: 10)
                                                .fill(Color.yellow.gradient)
                                        )
                                        .border(.blue, width: spacedRepetitionRating == Rating.good ? 4 : 0)
                                }
                                
                                Button {
                                    spacedRepetitionRating = .easy
                                } label: {
                                    Text("Easy")
                                        .foregroundStyle(.white)
                                        .bold()
                                        .padding()
                                        .background(
                                            RoundedRectangle(cornerRadius: 10)
                                                .fill(Color.green.gradient)
                                        )
                                        .border(.blue, width: spacedRepetitionRating == Rating.easy ? 4 : 0)
                                }
                                
                            } // End of HStack
                            
                        }
                        
                    } // End of if
                    Spacer()
                    // MARK: Next Card Button
                        Button {
                            answerRevealed = false
                            if spacedRepetition {
                                scheduleReview(card: currentCard as! Card, rating: spacedRepetitionRating!)
                                withAnimation(.easeInOut(duration: 0.2)) {
                                    (currentCard, finishedReviewing) = getNextCard(current: currentCard, singleClueMode: singleClue)
                                }
                                spacedRepetitionRating = nil
                            } else {
                                withAnimation(.easeInOut(duration: 0.2)) {
                                    (currentCard, _) = getNextCard(current: currentCard, singleClueMode: singleClue)
                                }
                            }
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
                        .disabled(((spacedRepetition == true && spacedRepetitionRating == nil) ? true : false))
                        .opacity(((spacedRepetition == true && spacedRepetitionRating == nil) ? 0.4 : 1))
                        
                    
                    
                    
                    // MARK: Undo Button
                    // for spaced repetition only
                    // MARK: TODO
                    
                }
                
                
                
                
            }.onAppear {
                currentCard = reviewCards[0]
                if sequentialClues {
                    clues = currentCard.personalNotes.split(whereSeparator: \.isNewline).map { String($0) }
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
            .navigationBarBackButtonHidden(true)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "chevron.backward")
                    }
                }
                
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showingDetails = true
                    } label: {
                        Image(systemName: "info.circle")
                    }
                }
                
            }
            .toolbarVisibility(.hidden, for: .tabBar)
            .frame(width: geometry.size.width * 0.9, height: geometry.size.height * 0.9)
            .position(x: geometry.size.width * 0.5, y: geometry.size.height * 0.5)
            .opacity(finishedReviewing ? 0 : 1)
            .allowsHitTesting(finishedReviewing ? false : true)
            .sheet(isPresented: $showingDetails) {
                NavigationStack {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 12) {
                            Text(currentCard.answer)
                                .font(.title2).bold()
                            if let card = currentCard as? Card {
                                Text(card.details)
                                    .padding()
                            } else if let pseudocard = currentCard as? Pseudocard {
                                Text(pseudocard.parentCard.details)
                                    .padding()
                            }
                        }
                        .padding()
                    }
                    .navigationTitle("Details")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .topBarTrailing) {
                            Button("Done") { showingDetails = false }
                        }
                    }
                }
            }
            .sensoryFeedback(.impact(weight: .light),  trigger: clueIndex)
            .sensoryFeedback(.impact(weight: .medium), trigger: answerRevealed)
            .sensoryFeedback(.impact(weight: .light),  trigger: spacedRepetitionRating)
            .sensoryFeedback(.impact(weight: .heavy),  trigger: answer)
            
            VStack {
                Text("Congrats!")
                    .font(.headline)
                Text("You have finished all of your automatically scheduled cards for today.")
            }.opacity(finishedReviewing ? 1 : 0)
            
        } // End of GeometryReader
        
        
        
    }
    
    // MARK: getNextCard
    private func getNextCard(current: any CardLike, singleClueMode: Bool) -> (any CardLike, Bool) {
        // returns a typle of (nextCard, complete) where complete is true if a full pass of the cards has been completed
        if !singleClueMode {
            let cards = reviewCards as! [Card]
            let currentCard = current as! Card
            
            guard let currentIndex = cards.firstIndex(of: currentCard) else {
                print("Error: currentCard not in reviewCards")
                return (current, false)
            }
            if currentIndex + 1 < cards.count {
                return (cards[currentIndex + 1], false)
                
            } else {
                return (cards[0], true)
            }
        } else {
            let cards = reviewCards as! [Pseudocard]
            let currentCard = current as! Pseudocard
            
            guard let currentIndex = cards.firstIndex(of: currentCard) else {
                print("Error: currentCard not in reviewCards")
                return (current, false)
            }
            if currentIndex + 1 < cards.count {
                return (cards[currentIndex + 1], false)
                
            } else {
                return (cards[0], true)
            }
        }
    }
    
    // MARK: scheduleReview
    private func scheduleReview(card: Card, rating: Rating) {
        // Returns a tuple of (oldCard, newCard) where newCard has an fsrsCard with the new scheduled review date and other properties
        let scheduler = LongTermScheduler()
        
        let review = scheduler.schedule(
            card: card.fsrsCard!,
            algorithm: .v5,
            reviewRating: rating, // or .easy, .hard, .again
            reviewTime: Date()
        )
        
//        let newCard = Card(dateReviewed: card.dateReviewed, answer: card.answer, details: card.details, category: card.category, personalNotes: card.personalNotes, rgba: card.rgba, markedForReview: card.markedForReview, fsrsCard: review.postReviewCard)
        
//        spacedRepetitionOld = card.fsrsCard
        
        card.fsrsCard = review.postReviewCard
        
        do {
            try modelContext.save()
        } catch {
            print("Error in saving scheduled card")
        }
        
    }
    
    
}

// MARK: CardView
struct CardView: View {
    let geometry: GeometryProxy
    let sequentialClues: Bool
    @Binding var answerRevealed: Bool
    @Binding var question: String
    @Binding var answer: String
    
    // For dealing with swipe gestures
    var onRevealAnswer: (() -> Void)? = nil
    var onNextClue: (() -> Void)? = nil
    
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
            .simultaneousGesture(
                DragGesture(minimumDistance: 40)
                    .onEnded { value in // Origin at top left, positive y goes down
                        let isRightSwipe = value.translation.width > 40 &&
                        abs(value.translation.width) >
                        abs(value.translation.height)
                        if isRightSwipe && sequentialClues {
                            onNextClue?()
                        }
                    }
            )
                
            Text(answer)
                .padding()
                .background(
                    RoundedRectangle(cornerRadius: 10)
                        .fill(Color.green.opacity(0.3).gradient)
                )
                .opacity(answerRevealed ? 1 : 0)
        }
        .contentShape(Rectangle())
        .onTapGesture {
            onRevealAnswer?()
        }
    }
}
