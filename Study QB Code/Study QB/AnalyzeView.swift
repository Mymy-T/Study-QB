//
//  AnalyzeView.swift
//  Study QB
//
//  Created by Milo on 10/23/25.
//

import FirebaseAILogic
import GroqSwift
import SwiftData
import SwiftUI
// TODO: Foundation Model
// TODO: Possibly regex for omitting extraneous answer info
struct AnalyzeView: View {
    
    // Following structs for representing json data returned from api
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
    
    
    @State private var answerQuery = ""
    @State private var difficulty: Difficulty = .hs
    @State private var categories: [Category] = [] // [] matches all categories
    @State private var responseText = AttributedString()
    @State private var prompt = ""
    @State private var awaitingResponse = false
    @State private var responseRecieved = false
    
    @State private var showEmptyQueryAlert = false
    
    @State private var analyzeHelpPresented = false
    
    let geometry: GeometryProxy
    
    
    let groqApiKey = Bundle.main.infoDictionary!["GroqApiKey"] as! String
    
    let model = FirebaseAI.firebaseAI(backend: .googleAI()).generativeModel(modelName: "gemini-2.5-flash-lite")
    
    var body: some View {
        NavigationStack {
            VStack {
                
                HStack {
                    
                    VStack {
                        
                        Text("Categories")
                        
                        // MARK: Categories Menu
                        Menu {
                            ForEach(Category.allCases) { category in
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
                            if categories.count == 0 {
                                for category in Category.allCases {
                                    categories.append(category)
                                }
                            }
                        }
                    }
                    
                    Spacer()
                    
                    VStack {
                        
                        Text("Difficulty")
                        
                        Menu {
                            ForEach(Difficulty.allCases) { difficulty in
                                DifficultySelectionButton(buttonDifficulty: difficulty, difficulty: $difficulty)
                            }
                        } label: {
                            ZStack {
                                
                                Label("Middle School", systemImage: "chevron.up.chevron.down")
                                    .padding()
                                    .opacity(0)
                                
                                Label(difficulty.rawValue, systemImage: "chevron.up.chevron.down")
                                    .foregroundStyle(.white)
                                    .padding()
                                    .background(
                                        RoundedRectangle(cornerRadius: 10)
                                            .fill(Color.red.gradient)
                                    )
                            }
                        }
                    }
                    
                    
                }.padding()
                
                TextField("", text: $answerQuery, prompt: Text("Enter query"))
                    .padding()
                    .background(
                        RoundedRectangle(cornerRadius: 10)
                            .fill(.gray.opacity(0.2))
                    )
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
                
                HStack {
                    
                    // MARK: Find Common Clues
                    Button {
                        
                        responseRecieved = false
                        
                        guard answerQuery != "" else {
                            showEmptyQueryAlert = true
                            return
                        }
                        
                        awaitingResponse = true
//                        Task {
//                            do {
//                                let data = await fetchData(query: answerQuery, difficulty: .hs, categories: categories)
//                                prompt = processFetchedData(data)
//                                let response = try await model.generateContent(prompt)
//                                responseText = processResponse(response.text ?? "")
//                                awaitingResponse = false
//                                responseRecieved = true
//                            } catch {
//                                responseText = "Error with generation"
//                                awaitingResponse = false
//                            }
//                        }
                        
                        Task {
                            do {
                                let groqClient = GroqClient(apiKey: groqApiKey)
                                let questionData = await fetchData(query: answerQuery, difficulty: .hs, categories: categories)
                                prompt = processFetchedData(questionData)
                                let request = ChatCompletionRequest(model: "groq/compound", messages: [Message(role: .user, content: prompt)], stream: false)
                                let response = try await groqClient.createChatCompletion(request)
                                
                                responseText = processResponse(response.choices[0].message.content)
                                awaitingResponse = false
                                responseRecieved = true
                                
                            } catch let error as GroqError {
                                responseText = "Sorry, something went wrong."
                                awaitingResponse = false
                                switch error {
                                    case .invalidRequest(let message):
                                        print("Invalid request: \(message)")
                                    case .authenticationFailed(let message):
                                        print("Auth error: \(message)")
                                        print("API Key: \(groqApiKey)")
                                    case .modelNotFound(let message):
                                    print("Model not found: \(message)")
                                    case .invalidResponse(let message):
                                        print("Invalid response: \(message)")
                                    case .invalidURL:
                                        print("Invalid URL")
                                default:
                                    print("Some other error: \(error.localizedDescription)")
                                    
                                    }
                            } catch {
                                responseText = "Sorry, something went wrong."
                                awaitingResponse = false
                                print("Unexpected error: \(error)")
                            }
                        }
                        
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
                    
                    Spacer()
                    
                    // MARK: Save Card
                    
                    NavigationLink {
                        CardEditorView(geometry: geometry, answer: answerQuery, details: responseText)
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
                }
                .sheet(isPresented: $analyzeHelpPresented) {
                    HelpView()
                }
            
        }
        
        
    }
    
    // MARK: fetchData
    private func fetchData(query: String, difficulty: Difficulty, categories: [Category]) async -> [Question] {
        
        let formattedQuery = query.replacingOccurrences(of: " ", with: "%20")
        
        // Preprocessing to obtain url
        let difficulties: String = {
            switch difficulty {
            case .ms:
                return "1"
            case .hs:
                return "2,3,4,5"
            case .col:
                return "6,7,8,9"
            }
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
        
        let urlString = "https://www.qbreader.org/api/query?queryString=\(formattedQuery)&searchType=answer&exactPhrase=true&difficulties=\(difficulties)&categories=\(categories)"
        
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
        var processedDataString = "Given these quizbowl questions, can you explain the most important clues between them that are worth remembering? The clues should be specific to the answer and should not be vague. Clues involving significant names, objects, or processes should be prioritized. Do not list clues that appear only two times or less in your response. Omit clues that appear less than three times in your response. The answer is \(answerQuery). Within the prompt, the questions will be delineated by the words END QUESTION. Order the clues so that those that appear most frequently are listed first. Use new lines (\n) to separate clues. Provide a description of each clue you found, in enough depth for someone new to the topic to understand. Do not explain why you chose each clue in the descriptions. Bold each clue. Do not bold the descriptions. Do not generate any extraneous information about the questions. The questions are as follows: "
        for i in data.indices {
            processedDataString += "This is question number \(i + 1). The answer is \(data[i].answer_sanitized). The question is as follows: \(data[i].question_sanitized) END QUESTION. "
        }
        
        return processedDataString
        
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
    
    
}

// MARK: CategorySelectionButton
struct CategorySelectionButton: View {
    let buttonCategory: Category
    //let showingCategoryColor: Bool
    @Binding var categories: [Category]
    
    var body: some View {
        Button {
            if categories.contains(buttonCategory) {
                categories.removeAll { category in
                    category == buttonCategory
                }
            } else {
                categories.append(buttonCategory)
            }
        } label: {
            //HStack {
                Image(systemName: categories.contains(buttonCategory) ? "checkmark.square" : "square")
                Text(buttonCategory.rawValue)
        }
    }
}



// MARK: DifficultySelectionButton
struct DifficultySelectionButton: View {
    let buttonDifficulty: Difficulty
    @Binding var difficulty: Difficulty
    
    var body: some View {
        Button {
            difficulty = buttonDifficulty
        } label: {
            HStack {
                Text(buttonDifficulty.rawValue)
                if difficulty == buttonDifficulty {
                    Image(systemName: "checkmark")
                }
            }
        }
    }
}
