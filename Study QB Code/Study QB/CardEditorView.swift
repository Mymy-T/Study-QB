//
//  CardEditorView.swift
//  Study QB
//
//  Created by Milo on 11/5/25.
//

import SwiftData
import SwiftUI

struct CardEditorView: View {
    
    @Environment(\.dismiss) var dismiss
    @Environment(\.fontResolutionContext) var fontResolutionContext
    @Environment(\.modelContext) var modelContext
    @Environment(\.locale) var locale
    
    struct InitialState {
        var dateReviewed: Date
        var answer: String
        var details: AttributedString
        var category: Category?
        var personalNotes: String
        var color: Color
    }
    
    let geometry: GeometryProxy
    let existingCard: Card? // If this isn't nil, the user is editing the card; otherwise, the user is creating a new card
    @State private var dateReviewed = Date.now
    @State private var answer = ""
    @State private var details = AttributedString()
    @State private var attributedTextSelection = AttributedTextSelection()
    @State private var category: Category? = .lit
    @State private var personalNotes = ""
    @State private var color: Color = .red.opacity(0.4)
    
    @FocusState private var editingDetails: Bool
    @FocusState private var editingPersonalNotes: Bool
    
    @State private var refresh = false // Only modified to trigger view reload
    
    var dateString: String {
        let dateFormatter = DateFormatter()
        dateFormatter.locale = locale
        dateFormatter.dateStyle = .medium
        dateFormatter.timeStyle = .short
        return dateFormatter.string(from: dateReviewed)
    }
    
    var unsavedChangesPresent: Bool {
        return !((initialState.dateReviewed == dateReviewed) && (initialState.answer == answer) && (initialState.details == details) && (initialState.category == category) && (initialState.personalNotes == personalNotes) && (initialState.color == color))
        
    }
    
    var navigationTitle: String {
        return existingCard != nil ? (!unsavedChangesPresent ? "Edit/View Card" : "Edit/View Card (unsaved changes)") : "Create Card"
    }
    
    @State private var initialState = InitialState(dateReviewed: Date.now, answer: "", details: AttributedString(), personalNotes: "", color: .red.opacity(0.4))
    
    
    let colors: [Color] = [.red, .orange, .yellow, .green, .teal, .blue, .indigo, .purple, .pink, .brown]
    
    var body: some View {
        Form {
            Section("Answer Line") {
                TextField("", text: $answer, prompt: Text("Enter the answerline of the question"))
            }
            Section("Details") {
                TextEditor(text: $details, selection: $attributedTextSelection)
                    .focused($editingDetails)
                    .frame(height: geometry.size.height/2)
            }
            
            Section("Category") {
                Picker("Select a category", selection: $category) {
                    ForEach(Category.allCases) { category in
                        Text(category.rawValue)
                            .tag(category)
                    }
                }
            }
            Section("Personal Notes") {
                ZStack {
                    TextEditor(text: $personalNotes)
                        .focused($editingPersonalNotes)
                        .frame(height: geometry.size.height / 4)
                    Text("Add any personal notes here.")
                        .opacity(personalNotes.trimmingCharacters(in: .whitespacesAndNewlines) == "" ? 0.3 : 0)
                }
            }
            
            Section("Date Reviewed") {
                VStack {
                    Button {
                        dateReviewed = Date.now
                    } label: {
                        Text("I Reviewed It!")
                            .foregroundStyle(.white)
                            .padding()
                            .background(
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(Color.green.gradient)
                            )
                    }.buttonStyle(.borderless)
                    
                    Text("This card was last reviewed on **\(dateString)**.")
                }
            }
            
            Section("Select Color") {
                HStack {
                    ForEach(colors.indices, id: \.self) { i in
                        Button {
                            color = colors[i].opacity(0.4)
                        } label: {
                            Circle()
                                .fill(colors[i].opacity(0.4))
                                .overlay(
                                    Circle()
                                        .fill(Color.clear)
                                        .stroke(Color.blue, lineWidth: color == colors[i].opacity(0.4) ? 5 : 0)
                                )
                        }.buttonStyle(.borderless)
                    }
                }
            }
            
        }.scrollContentBackground(.hidden)
            .background(
                Rectangle()
                    .fill(color)
            )
            .navigationTitle(navigationTitle)
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                if let existingCard = existingCard {
                    answer = existingCard.answer
                    details = existingCard.details
                    category = existingCard.category
                    personalNotes = existingCard.personalNotes
                    dateReviewed = existingCard.dateReviewed
                    color = convertRGBAToColor(existingCard.rgba)
                }
                initialState = InitialState(dateReviewed: dateReviewed, answer: answer, details: details, category: category, personalNotes: personalNotes, color: color)
                refresh.toggle()
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        save()
                        dismiss()
                    } label: {
                        Image(systemName: "checkmark")
                    }
                    .tint(.green)
                }
                ToolbarItem(placement: .keyboard) {
                    HStack {
                        Spacer()
                        Button("Done") {
                            if editingDetails {
                                editingDetails = false
                            }
                            if editingPersonalNotes {
                                editingPersonalNotes = false
                            }
                        }.padding(5)
                            .tint(.green)
                    }
                }
            }
//            .toolbarBackground(color, for: .navigationBar)
//            .toolbarBackground(color, for: .tabBar)
//            .toolbarBackgroundVisibility(.visible, for: .navigationBar)
//            .toolbarBackgroundVisibility(.visible, for: .tabBar)
    }
    
    private func save() {
        if let cardToEdit = existingCard {
            cardToEdit.answer = answer
            cardToEdit.details = details
            cardToEdit.category = category
            cardToEdit.personalNotes = personalNotes
            cardToEdit.dateReviewed = dateReviewed
            cardToEdit.rgba = convertColorToRGBA(color)
        } else {
            let newCard = Card(dateReviewed: dateReviewed, answer: answer, details: details, category: category, personalNotes: personalNotes, rgba: convertColorToRGBA(color))
            modelContext.insert(newCard)
        }
    }
    
}
