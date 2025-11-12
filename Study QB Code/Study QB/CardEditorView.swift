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
        
//        mutating func reinit(dateReviewed: Date, answer: String, details: AttributedString, category: Category?, personalNotes: String) {
//            self.dateReviewed = dateReviewed
//            self.answer = answer
//            self.details = details
//            self.category = category
//            self.personalNotes = personalNotes
//        }
    }
    
    let existingCard: Card? // If this isn't nil, the user is editing the card; otherwise, the user is creating a new card
    @State private var dateReviewed = Date.now
    @State private var answer = ""
    @State private var details = AttributedString()
//    @State private var bold = false
//    @State private var italic = false
    @State private var attributedTextSelection = AttributedTextSelection()
    @State private var category: Category? = .lit
    @State private var personalNotes = ""
    
    @FocusState private var editingDetails: Bool
    @FocusState private var editingPersonalNotes: Bool
    
    @State private var refresh = false // Only modified to trigger view reload
    
    var dateString: String {
        let dateFormatter = DateFormatter()
        dateFormatter.locale = locale
//        dateFormatter.dateFormat = "MM/dd/yyyy"
//        return dateFormatter.string(from: dateReviewed)
        dateFormatter.dateStyle = .medium
        dateFormatter.timeStyle = .short
        return dateFormatter.string(from: dateReviewed)
    }
    
    @State private var initialState = InitialState(dateReviewed: Date.now, answer: "", details: AttributedString(), personalNotes: "")
    
    
    var unsavedChangesPresent: Bool {
        return !((initialState.dateReviewed == dateReviewed) && (initialState.answer == answer) && (initialState.details == details) && (initialState.category == category) && (initialState.personalNotes == personalNotes))
        
    }
    
    var body: some View {
        VStack {
            Form {
                Section("Answer Line") {
                    TextField("", text: $answer, prompt: Text("Enter the answerline of the question"))
                }
                Section("Details") {
                    VStack {
//                        HStack {
//                            Button {
//                                bold.toggle()
//                                details.transformAttributes(in: &attributedTextSelection) { container in
//                                    let currentFont = container.font ?? .default
//                                    let resolved = currentFont.resolve(in: fontResolutionContext)
//                                    container.font = currentFont.bold(!resolved.isBold)
//                                }
//                            } label: {
//                                Image(systemName: "bold")
//                                    .padding()
//                                    .background(
//                                        RoundedRectangle(cornerRadius: 10)
//                                            .foregroundStyle(.blue.opacity(bold ? 0.3 : 0))
//                                    )
//                            }
//                        }
                        TextEditor(text: $details, selection: $attributedTextSelection)
                            .focused($editingDetails)
                            .scrollDisabled(true)
                    }
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
                            .scrollDisabled(true)
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
                
            }.navigationTitle(existingCard != nil ? (unsavedChangesPresent ? "Edit/View Card (unsaved changes)" : "Edit/View Card") : "Create Card")
                .navigationBarTitleDisplayMode(.inline)
                .onAppear {
                    if let existingCard = existingCard {
                        answer = existingCard.answer
                        details = existingCard.details
                        category = existingCard.category
                        personalNotes = existingCard.personalNotes
                        dateReviewed = existingCard.dateReviewed
                    }
                    initialState = InitialState(dateReviewed: dateReviewed, answer: answer, details: details, category: category, personalNotes: personalNotes)
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
//                            Button("Bold", systemImage: "bold") {
//                                //bold.toggle()
//                                details.transformAttributes(in: &attributedTextSelection) { container in
//                                    let currentFont = container.font ?? .default
//                                    let resolved = currentFont.resolve(in: fontResolutionContext)
//                                    container.font = currentFont.bold(!resolved.isBold)
//                                }
//                            }.opacity(editingDetails ? 1 : 0)
//                                .padding(5)
//                                .background(
//                                    Rectangle()
//                                        .fill(.blue.opacity(bold ? 0.2 : 0))
//                                )
//                            Divider()
//                                .opacity(editingDetails ? 1 : 0)
//                            Button("Italic", systemImage: "italic") {
//                                //italic.toggle()
//                                details.transformAttributes(in: &attributedTextSelection) { container in
//                                    let currentFont = container.font ?? .default
//                                    let resolved = currentFont.resolve(in: fontResolutionContext)
//                                    container.font = currentFont.italic(!resolved.isItalic)
//                                }
//                            }.opacity(editingDetails ? 1 : 0)
//                                .padding(5)
//                                .background(
//                                    Rectangle()
//                                        .fill(.blue.opacity(italic ? 0.2 : 0))
//                                )
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
                }//.toolbarBackground(.gray, for: .keyboard)
        }
    }
    
    private func save() {
        if let cardToEdit = existingCard {
            cardToEdit.answer = answer
            cardToEdit.details = details
            cardToEdit.category = category
            cardToEdit.personalNotes = personalNotes
            cardToEdit.dateReviewed = dateReviewed
        } else {
            let newCard = Card(dateReviewed: dateReviewed, answer: answer, details: details, category: category, personalNotes: personalNotes)
            modelContext.insert(newCard)
        }
    }
    
}
