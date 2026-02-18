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
    @FocusState var queryKeyboard: Bool
    @State private var difficulties: Set<Int> = [2, 3, 4, 5]
    @State private var difficulty: Difficulty = .hs
    @State private var categories: [Category] = [] // [] matches all categories
    @State private var responseText = AttributedString()
    @State private var prompt = ""
    @State private var awaitingResponse = false
    @State private var responseRecieved = false
    @State private var numQuestions = 0
    
    @State private var showEmptyQueryAlert = false
    
    @State private var analyzeHelpPresented = false
    
    let geometry: GeometryProxy
    
    
    let groqApiKey = Bundle.main.infoDictionary!["GroqApiKey"] as! String
    let models = ["llama-3.3-70b-versatile", "moonshotai/kimi-k2-instruct-0905", "moonshotai/kimi-k2-instruct", "openai/gpt-oss-120b", "openai/gpt-oss-20b", "openai/gpt-oss-safeguard-20b", "meta-llama/llama-4-maverick-17b-128e-instruct", "meta-llama/llama-4-scout-17b-16e-instruct"]
    @State private var modelIndex = 0
    var currentModelName: String {
        return models[modelIndex]
    }
    
    var body: some View {
        NavigationStack {
            VStack {
                
                HStack {
                    CategorySelectionMenu(categories: $categories, labelView: defaultCategoryMenuLabel())
                    Spacer()
                    DifficultySelectionMenu(difficulties: $difficulties)
                }.padding()
                
                TextField("", text: $answerQuery, prompt: Text("Enter query"))
                    .padding()
                    .background(
                        RoundedRectangle(cornerRadius: 10)
                            .fill(.gray.opacity(0.2))
                    )
                    .padding(.horizontal)
                    .onSubmit {
                        
                        guard answerQuery != "" else {
                            showEmptyQueryAlert = true
                            return
                        }
                        
                        Task {
                            await getLLMResponse()
                        }
                        
                    }
                    .focused($queryKeyboard)
                
                
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
                
                Text("\(numQuestions) quizbowl questions were analyzed using the \(currentModelName) model.")
                    .font(.caption)
                    .padding(.top)
                    .opacity(responseRecieved ? 1 : 0)
                
                
                HStack {
                    
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
                    
                    Spacer()
                    
                    // MARK: Find Common Clues
                    Button {
                        
                        guard answerQuery != "" else {
                            showEmptyQueryAlert = true
                            return
                        }
                        
                        Task {
                            await getLLMResponse()
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
                let questionData = await fetchData(query: answerQuery, difficulty: .hs, categories: categories)
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
                awaitingResponse = false
                responseRecieved = true
                
            } catch let error as GroqError {
                awaitingResponse = false
                switch error {
                case .maxTokensExceeded(let message):
                    responseText = AttributedString("Max tokens exceeded: \(message)")
                case .rateLimitExceeded(let message):
                    responseText = AttributedString("Rate limit exceeded, attempting to switch to another model: \n \(message)")
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
    
    
}
