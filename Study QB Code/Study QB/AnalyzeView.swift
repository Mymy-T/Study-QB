//
//  AnalyzeView.swift
//  Study QB
//
//  Created by Milo on 10/23/25.
//

import GroqSwift
import SwiftData
import SwiftUI
// TODO: Foundation Model

struct AnalyzeView: View {
    
    // Following structs for representing api json data returned from query
    struct Question: Codable {
        var question_sanitized: String
        var answer_sanitized: String
        var difficulty: Int
    }
    
    struct Questions: Codable {
        var questionArray: [Question]
    }
    
    struct Tossups: Codable {
        var tossups: Questions
    }
    
    
    @Environment(\.modelContext) var modelContext
    @Query var cards: [Card]


    @State private var answerQuery = ""
    @State private var answerCategory: Category? = nil
    @FocusState var queryKeyboard: Bool
    @State private var difficulties: Set<Int> = [2, 3, 4, 5]
    @State private var categories: [Category] = [] // [] matches all categories
    @State private var responseText = AttributedString()
    @State private var prompt = ""
    @State private var awaitingResponse = false
    @State private var responseRecieved = false
    @State private var numQuestions = 0
    
    @State private var showEmptyQueryAlert = false

    @State private var awaitingSuggestion = false
    @State private var showNoSuggestionAlert = false
    @State private var suggestedText: String = ""
    @State private var validSuggestionMade = false

    @State private var analyzeHelpPresented = false
    
    @State private var showingFreqList = false
    @State private var freqListItems: [FreqListItem] = []
    @State private var awaitingFreqList = false
    
    let geometry: GeometryProxy
    
    
    let groqApiKey = Bundle.main.infoDictionary!["GroqApiKey"] as! String
    let models = ["openai/gpt-oss-120b", "openai/gpt-oss-20b", "qwen/qwen3.8-27b"]
    @State private var modelIndex = 0
    
    @State private var displayModelName: String = ""
    
    var body: some View {
        NavigationStack {
            VStack {
                
                HStack {
                    CategorySelectionMenu(categories: $categories, labelView: defaultCategoryMenuLabel())
                    Spacer()
                    DifficultySelectionMenu(difficulties: $difficulties)
                }.padding()
                
                HStack {
                    TextField("", text: $answerQuery, prompt: Text("Enter query"))
                        .padding()
                        .background(
                            RoundedRectangle(cornerRadius: 10)
                                .fill(.gray.opacity(0.2))
                        )
                        .onSubmit {

                            analyzeClues()

                        }
                        .focused($queryKeyboard)

                    Button {
                        Task {
                            await suggestTerm()
                        }
                    } label: {
                        Group {
                            if awaitingSuggestion {
                                ProgressView()
                            } else {
                                Image(systemName: "sparkles")
                            }
                        }
                        .foregroundStyle(.white)
                        .frame(width: 44, height: 44)
                        .background(Circle().fill(Color.blue.gradient))
                    }
                    .disabled(awaitingSuggestion)
                    .alert("No Suggestions Found", isPresented: $showNoSuggestionAlert) {
                        Button("Ok") { }
                    } message: {
                        Text("You already have cards for the top terms in the selected categories.")
                    }
                }
                .padding(.horizontal)
                
                
                ZStack {
                    ScrollView {
                        Text(responseText)
                            .textSelection(.enabled)
                            .padding(.horizontal)
                    }.opacity(awaitingResponse ? 0 : 1)
                    
                    if awaitingResponse {
                        VStack {
                            ProgressView()
                            Text("This may take a moment...")
                        }
                    }
                    
                }
                
                Text("\(numQuestions) quizbowl questions were analyzed using the \(displayModelName) model.")
                    .font(.caption)
                    .padding(.top)
                    .opacity(responseRecieved ? 1 : 0)
                
                
                HStack {
                    
                    // MARK: Save Card
                    
                    NavigationLink {
                        CardEditorView(geometry: geometry, answer: answerQuery, details: responseText, category: answerCategory ?? .lit)
                    } label: {
                        Text("Save")
                            .foregroundStyle(.white)
                            .padding()
                            .background(
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(Color.green.gradient)
                            )
                            .opacity(responseRecieved ? 1.0 : 0.0)
                            .disabled(!responseRecieved)
                    }
                    
                    Spacer()
                    
                    // MARK: Find Common Clues
                    Button {
                        
                        analyzeClues()
                        
                    } label: {
                        Text("Find Common Clues")
                            .foregroundStyle(.white)
                            .bold()
                            .padding()
                            .background(
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(Color.blue.gradient)
                            )
                    }
                    .alert("Empty Query", isPresented: $showEmptyQueryAlert) {
                        Button("Ok") { }
                    } message: {
                        Text("Please enter a query.")
                    }
                    
                    
                }.padding()
                
                
            }.navigationTitle("Analyze")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button {
                            analyzeHelpPresented = true
                        } label: {
                            Image(systemName: "questionmark.circle")
                        }
                    }
                    ToolbarItem(placement: .topBarTrailing) {
                        Menu {
                            ForEach(Category.allCases) { cat in
                                Button(cat.id) {
                                    showingFreqList = true
                                    awaitingFreqList = true
                                    Task {
                                        freqListItems = await fetchFrequencyList(category: cat, difficulties: difficulties)
                                        awaitingFreqList = false
                                    }
                                }
                            }
                        } label: {
                            Text("Freq Lists")
                        }
                    }
                    ToolbarItem(placement: .keyboard) {
                        HStack {
                            Spacer()
                            Button("Cancel") {
                                queryKeyboard = false
                            }
                        }
                    }
                }
                .sheet(isPresented: $analyzeHelpPresented) {
                    HelpView()
                }
                .sheet(isPresented: $showingFreqList) {
                    GeometryReader { geometry in
                        FreqListView(awaitingFreqList: $awaitingFreqList, geometry: geometry, freqListItems: freqListItems, cards: cards)
                    }
                }
            
        }
        
        
    }
    
    // MARK: fetchFrequencyList
    private func fetchFrequencyList(category: Category, difficulties: Set<Int>) async -> [FreqListItem] {
        
        let difficultiesString: String = {
            var string = ""
            var index = 0
            for difficulty in difficulties {
                string += String(difficulty)
                if index < difficulties.count - 1 {
                    string += ","
                }
                index += 1
            }
            return string
        }()
        
        let urlString = "https://www.qbreader.org/api/frequency-list?category=\(category.id)&difficulties=\(difficultiesString)&limit=100"
        
        let url = URL(string: urlString)
        
        guard let unwrappedUrl = url else {
            print("Error with url")
            return []
        }
        
        var finalData: [FreqListItem] = []
        
        do {
            let data = try await URLSession.shared.data(for: URLRequest(url: unwrappedUrl)) // data is tuple: (data, http code)
            let decoder = JSONDecoder()
            if let decodedData = try? decoder.decode(FrequencyList.self, from: data.0) {
                finalData = decodedData.frequencyList
            }
        } catch {
            print("An error occured while fetching data: \(error.localizedDescription)")
            return []
        }
        
        return finalData
    }
    
    // MARK: suggestTerm
    private func suggestTerm() async {
        awaitingSuggestion = true

        let candidateCategories = (categories.isEmpty ? Category.allCases : categories).shuffled()

        for category in candidateCategories {
            let items = await fetchFrequencyList(category: category, difficulties: difficulties)
            var suggestions = items.filter { !cardExists(forAnswer: $0.answer, in: cards) }
            suggestions = suggestions.sorted {
                $0.count > $1.count
            }
            let numSuggestions = suggestions.count
            let range = 0..<min(numSuggestions, 5) // One of the top 5, or less if there aren't that many viable elements
            let topSuggestions = suggestions[range]
            var suggestion: FreqListItem? = nil
            
            if topSuggestions.count > 1 {
                var uniqueSuggestion = false
                while !uniqueSuggestion {
                    suggestion = topSuggestions.randomElement()
                    uniqueSuggestion = suggestion!.answer != suggestedText
                    suggestedText = suggestion!.answer
                }
            } else if topSuggestions.count == 1 {
                suggestion = topSuggestions.randomElement()
            }
            
            if suggestion != nil {
                answerQuery = suggestion!.answer
                answerCategory = category
                awaitingSuggestion = false
                validSuggestionMade = true
                suggestedText = answerQuery
                return
            }
        }

        awaitingSuggestion = false
        showNoSuggestionAlert = true
    }
    
    // MARK: Lookup Cached Query
    private func lookupCachedQuery(for query: String) async -> Bool {
        
        guard let url = Bundle.main.url(forResource: "dataset", withExtension: "jsonl") else {
            responseText = "Loading cached data failed; please wait, trying direct generation..."
            return false
        }
        
        let decoder = JSONDecoder()
        
        let acceptedCategories = categories.map { category in
            return category.rawValue.lowercased().replacing(" ", with: "_")
        }
        
        do {
            for try await line in url.lines {
                guard let data = line.data(using: .utf8) else { continue }
                
                let cachedItem: CachedItem
                do {
                    cachedItem = try decoder.decode(CachedItem.self, from: data)
                } catch {
                    print("Error decoding cached item: \(error)")
                    continue
                }

                if fuzzyMatch(typed: query, correct: cachedItem.answer.replacing("_", with: " ")) {
                    
                    if acceptedCategories.contains(cachedItem.category) {
                        
                        responseText = processResponse(cachedItem.details)
                        numQuestions = cachedItem.num_questions_analyzed
                        displayModelName = cachedItem.model
                        
                        awaitingResponse = false
                        queryKeyboard = false
                        responseRecieved = true
                        
                        return true
                    }
                }
            }
        } catch {
            print("Error reading file: \(error)")
            return false
        }
        
        return false
        
    }
    

    // MARK: fetchData
    private func fetchData(query: String, difficulties: Set<Int>, categories: [Category]) async -> [Question] {
        
        let formattedQuery = query.replacingOccurrences(of: " ", with: "%20")
        
        // Preprocessing to obtain url
        let difficultiesString: String = {
            var string = ""
            var index = 0
            for difficulty in difficulties {
                string += String(difficulty)
                if index < difficulties.count - 1 {
                    string += ","
                }
                index += 1
            }
            return string
        }()
        
        
        
        let categories: String = {
            if categories.count == 0 {
                // Get all categories
                var string = ""
                for category in Category.allCases {
                    string += category.rawValue.replacingOccurrences(of: " ", with: "%20")
                    string += ","
                }
                return string
            } else {
                var string = ""
                for i in categories.indices {
                    string += categories[i].rawValue.replacingOccurrences(of: " ", with: "%20")
                    string += ","
                }
                return string
            }
        }()
        
        let urlString = "https://www.qbreader.org/api/query?queryString=\(formattedQuery)&searchType=answer&exactPhrase=true&difficulties=\(difficultiesString)&categories=\(categories)"
        
        let url = URL(string: urlString)
        
        guard let unwrappedUrl = url else {
            print("Error with url")
            return []
        }
        
        var finalData: [Question] = []
        
        do {
            let data = try await URLSession.shared.data(for: URLRequest(url: unwrappedUrl))
            let decoder = JSONDecoder()
            if let decodedData = try? decoder.decode(Tossups.self, from: data.0) {
                finalData = decodedData.tossups.questionArray
            }
        } catch {
            print("An error occured while fetching data: \(error.localizedDescription)")
            return []
        }
        
        return finalData
        
    }
    
    // MARK: processFetchedData
    private func processFetchedData(_ data: [Question]) -> String {
        // Converts the data into a string to feed into llm
        var processedDataString = "Given these quizbowl questions, explain the most important clues worth remembering. The clues should be specific. Clues that are frequent or involve significant names, objects, or processes should be prioritized. Try to list between 10 and 15 clues. The answer is \(answerQuery). Within the prompt, the questions will be delineated by the words END QUESTION. Order the clues so more common ones listed first. Use new lines (\n) to separate clues. Provide a description of each clue you found, in enough depth for someone new to the topic to understand. Do not explain why you chose the clues. Bold each clue. Do not bold the descriptions. Do not generate any extraneous information. The questions are as follows: "
        for i in data.indices {
            processedDataString += "This is question number \(i + 1). The answer is \(data[i].answer_sanitized). The question is as follows: \(data[i].question_sanitized) END QUESTION. "
        }
        
        return processedDataString
        
    }
    
    // MARK: getLLMResponse
    private func getLLMResponse() async {
        responseRecieved = false
        awaitingResponse = true
        queryKeyboard = false
        Task {
            do {
                let groqClient = GroqClient(apiKey: groqApiKey)
                let questionData = await fetchData(query: answerQuery, difficulties: difficulties, categories: categories)
                prompt = processFetchedData(questionData)
                numQuestions = questionData.count
                guard numQuestions != 0 else {
                    responseText = "Your query didn't match any quizbowl questions in Qbreader. Please make sure that there aren't any typos or spelling errors in your query."
                    awaitingResponse = false
                    return
                }
                let request = ChatCompletionRequest(model: models[modelIndex], messages: [Message(role: .user, content: prompt)], stream: false)
                let response = try await groqClient.createChatCompletion(request)
                
                responseText = processResponse(response.choices[0].message.content)
                displayModelName = models[modelIndex]
                awaitingResponse = false
                responseRecieved = true
                
            } catch let error as GroqError {
                awaitingResponse = false
                switch error {
                case .maxTokensExceeded(let message):
                    responseText = AttributedString("Max tokens exceeded: \(message)")
                case .rateLimitExceeded(let message):
                    responseText = AttributedString("Rate limit exceeded, attempting to switch to another model. Please try again. \n \n \n \n \n Error message: \(message)")
                    if modelIndex < models.count - 1 {
                        modelIndex += 1
                    } else {
                        modelIndex = 0
                    }
                case .invalidRequest(let message):
                    responseText = AttributedString("Invalid request: \(message)")
                case .authenticationFailed(let message):
                    responseText = AttributedString("Authentication failed: \(message)")
                case .modelNotFound(let message):
                    responseText = AttributedString("Model not found: \(message)")
                case .invalidResponse(let message):
                    responseText = AttributedString("Invalid response: \(message)")
                default:
                    print("Some other error: \(error.localizedDescription)")
                    responseText = "Sorry, something went wrong"
                }
            } catch {
                responseText = "Sorry, something went wrong."
                awaitingResponse = false
                print("Unexpected error: \(error)")
            }
        }
    }
    
    // MARK: processResponse
    private func processResponse(_ response: String) -> AttributedString {
        do {
            let processedResult = try AttributedString(markdown: response, options: .init(interpretedSyntax: .inlineOnlyPreservingWhitespace))
            
            return processedResult
        } catch {
            print("Error with processing response")
            return "Error with processing response"
        }
    }
    
    
    // MARK: analyzeClues
    private func analyzeClues() {
        guard answerQuery != "" else {
            showEmptyQueryAlert = true
            return
        }
        
        if !validSuggestionMade {
            answerCategory = nil
        }
        
        if categories.count == 1 && !validSuggestionMade {
            answerCategory = categories[0]
        }

        Task {
            let success = await lookupCachedQuery(for: answerQuery)

            if !success {
                await getLLMResponse()
            }
        }
        
        if answerQuery != suggestedText {
            validSuggestionMade = false
        }
        
    }
    
    
}
