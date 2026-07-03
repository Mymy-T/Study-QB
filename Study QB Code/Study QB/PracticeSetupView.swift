//
//  ReviewView.swift
//  Study QB
//
//  Created by Milo on 12/20/25.
//

import SwiftData
import SwiftUI

// TODO: Fix keyboard toolbar (to allow dismissal of keyboard)

struct Pseudocard: CardLike, Equatable {
    let answer: String
    let personalNotes: String
    let parentCard: Card
}

struct PracticeSetupView: View {
    
    enum CardSelectionMethod {
        case random, manual, automatic
    }
    
    @Query var cards: [Card]
    
    @State private var cardSelectionMethod = PracticeSetupView.CardSelectionMethod.automatic
    @State private var numberOfRandomCards = 5
    @FocusState private var enteringCardNumber: Bool
    @State private var categories: [Category] = []
    @State private var manuallySelectedCards: [Card] = []
    @State private var showingManualSelectionView = false
    @State private var onlyMarkedCards = false
    
    @State private var sequentialClues = false
    @State private var singleClueMode = false
    
    @State private var showingCardNumberAlert = false
    @State private var cardNumberAlertMessage = ""
    @State private var showingMissingNotesAlert = false
    @State private var missingNotesAlertMessage = ""
    
    @State private var practicing = false
    @State private var cardsToReview: [any CardLike] = []
    
    var numOfAvaliableRandomCards: Int {
        var totalNum = 0
        var cardPool: [Card] = []
        if onlyMarkedCards {
            cardPool = cards.filter { cardToFilter in
                return cardToFilter.markedForReview == true
            }
        } else {
            cardPool = cards
        }
        for category in categories {
            let categoryCards = cardPool.filter { cardToFilter in
                return cardToFilter.category == category
            }
            totalNum += categoryCards.count
        }
        return totalNum
    }
    
    var numScheduledCards: Int {
        var num = 0
        for card in cards {
            let today = Calendar.current.startOfDay(for: Date())
            let dueDate = Calendar.current.startOfDay(for: card.fsrsCard!.due)
            
            if dueDate <= today {
                num += 1
            }
        }
        return num
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
            GeometryReader { geometry in
                Form {
                    Section("Card Selection") {
                        Picker("Card Selection Picker", selection: $cardSelectionMethod) {
                            Text("Automatic")
                                .tag(CardSelectionMethod.automatic)
                            Text("Random")
                                .tag(CardSelectionMethod.random)
                            Text("Manual")
                                .tag(CardSelectionMethod.manual)
                        }.labelsHidden()
                            .pickerStyle(.segmented)
                        
                        if cardSelectionMethod == .random {
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
                                    .onChange(of: numOfAvaliableRandomCards) { _, newValue in
                                        if numberOfRandomCards > newValue {
                                            numberOfRandomCards = newValue
                                        }
                                    }
                            }
                            
                            VStack {
                                Text("Customize the card categories you will be reviewing. Please make sure that the total number of cards across the selected categories in greater than or equal to the number of cards to review.")
                                    .font(.caption)
                                
                                CategorySelectionMenu(categories: $categories, labelView: defaultCategoryMenuLabel())
                                    .padding(.vertical)
                                
                                Toggle(isOn: $onlyMarkedCards) {
                                    Text("Use review-marked cards only?")
                                        
                                }.padding()
                                .border(.orange)
                                .frame(width: geometry.size.width*0.8)
                                .padding(.bottom, 5)
                                
                                Text("There are a total of **\(numOfAvaliableRandomCards)** cards with the selected categories.")
                                    .font(.caption)
                                
                            }
                            
                            
                        } else if cardSelectionMethod == .manual {
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
                                
                            }.frame(width: geometry.size.width*0.9)
                        } else {
                            // MARK: Automatic
                            VStack {
                                Text("The cards to review will be automatically designated by the **spaced reptition** algorithm.")
                                Text("Unfortunately, single clue mode isn't avaliable for this setting.")
                                    .font(.caption)
                                    .padding()
                                Text("**\(numScheduledCards)** cards to review today.")
                                    .font(.caption)
                            }
                        }
                        
                    }
                    
                    // MARK: Practice Options
                    Section("Practice Options") {
                        Toggle(isOn: $sequentialClues) {
                            Text("Slowly reveal clues?")
                        }.onChange(of: sequentialClues) { oldValue, newValue in
                            if newValue == true {
                                singleClueMode = false
                            }
                        }
                        
                        if cardSelectionMethod != .automatic {
                            
                            Toggle(isOn: $singleClueMode) {
                                Text("Only 1 clue at a time?")
                            }
                            .onChange(of: singleClueMode) { oldValue, newValue in
                                if newValue == true {
                                    sequentialClues = false
                                }
                            }
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
                        
                        if singleClueMode {
                            cardsToReview = singleClueModePreprocessing(cards: cardsToReview as! [Card])
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
                    PracticeView(reviewCards: cardsToReview, sequentialClues: sequentialClues, singleClue: singleClueMode, spacedRepetition: (cardSelectionMethod == .automatic ? true : false))
                }
                .navigationDestination(isPresented: $showingManualSelectionView) {
                    GeometryReader { geometry in
                        ManageCardView(geometry: geometry, editingMode: false, manuallySelectedCards: Binding.init($manuallySelectedCards))
                    }
                }
                .onAppear {
                    numberOfRandomCards = cards.count
                }
                .safeAreaInset(edge: .bottom) {
                    if enteringCardNumber {
                        HStack {
                            Spacer()
                            Button("Done") { enteringCardNumber = false }
                                .padding()
                        }
                        .background(.bar)
                        .glassEffectIfAvaliable()
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
    }
    
    // MARK: checkForInvalidNum
    private func checkForInvalidSelectionNumber() -> Bool { // Returns false if valid
        if cards.count == 0 {
            cardNumberAlertMessage = "Please create some cards first!"
            return true
        }
        if cardSelectionMethod == .random {
            if numOfAvaliableRandomCards < numberOfRandomCards {
                cardNumberAlertMessage = "Please make sure that the number of cards is less than or equal the amount of cards you have subject to the category conditions."
                return true
            }
        } else if cardSelectionMethod == .manual {
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
        for card in (cardsToReview as! [Card]) {
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
        if cardSelectionMethod == .random {
            let randomCardPool: [Card] = {
                var categoryIndependentPool: [Card] = [] // Contains all cards regardless of category, depending on the value of onlyMarkedCards
                var pool: [Card] = []
                if onlyMarkedCards {
                    categoryIndependentPool = cards.filter { cardToFilter in
                        return cardToFilter.markedForReview == true
                    }
                } else {
                    categoryIndependentPool = cards
                }
                for category in categories {
                    let categoryCards = categoryIndependentPool.filter { cardToFilter in
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
            
        } else if cardSelectionMethod == .manual {
            cardsToReview = manuallySelectedCards.shuffled()
        } else {
            for card in cards {
                let today = Calendar.current.startOfDay(for: Date())
                let dueDate = Calendar.current.startOfDay(for: card.fsrsCard!.due)
                
                if dueDate <= today {
                    cardsToReview.append(card)
                }
            }
            cardsToReview = cardsToReview.shuffled()
        }
        return cardsToReview
    }
    
    // MARK: singleCluePreprocessing
    private func singleClueModePreprocessing(cards: [Card]) -> [Pseudocard] {
        var pseudocards: [Pseudocard] = [] // These aren't the true cards; each references a parent card to adjust date reviewed
        for card in cards {
            let clues = card.personalNotes.split(whereSeparator: \.isNewline).map{ String($0) }
            for clue in clues {
                // Values of dateReviewed, details, category, and rgba don't matter
                let pseudocard = Pseudocard(answer: card.answer, personalNotes: clue, parentCard: card)
                pseudocards.append(pseudocard)
            }
        }
        return pseudocards.shuffled()
    }
    
    
    
    
    
}
