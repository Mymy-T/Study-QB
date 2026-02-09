//
//  ReviewView.swift
//  Study QB
//
//  Created by Milo on 12/20/25.
//

import SwiftData
import SwiftUI

// TODO: Fix keyboard toolbar (to allow dismissal of keyboard)

struct ReviewView: View {
    @Query var cards: [Card]
    
    @State private var useRandomCards = true
    @State private var numberOfRandomCards = 5
    @FocusState private var enteringCardNumber: Bool
    @State private var categories: [Category] = []
    @State private var manuallySelectedCards: [Card] = []
    @State private var showingManualSelectionView = false
    
    @State private var shuffleCards = false
    @State private var sequentialClues = false
    
    @State private var showingCardNumberAlert = false
    @State private var cardNumberAlertMessage = ""
    @State private var showingMissingNotesAlert = false
    @State private var missingNotesAlertMessage = ""
    
    @State private var practicing = false
    @State private var cardsToReview: [Card] = []
    
    var numOfAvaliableRandomCards: Int {
        var totalNum = 0
        for category in categories {
            let categoryCards = cards.filter { cardToFilter in
                return cardToFilter.category == category
            }
            totalNum += categoryCards.count
        }
        return totalNum
    }
    
    
    var manualSelectionDescription: String {
        var description = ""
        if manuallySelectedCards.count == 0 {
            return ""
        } else {
            description += " The selected card(s) include "
            let endNum = min(5, manuallySelectedCards.count) // List maximum of 5 card answerlines
            for i in 0..<endNum {
                description += manuallySelectedCards[i].answer
                if i < endNum - 1 {
                    description += ", "
                } else {
                    if manuallySelectedCards.count > endNum {
                        description += "..."
                    } else {
                        description += "."
                    }
                }
            }
            return description
        }
    }
    
    var body: some View {
        NavigationStack {
            Form {
                Section("Card Selection") {
                    Picker("Card Selection Picker", selection: $useRandomCards) {
                        Text("Random")
                            .tag(true)
                        Text("Manual")
                            .tag(false)
                    }.labelsHidden()
                        .pickerStyle(.segmented)
                    
                    if useRandomCards {
                        // MARK: Random Options
                        VStack {
                            Text("Enter the number of cards you want to review.")
                            TextField("Number of Cards", value: $numberOfRandomCards, format: .number, prompt: Text("Enter a number"))
                                .keyboardType(.numberPad)
                                .focused($enteringCardNumber)
                                .padding()
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10)
                                        .stroke(Color.blue.gradient)
                                )
                                .onChange(of: numberOfRandomCards) { _, newValue in
                                    if newValue <= 0 {
                                        numberOfRandomCards = 1
                                    }
                                    if (newValue > numOfAvaliableRandomCards) && (numOfAvaliableRandomCards != 0) {
                                        numberOfRandomCards = numOfAvaliableRandomCards
                                    }
                                }
                        }
                        
                        VStack {
                            Text("Customize the card categories you will be reviewing. Please make sure that the total number of cards across the selected categories in greater than or equal to the number of cards to review.")
                            
                            CategorySelectionMenu(categories: $categories, labelView: defaultCategoryMenuLabel())
                            
                            Text("There are a total of \(numOfAvaliableRandomCards) cards with the selected categories.")
                            
                        }
                        
                    } else {
                        // MARK: Manual Options
                        VStack(alignment: .center) {
                            Text("**Select the cards you want to review.**")
                            Text("You have currently selected **\(manuallySelectedCards.count)** card(s) to review. \(manualSelectionDescription)")
                            
                            Button {
                                showingManualSelectionView = true
                            } label: {
                                Text("Edit Selection")
                                    .foregroundStyle(.white)
                                    .bold()
                                    .padding()
                                    .background(
                                        RoundedRectangle(cornerRadius: 10)
                                            .fill(Color.green.gradient)
                                    )
                            }
                            
                        }
                    }

                }
                
                // MARK: Practice Options
                Section("Practice Options") {
                    Toggle(isOn: $shuffleCards) {
                        Text("Shuffle cards when reviewing?")
                    }
                    Toggle(isOn: $sequentialClues) {
                        Text("Slowly reveal clues?")
                    }
                }
                
                    // MARK: Begin Button
                    Button {
                        // Perform any necessary checks
                        guard checkForInvalidSelectionNumber() == false else {
                            showingCardNumberAlert = true
                            return
                        }
                        cardsToReview = getReviewCards()
                        guard checkForMissingPersonalNotes() == false else {
                            showingMissingNotesAlert = true
                            return
                        }
                        practicing = true
                        
                    } label: {
                        Text("Start Practice")
                            .foregroundStyle(.white)
                            .font(.title)
                            .bold()
                            .padding()
                            .background(
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(Color.green.gradient)
                            )
                    }.buttonStyle(.borderless)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                
            }
            .navigationTitle("Review")
                .navigationBarTitleDisplayMode(.inline)
                .navigationDestination(isPresented: $practicing) {
                    PracticeView(reviewCards: cardsToReview, shuffle: shuffleCards, sequentialClues: sequentialClues)
                }
                .navigationDestination(isPresented: $showingManualSelectionView) {
                    GeometryReader { geometry in
                        ManageCardView(geometry: geometry, editingMode: false, manuallySelectedCards: Binding.init($manuallySelectedCards))
                    }
                }
                .toolbar {
                    ToolbarItem(placement: .keyboard) {
                        HStack {
                            Spacer()
                            Button("Done") {
                                enteringCardNumber = false
                            }
                        }
                    }
                }
                .alert("Not Enough Cards", isPresented: $showingCardNumberAlert) {
                    Button("Ok") { }
                } message: {
                    Text(cardNumberAlertMessage)
                }
                .alert("Missing Personal Notes", isPresented: $showingMissingNotesAlert) {
                    Button("Ok") { }
                } message: {
                    Text(missingNotesAlertMessage)
                }

        }
    }
    
    // MARK: checkForInvalidNum
    private func checkForInvalidSelectionNumber() -> Bool { // Returns false if valid
        if cards.count == 0 {
            cardNumberAlertMessage = "Please create some cards first!"
            return true
        }
        if useRandomCards {
            if numOfAvaliableRandomCards < numberOfRandomCards {
                cardNumberAlertMessage = "Please make sure that the number of cards is less than or equal the amount of cards you have subject to the category conditions."
                return true
            }
        } else {
            if manuallySelectedCards.count == 0 {
                cardNumberAlertMessage = "Please select at least 1 card."
                return true
            }
        }
        
        return false // If all checks pass (none of the if statements run and return true), then return false
    }
    
    // MARK: checkForMissingNotes
    private func checkForMissingPersonalNotes() -> Bool { // Returns true if any card to be reviewed doesn't have any personal notes
        var alertMessage = "Some cards don't have any personal notes. When reviewing, you review your personal notes for your cards. Please add personal notes to the following cards: "
        var missingNotesCards: [Card] = []
        for card in cardsToReview {
            if card.personalNotes.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                missingNotesCards.append(card)
            }
        }
        if missingNotesCards.count != 0 {
            for i in 0..<missingNotesCards.count {
                alertMessage += missingNotesCards[i].answer
                if i + 1 < missingNotesCards.count {
                    alertMessage += ", "
                } else {
                    alertMessage += "."
                }
            }
            missingNotesAlertMessage = alertMessage
            return true
        }
        
        return false
    }
    
    // MARK: getReviewCards
    private func getReviewCards() -> [Card] {
        var cardsToReview: [Card] = []
        if useRandomCards {
            let randomCardPool: [Card] = {
                var pool: [Card] = []
                for category in categories {
                    let categoryCards = cards.filter { cardToFilter in
                        cardToFilter.category.id == category.id
                    }
                    pool.append(contentsOf: categoryCards)
                }
                return pool
            }()
            var usedIndices: [Int] = [] // Prevent card repeats
            while cardsToReview.count < numberOfRandomCards {
                let randIndex = Int.random(in: 0..<randomCardPool.count)
                if usedIndices.contains(randIndex) {
                    continue
                } else {
                    cardsToReview.append(randomCardPool[randIndex])
                    usedIndices.append(randIndex)
                }
            }
            
        } else {
            cardsToReview = manuallySelectedCards
        }
        return cardsToReview
    }
    
    
    
    
    
}
