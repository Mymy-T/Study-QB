//
//  BuzzInView.swift
//  Study QB
//
//  Created by Milo on 7/5/26.
//

import GroqSwift
import SwiftData
import SwiftFSRS
import SwiftUI

struct BuzzInView: View {

    let reviewCards: [Card]
    let spacedRepetition: Bool

    @Environment(\.modelContext) var modelContext
    @Environment(\.dismiss) var dismiss

    @State private var currentIndex = 0
    @State private var words: [String] = []
    @State private var revealedCount = 0
 //   @State private var wordTimerProgress: Double = 0.0
    @State private var tickCount = 0
    @State private var buzzed = false
    @State private var gotIt: Bool? = nil
    @State private var generatingTossup = false
    @State private var generationError = false
    @State private var sessionComplete = false
    
    var questionProgressPercent: Double {
        return Double(revealedCount) / Double(words.count)
    }

    static let tickLength = 0.05 // This is how many seconds per tick
    let desiredWPM = 300
    var timePerWord: Double {
        return 60.0/Double(desiredWPM) // In seconds
    }
    let progressTimer = Timer.publish(every: tickLength, on: .main, in: .common).autoconnect()

    let groqApiKey = Bundle.main.infoDictionary!["GroqApiKey"] as! String
    let tossupModel = "llama-3.1-8b-instant" // Model different from Analyze tab models to prevent reaching usage limit

    var markedForReview: Bool {
        reviewCards[currentIndex].markedForReview
    }

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                VStack(alignment: .center, spacing: 16) {

                    // MARK: Header
                    HStack {
                        Text("Card \(currentIndex + 1) of \(reviewCards.count)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Spacer()
                        Button {
                            reviewCards[currentIndex].markedForReview.toggle()
                        } label: {
                            VStack {
                                Image(systemName: markedForReview ? "star.fill" : "star")
                                    .resizable()
                                    .scaledToFit()
                                    .foregroundStyle(.yellow)
                                Text("Mark for Review")
                                    .font(.caption2)
                            }.frame(height: geometry.size.height / 14)
                        }
                    }.frame(width: geometry.size.width * 0.85)

                    // MARK: Tossup text area
                    ScrollViewReader { proxy in
                        ScrollView {
                            if generatingTossup {
                                VStack(spacing: 8) {
                                    ProgressView()
                                    Text("Generating tossup…")
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                                .frame(maxWidth: .infinity)
                                .padding()
                            } else {
                                Text(words.prefix(revealedCount).joined(separator: " "))
                                    .font(.body)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .padding()
                                    .id("buzzText")
                            }
                        }
                        .frame(width: geometry.size.width * 0.85, height: geometry.size.height * 0.42)
                        .background(
                            RoundedRectangle(cornerRadius: 10)
                                .fill(Color.blue.opacity(0.3).gradient)
                        )
                        .onChange(of: revealedCount) {
                            withAnimation { proxy.scrollTo("buzzText", anchor: .bottom) }
                        }
                    }

                    if generationError {
                        Text("Offline — using simplified format")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }

                    // MARK: Per-word timer bar
                    if !generatingTossup && !buzzed {
                        GeometryReader { barGeometry in
                            ZStack(alignment: .leading) {
                                RoundedRectangle(cornerRadius: 4)
                                    .fill(Color.gray.opacity(0.25))
                                RoundedRectangle(cornerRadius: 4)
                                    .fill(Color.blue.opacity(0.6))
                                    .frame(width: barGeometry.size.width * questionProgressPercent)
                                    .animation(.linear(duration: 0.05), value: questionProgressPercent)
                            }
                        }
                        .frame(width: geometry.size.width * 0.85, height: 8)
                    }

                    // MARK: Answer reveal
                    if buzzed {
                        Text(reviewCards[currentIndex].answer)
                            .font(.title2).bold()
                            .padding()
                            .background(
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(Color.green.opacity(0.3).gradient)
                            )
                            .transition(.opacity)
                    }

                    // MARK: Action buttons
                    if generatingTossup {
                        // No button while generating
                    } else if !buzzed {
                        Button("BUZZ!") {
                            withAnimation { buzzed = true }
                        }
                        .font(.title).bold()
                        .foregroundStyle(.white)
                        .padding(.horizontal, 40)
                        .padding(.vertical, 14)
                        .background(
                            RoundedRectangle(cornerRadius: 16)
                                .fill(Color.red.gradient)
                        )
                        .disabled(revealedCount == 0)
                        .opacity(revealedCount == 0 ? 0.4 : 1)
                    } else if gotIt == nil {
                        HStack(spacing: 12) {
                            Button("Got it ✓") {
                                gotIt = true
                                revealedCount = words.count
                                if spacedRepetition {
                                    scheduleReview(card: reviewCards[currentIndex], rating: .good)
                                }
                            }
                            .foregroundStyle(.white)
                            .bold()
                            .padding()
                            .background(
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(Color.green.gradient)
                            )

                            Button("Didn't get it") {
                                gotIt = false
                                revealedCount = words.count
                                if spacedRepetition {
                                    scheduleReview(card: reviewCards[currentIndex], rating: .again)
                                }
                            }
                            .foregroundStyle(.white)
                            .bold()
                            .padding()
                            .background(
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(Color.red.gradient)
                            )
                        }
                    } else {
                        Button("Next Card") {
                            advanceCard()
                        }
                        .foregroundStyle(.white)
                        .bold()
                        .padding()
                        .background(
                            RoundedRectangle(cornerRadius: 10)
                                .fill(Color.blue.gradient)
                        )
                    }

                    Spacer()
                }
                .frame(width: geometry.size.width * 0.9)
                .position(x: geometry.size.width * 0.5, y: geometry.size.height * 0.5)
                .opacity(sessionComplete ? 0 : 1)
                .allowsHitTesting(!sessionComplete)

                // MARK: Session complete overlay
                if sessionComplete {
                    VStack(spacing: 16) {
                        Text("Session Complete!")
                            .font(.largeTitle).bold()
                        Text("You reviewed \(reviewCards.count) card\(reviewCards.count == 1 ? "" : "s").")
                        Button("Done") {
                            dismiss()
                        }
                        .foregroundStyle(.white)
                        .bold()
                        .padding()
                        .background(
                            RoundedRectangle(cornerRadius: 10)
                                .fill(Color.green.gradient)
                        )
                    }
                    .position(x: geometry.size.width * 0.5, y: geometry.size.height * 0.5)
                }
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
        }
        .toolbarVisibility(.hidden, for: .tabBar)
        .task {
            await loadCurrentCard()
        }
        .onReceive(progressTimer) { _ in
            guard !buzzed, !generatingTossup, !words.isEmpty else { return }
            guard revealedCount < words.count else {
//                wordTimerProgress = 1.0
                return
            }
            tickCount += 1
            if Double(tickCount) * BuzzInView.tickLength >= timePerWord {
                revealedCount += 1
                tickCount = 0
            }
//            wordTimerProgress = min(1.0, wordTimerProgress + 0.05 / 0.4)
//            if wordTimerProgress >= 1.0 {
//                wordTimerProgress = 0.0
//                withAnimation(.easeIn(duration: 0.1)) {
//                    revealedCount += 1
//                }
//            }
        }
    }

    // MARK: loadCurrentCard
    private func loadCurrentCard() async {
        buzzed = false
        gotIt = nil
        revealedCount = 0
//        wordTimerProgress = 0.0
        tickCount = 0
        generationError = false
        words = []
        let card = reviewCards[currentIndex]

        if let cached = card.cachedTossup {
            if !cached.isEmpty {
                words = cached.split(separator: " ").map(String.init)
                return
            }
        }

        generatingTossup = true
        do {
            let groqClient = GroqClient(apiKey: groqApiKey)
            let request = ChatCompletionRequest(
                model: tossupModel,
                messages: [Message(role: .user, content: buildTossupPrompt(for: card))],
                stream: false
            )
            let response = try await groqClient.createChatCompletion(request)
            let tossup = response.choices[0].message.content.trimmingCharacters(in: .whitespacesAndNewlines)
            card.cachedTossup = tossup
            try? modelContext.save()
            words = tossup.split(separator: " ").map(String.init)
        } catch {
            let fallback = ruleBasedTossup(for: card)
            words = fallback.split(separator: " ").map(String.init)
            generationError = true // Informs user
        }
        generatingTossup = false
    }

    // MARK: advanceCard
    private func advanceCard() {
        if currentIndex + 1 < reviewCards.count {
            currentIndex += 1
            Task {
                await loadCurrentCard()
            }
        } else {
            sessionComplete = true
        }
    }

    // MARK: scheduleReview
    private func scheduleReview(card: Card, rating: Rating) {
        let scheduler = LongTermScheduler()
        let review = scheduler.schedule(
            card: card.fsrsCard!,
            algorithm: .v5,
            reviewRating: rating,
            reviewTime: Date()
        )
        card.fsrsCard = review.postReviewCard
        do {
            try modelContext.save()
        } catch {
            print("Error saving FSRS review card from BuzzInView")
        }
    }

    // MARK: buildTossupPrompt
    private func buildTossupPrompt(for card: Card) -> String {
        let clues = card.personalNotes
            .split(whereSeparator: \.isNewline)
            .map(String.init)
            .filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
            .reversed()
        let clueList = clues.map { "• \($0)" }.joined(separator: "\n")
        return """
        You are stitching bullet-point clues into a quizbowl tossup paragraph.
        Subject category: \(card.category.rawValue)
        Answer: \(card.answer) (YOU MUST NOT MENTION THE ANSWER ANYWHERE IN YOUR RESPONSE!)

        Clues (ordered hard to easy):
        \(clueList)

        Write a single flowing paragraph that:
        - Uses "This [subject type]" at the start of each sentence (infer the subject type from the category, e.g. "This composer", "This battle", "This novel", "This philosopher", "This country")
        - Incorporates each clue as natural prose WITHOUT changing the meaning
        - Orders sentences from most obscure to most commonly known
        - Does NOT state or hint at the answer
        - Contains no bullet points, numbered lists, or markdown
        - The number of sentences should be about the same as the number of clues. Do not add additional details
        - The final sentence should begin with the phrase "For 10 points"
        Output only the paragraph, nothing else.
        """
    }

    // MARK: ruleBasedTossup (offline fallback)
    private func ruleBasedTossup(for card: Card) -> String {
        let prefix: String = {
            switch card.category {
            case .lit:  return "This literary work"
            case .hist: return "This historical figure or event"
            case .sci:  return "This scientific concept"
            case .art:  return "This artist or artwork"
            case .rel:  return "This religious figure or concept"
            case .myth: return "This mythological figure"
            case .phil: return "This philosopher or philosophical concept"
            case .ss:   return "This concept"
            case .ce:   return "This"
            case .geo:  return "This place"
            }
        }()
        let clues = card.personalNotes
            .split(whereSeparator: \.isNewline)
            .map(String.init)
            .filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
            .reversed()
        return clues
            .map { "\(prefix) \($0.trimmingCharacters(in: .whitespaces))." }
            .joined(separator: " ")
    }

}
