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
    
    let existingCard: Card? // If this isn't nil, the user is editing the card; otherwise, the user is creating a new card
    @State private var dateReviewed = Date.now
    @State private var answer = ""
    @State private var details = AttributedString()
    @State private var attributedTextSelection = AttributedTextSelection()
    @State private var category: Category?
    @State private var personalNotes = ""
    
    
    var body: some View {
        VStack {
            Form {
                Section("Answerline") {
                    TextField("", text: $answer, prompt: Text("Enter the answerline of the question"))
                }
                Section("Details") {
                    VStack {
                        HStack {
                            Button {
                                details.transformAttributes(in: &attributedTextSelection) { container in
                                    let currentFont = container.font ?? .default
                                    let resolved = currentFont.resolve(in: fontResolutionContext)
                                    container.font = currentFont.bold(!resolved.isBold)
                                }
                            } label: {
                                Image(systemName: "bold")
                            }
                        }
                        TextEditor(text: $details, selection: $attributedTextSelection)
                    }
                }
                Section("Category") {
                    // TODO
                }
                Section("Personal Notes") {
                    TextEditor(text: $personalNotes)
                }
            }.navigationTitle(existingCard != nil ? "Edit/View Card" : "Create Card")
                .navigationBarTitleDisplayMode(.inline)
                .onAppear {
                    if let existingCard = existingCard {
                        answer = existingCard.answer
                        details = existingCard.details
                        category = existingCard.category
                        personalNotes = existingCard.personalNotes
                        dateReviewed = existingCard.dateReviewed
                    }
                }
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {
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
                            dismiss()
                        } label: {
                            Image(systemName: "checkmark")
                        }
                        .tint(.green)
                    }
                }
        }
    }
}
