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
    @State private var tickCount = 0
    @State private var buzzed = false
    @State private var gotIt: Bool? = nil
    @State private var generatingTossup = false
    @State private var generationError = false
    @State private var sessionComplete = false
    @State private var sessionPoints = 0
    @State private var numberCorrect = 0
    @State private var numberIncorrect = 0
    @State private var buzzAnswer = ""
    @State private var buzzCountdownTicks = 0
    @State private var countdownFinished = false
    @FocusState private var answerFieldFocused: Bool
    
    var questionProgressPercent: Double {
        return Double(revealedCount) / Double(words.count)
    }

    var countdownSecondsRemaining: Int {
        max(0, Int(ceil(BuzzInView.countdownDuration - Double(buzzCountdownTicks) * BuzzInView.tickLength)))
    }

    var countdownProgress: Double {
        min(1.0, Double(buzzCountdownTicks) * BuzzInView.tickLength / BuzzInView.countdownDuration)
    }

    var suggestedGotIt: Bool? {
        guard countdownFinished else { return nil }
        let typed = buzzAnswer.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !typed.isEmpty else { return false }
        return fuzzyMatch(typed: typed, correct: reviewCards[currentIndex].answer)
    }

    static let tickLength = 0.05 // This is how many seconds per tick
    static let countdownDuration = 5.0
    let desiredWPM = 300 // TODO: Make customizable
    var timePerWord: Double {
        return 60.0/Double(desiredWPM) // In seconds
    }
    let progressTimer = Timer.publish(every: tickLength, on: .main, in: .common).autoconnect()

    let groqApiKey = Bundle.main.infoDictionary!["GroqApiKey"] as! String
    let tossupModel = "openai/gpt-oss-safeguard-20b"

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
                        Text("\(sessionPoints) pts")
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
                                Text((buzzed && countdownFinished ? words : Array(words.prefix(revealedCount))).joined(separator: " "))
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

                    // MARK: Countdown bar
                    if !generatingTossup {
                        if !buzzed && revealedCount < words.count {
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
                        } else if !countdownFinished {
                            VStack(spacing: 4) {
                                Text("\(countdownSecondsRemaining)")
                                    .font(.title2).bold()
                                    .foregroundStyle(countdownSecondsRemaining <= 2 ? .red : .primary)
                                    .contentTransition(.numericText())
                                GeometryReader { barGeometry in
                                    ZStack(alignment: .leading) {
                                        RoundedRectangle(cornerRadius: 4).fill(Color.gray.opacity(0.25))
                                        RoundedRectangle(cornerRadius: 4)
                                            .fill(countdownSecondsRemaining <= 2 ? Color.red.opacity(0.7) : Color.orange.opacity(0.6))
                                            .frame(width: barGeometry.size.width * countdownProgress)
                                            .animation(.linear(duration: BuzzInView.tickLength), value: countdownProgress)
                                    }
                                }
                                .frame(height: 8)
                            }
                            .frame(width: geometry.size.width * 0.85)
                        }
                    }

                    // MARK: Answer input
                    if buzzed {
                        TextField("Type your answer...", text: $buzzAnswer)
                            .textFieldStyle(.roundedBorder)
                            .focused($answerFieldFocused)
                            .disabled(countdownFinished)
                            .submitLabel(.done)
                            .onSubmit { countdownFinished = true; answerFieldFocused = false }
                            .frame(width: geometry.size.width * 0.75)
                    }

                    // MARK: Answer reveal
                    if countdownFinished {
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
                    } else if countdownFinished {
                        if gotIt == nil {
                            HStack(spacing: 12) {
                                Button("Got it ✓") {
                                    gotIt = true
                                    sessionPoints += 10
                                    numberCorrect += 1
                                    recordCardAttempt(correct: true)
                                    if spacedRepetition {
                                        scheduleReview(card: reviewCards[currentIndex], rating: .good, modelContext: modelContext)
                                    }
                                }
                                .foregroundStyle(.white)
                                .bold()
                                .padding()
                                .background(
                                    RoundedRectangle(cornerRadius: 10)
                                        .fill(Color.green.gradient)
                                )
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10)
                                        .stroke(Color.blue, lineWidth: suggestedGotIt == true ? 4 : 0)
                                )

                                Button("Didn't get it") {
                                    gotIt = false
                                    numberIncorrect += 1
                                    let interrupted = revealedCount < words.count
                                    sessionPoints += interrupted ? -5 : 0
                                    recordCardAttempt(correct: false)
                                    if spacedRepetition {
                                        scheduleReview(card: reviewCards[currentIndex], rating: .again, modelContext: modelContext)
                                    }
                                }
                                .foregroundStyle(.white)
                                .bold()
                                .padding()
                                .background(
                                    RoundedRectangle(cornerRadius: 10)
                                        .fill(Color.red.gradient)
                                )
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10)
                                        .stroke(Color.blue, lineWidth: suggestedGotIt == false ? 4 : 0)
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
                    } else if buzzed {
                        Button("Submit") {
                            countdownFinished = true
                            answerFieldFocused = false
                        }
                        .foregroundStyle(.white)
                        .bold()
                        .padding()
                        .background(
                            RoundedRectangle(cornerRadius: 10)
                                .fill(Color.blue.gradient)
                        )
                    } else {
                        Button("BUZZ!") {
                            withAnimation { buzzed = true }
                            buzzCountdownTicks = 0
                            countdownFinished = false
                            buzzAnswer = ""
                            answerFieldFocused = true
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
                        Text("\(sessionPoints) points — \(numberCorrect) correct, \(numberIncorrect) incorrect")
                            .foregroundStyle(.secondary)
                        Text("Save these session stats?")
                            .font(.subheadline)
                        HStack(spacing: 12) {
                            Button("Discard") {
                                dismiss()
                            }
                            .foregroundStyle(.white)
                            .bold()
                            .padding()
                            .background(
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(Color.gray.gradient)
                            )

                            Button("Save") {
                                let stat = BuzzGameStat(points: sessionPoints, numberCorrect: numberCorrect, numberIncorrect: numberIncorrect)
                                modelContext.insert(stat)
                                do {
                                    try modelContext.save()
                                } catch {
                                    print("Error saving BuzzGameStat")
                                }
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
            guard !generatingTossup, !words.isEmpty else { return }
            if !countdownFinished, buzzed || revealedCount == words.count {
                buzzCountdownTicks += 1
                if Double(buzzCountdownTicks) * BuzzInView.tickLength >= BuzzInView.countdownDuration {
                    countdownFinished = true
                    answerFieldFocused = false
                }
            } else if !buzzed, revealedCount < words.count {
                tickCount += 1
                if Double(tickCount) * BuzzInView.tickLength >= timePerWord {
                    revealedCount += 1
                    tickCount = 0
                }
            }
        }
    }

    // MARK: loadCurrentCard
    private func loadCurrentCard() async {
        buzzed = false
        gotIt = nil
        revealedCount = 0
        tickCount = 0
        generationError = false
        words = []
        buzzAnswer = ""
        buzzCountdownTicks = 0
        countdownFinished = false
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
                messages: [Message(role: .user, content: try await buildTossupPrompt(for: card))],
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

    // MARK: recordCardAttempt
    private func recordCardAttempt(correct: Bool) {
        let card = reviewCards[currentIndex]
        let buzzPositionPercent = words.isEmpty ? 100.0 : Double(revealedCount) / Double(words.count) * 100.0
        let allStats = (try? modelContext.fetch(FetchDescriptor<CardStat>())) ?? []
        let stat = allStats.first { $0.answer == card.answer && $0.category == card.category } ?? {
            let new = CardStat(answer: card.answer, category: card.category)
            modelContext.insert(new)
            return new
        }()
        stat.timesSeen += 1
        if correct { stat.timesCorrect += 1 }
        stat.totalBuzzPositionPercent += buzzPositionPercent
        do {
            try modelContext.save()
        } catch {
            print("Error saving CardStat")
        }
    }

    // MARK: buildTossupPrompt
    private func buildTossupPrompt(for card: Card) async throws -> String {
        let clues = card.personalNotes
            .split(whereSeparator: \.isNewline)
            .map(String.init)
            .filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
            .reversed()
        let clueList = clues.map { "• \($0)" }.joined(separator: "\n")
        
        // Get the subject type
        let subjectPrompt = """
            You are classifying quizbowl answers.

            Given an answer, determine the most appropriate subject type for referring to that answer in a quizbowl tossup.

            Choose EXACTLY ONE subject type from the following list:

            person
            author
            poet
            playwright
            philosopher
            psychologist
            scientist
            physicist
            chemist
            biologist
            mathematician
            astronomer
            economist
            composer
            musician
            artist
            architect
            politician
            monarch
            religious figure
            mythological figure

            novel
            short story
            play
            poem
            epic poem
            essay
            speech
            book
            literary work
            character

            painting
            sculpture
            building
            architectural style
            opera
            ballet
            symphony
            concerto
            song
            musical work

            battle
            war
            treaty
            rebellion
            revolution
            historical event
            empire
            dynasty
            civilization

            country
            city
            river
            mountain
            island
            sea
            ocean
            desert
            continent
            region

            planet
            moon
            star
            galaxy
            constellation
            chemical element
            compound
            particle
            organism
            species
            disease
            anatomical structure

            theory
            law
            principle
            equation
            phenomenon
            theorem
            proof
            algorithm
            function

            religion
            religious text
            school of thought
            concept
            organization
            company
            language
            film
            television series
            artifact
            document
            award
            festival

            Guidelines:
            - Choose the most natural subject type for beginning every sentence of a tossup (e.g. "This novel...", "This composer...", "This battle...").
            - Prefer commonly used categories over overly specific ones
            - If multiple types could apply, choose the one that would sound most natural repeated throughout a quizbowl tossup
            - Do not invent new subject types, unless you are very confident that the subject is not described by any word in the list

            The answer is: \(card.answer)
            The category is: \(card.categoryString)
        """
        let groqClient = GroqClient(apiKey: groqApiKey)
        let request = ChatCompletionRequest(
            model: tossupModel,
            messages: [Message(role: .user, content: subjectPrompt)],
            stream: false
        )
        let response = try await groqClient.createChatCompletion(request)
        let subjectType = response.choices[0].message.content.trimmingCharacters(in: .whitespacesAndNewlines)
        print(subjectType)
        
        return """
        You are stitching bullet-point clues into a quizbowl tossup paragraph. The subject category is \(card.category.rawValue). The intended answer is \(card.answer). The subject type is \(subjectType). YOU MUST NOT MENTION THE ANSWER ANYWHERE IN YOUR RESPONSE!

        The clues are as follows (ordered hard to easy):
        \(clueList)

        Based on the clues, write a single flowing paragraph that:
        - Uses "This [subject type]" at the start of each sentence
        - Incorporates each clue as natural prose WITHOUT changing the meaning; If a clue is phrased about another entity (such as an author, composer, discoverer, character, or location), rewrite it so it becomes a fact about the answer while preserving its meaning.
        - Uses every clue exactly once, in order from hardest to easiest and does not add, remove, or alter factual information.
        - Does NOT state or hint at the answer
        - Produces approximately one sentence per clue without any additional details
        - Contains no bullet points, numbered lists, or markdown
        - The final sentence/clue should begin with the phrase "For 10 points, name this [answer_type] ... [rest of clue]"
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

    // MARK: Fuzzy match funcs
    private func fuzzyMatch(typed: String, correct: String) -> Bool {
        let t = normalize(typed)
        let c = normalize(correct)
        guard !t.isEmpty else { return false }
        if c.contains(t) || t.contains(c) { return true }
        let dist = levenshtein(t, c)
        return dist <= max(1, c.count / 4)
    }

    private func normalize(_ s: String) -> String {
        var r = s.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)
        for article in ["the ", "a ", "an "] {
            if r.hasPrefix(article) { r = String(r.dropFirst(article.count)) }
        }
        r = r.filter { $0.isLetter || $0.isNumber || $0.isWhitespace }
        return r.trimmingCharacters(in: .whitespaces)
    }

    private func levenshtein(_ a: String, _ b: String) -> Int {
        let a = Array(a), b = Array(b)
        if a.isEmpty { return b.count }
        if b.isEmpty { return a.count }
        var dp = Array(0...b.count)
        for i in 1...a.count {
            let prev = dp
            dp[0] = i
            for j in 1...b.count {
                dp[j] = a[i-1] == b[j-1] ? prev[j-1] : 1 + min(prev[j-1], prev[j], dp[j-1])
            }
        }
        return dp[b.count]
    }

}
