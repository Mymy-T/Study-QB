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
    
    @State private var showingCardNumberAlert = false
    @State private var cardNumberAlertMessage = ""
    
    @State private var practicing = false
    @State private var cardsToReview: [Card] = []
    
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
                        }
                        
                        VStack {
                            Text("Customize the card categories you will be reviewing. Please make sure that the total number of cards across the selected categories in greater than or equal to the number of cards to review.")
                            
                            Menu {
                                ForEach(Category.allCases) { category in
                                    // CategorySelectionButton defined in AnalyzeView
                                    CategorySelectionButton(buttonCategory: category, categories: $categories)
                                }
                                Divider()
                                Button("Select All") {
                                    categories.removeAll()
                                    for category in Category.allCases {
                                        categories.append(category)
                                    }
                                }
                                Button("Remove All") {
                                    categories.removeAll()
                                }
                            } label: {
                                Label("Select Categories", systemImage: "chevron.up.chevron.down")
                                    .foregroundStyle(.white)
                                    .padding()
                                    .background(
                                        RoundedRectangle(cornerRadius: 10)
                                            .fill(Color.green.gradient)
                                    )
                            }
                            .menuActionDismissBehavior(.disabled)
                            .onAppear {
                                for category in Category.allCases {
                                    categories.append(category)
                                }
                            }
                            
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
                            }.navigationDestination(isPresented: $showingManualSelectionView) {
                                GeometryReader { geometry in
                                    ManageCardView(geometry: geometry, editingMode: false, manuallySelectedCards: Binding.init($manuallySelectedCards))
                                }
                            }
                            
                        }
                    }

                }
                
                // MARK: Practice Options
                Section("Practice Options") {
                    Toggle(isOn: $shuffleCards) {
                        Text("Shuffle cards when reviewing?")
                    }
                }
                
                Section("Begin Practice") {
                    // TODO: Make the button bigger or center it
                    Button {
                        // MARK: Begin Button
                        // Perform any necessary checks
                        guard checkForInvalidSelectionNumber() == false else {
                            showingCardNumberAlert = true
                            return
                        }
                        cardsToReview = getReviewCards()
                        practicing = true
                        
                        
                    } label: {
                        Text("Start Practice")
                            .foregroundStyle(.white)
                            .bold()
                            .padding()
                            .background(
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(Color.green.gradient)
                            )
                    }.buttonStyle(.borderless)
                }
                
                Section("Save/Load Practice Configuration") {
                    // TODO
                }
                
            }
            .navigationTitle("Review")
                .navigationBarTitleDisplayMode(.inline)
                .navigationDestination(isPresented: $practicing) {
                    PracticeView(reviewCards: cardsToReview, shuffle: shuffleCards)
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

        }
    }
    
    private func checkForInvalidSelectionNumber() -> Bool { // Returns false if valid
        if cards.count == 0 {
            cardNumberAlertMessage = "Please create some cards first!"
            return true
        }
        if useRandomCards {
            var totalNum = 0
            for category in categories {
                let categoryCards = cards.filter { cardToFilter in
                    return cardToFilter.category == category
                }
                totalNum += categoryCards.count
            }
            if totalNum < numberOfRandomCards {
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
    
    
    private func getReviewCards() -> [Card] {
        var cardsToReview: [Card] = []
        if useRandomCards {
            let randomCardPool: [Card] = {
                var pool: [Card] = []
                for category in categories {
                    let categoryCards = cards.filter { cardToFilter in
                        cardToFilter.category == category
                    }
                    pool.append(contentsOf: categoryCards)
                }
                return pool
            }()
            var usedIndices: [Int] = [] // Prevent card repeats
            while usedIndices.count < numberOfRandomCards {
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
