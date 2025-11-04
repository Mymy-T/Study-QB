//
//  AnalyzeView.swift
//  Study QB
//
//  Created by Milo on 10/23/25.
//

import FirebaseAILogic
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
    
    enum  Difficulty {
        case ms, hs, col
    }
    
    enum Category: String {
        case lit = "Literature"
        case hist = "History"
        case sci = "Science"
        case art = "Fine Arts"
        case rel = "Religion"
        case myth = "Mythology"
        case phil = "Philosophy"
        case ss = "Social Science"
        case ce = "Current Events"
        case geo = "Geography"
    }
    
    @State private var answerQuery = ""
    @State private var difficulty: Difficulty = .hs
    @State private var categories: [Category] = [] // [] matches all categories
    @State private var responseText = AttributedString()
    @State private var prompt = ""
    @State private var awaitingResponse = false
    
    @State private var showEmptyQueryAlert = false
    
    var difficultyBounds: (Int, Int) {
        // (Low, High)
        switch difficulty {
        case .ms:
            return (1, 1)
        case .hs:
            return (2, 5)
        case .col:
            return (6, 9)
        }
    }
    
    
    let model = FirebaseAI.firebaseAI(backend: .googleAI()).generativeModel(modelName: "gemini-2.5-flash-lite")
    
    var body: some View {
        VStack {
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
                    ProgressView()
                }
                
            }
            
            Button {
                
                guard answerQuery != "" else {
                    showEmptyQueryAlert = true
                    return
                }
                
                awaitingResponse = true
                Task {
                    do {
                        let data = await fetchData(query: answerQuery, difficulty: .hs, categories: [])
                        prompt = processFetchedData(data)
                        let tokenResp = try await model.countTokens(prompt)
                        print("Tokens: \(tokenResp.totalTokens)")
//                        let responseStream = try model.generateContentStream(prompt)
//                        for try await responseChunk in responseStream {
//                            responseText += processResponse(responseChunk.text ?? " ")
//                            awaitingResponse = false
//                        }
                        let response = try await model.generateContent(prompt)
                        responseText = processResponse(response.text ?? "")
                        awaitingResponse = false
                    } catch {
                        responseText = "Error with generation"
                        awaitingResponse = false
                    }
                }
            } label: {
                Text("Find Common Clues")
                    .foregroundStyle(.white)
                    .bold()
                    .padding()
                    .background(
                        Capsule()
                            .fill(Color.blue.gradient)
                    )
            }
            .alert("Empty Query", isPresented: $showEmptyQueryAlert) {
                Button("Ok") { }
            } message: {
                Text("Please enter a query.")
            }
            
        }
        
        
    }
    
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
                return ""
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
            print("An error occured: \(error.localizedDescription)")
            return []
        }
        
        return finalData
        
    }
    
    private func processFetchedData(_ data: [Question]) -> String {
        // Converts the data into a string to feed into llm
        var processedDataString = "Given these quizbowl questions, can you find common clues between them? The answer is \(answerQuery). For the following questions, the easiest difficulty is \(difficultyBounds.0) and the hardest difficulty is \(difficultyBounds.1), where the difficulty increases with the number. Order the clues by those that appear more often at easier difficulties to those that appear more often in harder difficulties. Each question will be separated from the next by 'END QUESTION'. Use new lines (\n) to separate clues (do not use bullet points). Separate your response into easy, medium, and hard difficulties, and provide a brief description (1 to 2 sentences) of each clue you found. Bold each clue and difficulty, but not the description. Do not generate any extraneous information about the question number of difficulty number. END INTRO. "
        for i in data.indices {
            processedDataString += "This is question number \(i + 1). The difficulty is \(data[i].difficulty). The answer is \(data[i].answer_sanitized). The question is as follows: \(data[i].question_sanitized) END QUESTION. "
        }
        
        print(processedDataString)
        
        return processedDataString
        
    }
    
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
