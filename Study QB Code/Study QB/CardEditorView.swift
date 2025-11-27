//
//  CardEditorView.swift
//  Study QB
//
//  Created by Milo on 11/5/25.
//

import SwiftData
import SwiftUI
import UIKit

struct CardEditorView: View {
    
    @Environment(\.dismiss) var dismiss
    @Environment(\.fontResolutionContext) var fontResolutionContext
    @Environment(\.modelContext) var modelContext
    @Environment(\.locale) var locale
    @Environment(\.font) var environmentFont
    
    struct InitialState {
        var dateReviewed: Date
        var answer: String
        var details: AttributedString
        var category: Category
        var personalNotes: String
        var color: Color
    }
    
    let geometry: GeometryProxy
    let existingCard: Card? // If this isn't nil, the user is editing the card; otherwise, the user is creating a new card
    @State private var dateReviewed = Date.now
    @State private var answer = ""
    @State private var details = NSAttributedString()
//    @State private var attributedTextSelection = AttributedTextSelection()
    @State private var category: Category = .lit
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
        return !((initialState.dateReviewed == dateReviewed) && (initialState.answer == answer) && (initialState.details == AttributedString(details)) && (initialState.category == category) && (initialState.personalNotes == personalNotes) && (initialState.color == color))
        
    }
    
    var navigationTitle: String {
        return existingCard != nil ? (!unsavedChangesPresent ? "Edit/View Card" : "Edit/View Card (unsaved changes)") : "Create Card"
    }
    
    @State private var initialState = InitialState(dateReviewed: Date.now, answer: "", details: AttributedString(), category: .lit, personalNotes: "", color: .red.opacity(0.4))
    
    
    let colors: [Color] = [.red, .orange, .yellow, .green, .teal, .blue, .indigo, .purple, .pink, .brown]
    
    // Creating a completely new card
    init(geometry: GeometryProxy) {
        self.geometry = geometry
        existingCard = nil
    }
    
    // Editing or viewing card that has already been created
    init(geometry: GeometryProxy, existingCard: Card) {
        self.geometry = geometry
        self.existingCard = existingCard
    }
    
    // Creating a new card from AnalyzeView
    init(geometry: GeometryProxy, answer: String, details: AttributedString) {
        self.geometry = geometry
        self.answer = answer
        var formattedDetails = details
        formattedDetails.font = UIFont.systemFont(ofSize: 17)
        self.details = NSAttributedString(formattedDetails)
        existingCard = nil
    }
    
    var body: some View {
        Form {
            Section("Answer Line") {
                TextField("", text: $answer, prompt: Text("Enter the answerline of the question"))
            }
            Section("Category") {
                Picker("Select a category", selection: $category) {
                    ForEach(Category.allCases) { category in
                        Text(category.rawValue)
                            .tag(category)
                    }
                }
            }
            Section("Select Color") {
                colorPickerView()
            }
            Section("Personal Notes") {
                ZStack {
                    TextEditor(text: $personalNotes)
                        .scrollDisabled(true)
                        .focused($editingPersonalNotes)
                    //.frame(height: geometry.size.height/5)
                    Text("Add any personal notes here.")
                        .opacity(personalNotes.trimmingCharacters(in: .whitespacesAndNewlines) == "" ? 0.3 : 0)
                }
            }
            
            // MARK: Details
                Section("Details") {
                    //                TextEditor(text: $details, selection: $attributedTextSelection)
                    //                //.frame(height: geometry.size.height/2)
                    //                    .scrollDisabled(true)
                    //                    .focused($editingDetails)
                    TextEditorView(text: $details, geometry: geometry)
                        //.frame(width: geometry.size.width * 0.9)
                        .frame(maxWidth: .infinity)
                        .frame(minHeight: geometry.size.height * 0.1)
                        //.border(.green)
                        .focused($editingDetails)
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
            
        }.scrollContentBackground(.hidden)
            .background(
                Rectangle()
                    .fill(color)
                    .ignoresSafeArea()
            )
            .navigationTitle(navigationTitle)
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                loadAndPreprocess()
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
            .toolbarVisibility(.hidden, for: .tabBar)
    }
    
    private func loadAndPreprocess() {
        if let existingCard = existingCard {
            answer = existingCard.answer
            details = NSAttributedString(existingCard.details)
            category = existingCard.category
            personalNotes = existingCard.personalNotes
            dateReviewed = existingCard.dateReviewed
            color = convertRGBAToColor(existingCard.rgba)
        }
//        let paragraphStyle = NSMutableParagraphStyle()
//        paragraphStyle.lineBreakMode = .byWordWrapping
//        details.paragraphStyle = paragraphStyle
        initialState = InitialState(dateReviewed: dateReviewed, answer: answer, details: AttributedString(details), category: category, personalNotes: personalNotes, color: color)
    }
    
    private func save() {
        if let cardToEdit = existingCard {
            cardToEdit.answer = answer
            cardToEdit.details = AttributedString(details)
            cardToEdit.category = category
            cardToEdit.personalNotes = personalNotes
            cardToEdit.dateReviewed = dateReviewed
            cardToEdit.rgba = convertColorToRGBA(color)
        } else {
            let newCard = Card(dateReviewed: dateReviewed, answer: answer, details: AttributedString(details), category: category, personalNotes: personalNotes, rgba: convertColorToRGBA(color))
            modelContext.insert(newCard)
        }
    }
    
    private func colorPickerView() -> some View {
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
    
}

struct TextEditorView: UIViewRepresentable {
    @Binding var text: NSAttributedString
    var geometry: GeometryProxy
    
    func makeUIView(context: Context) -> UITextView {
        let textView = UITextView()
        textView.isEditable = true
        textView.font = UIFont.systemFont(ofSize: 17) // Default SwiftUI font size
        textView.isScrollEnabled = false
        textView.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        textView.delegate = context.coordinator
        
        textView.textContainer.lineFragmentPadding = 0
        textView.textContainerInset = .zero
        
        return textView
    }
    
    func updateUIView(_ uiView: UITextView, context: Context) {
        //let selectedRange = uiView.selectedRange
        uiView.attributedText = text
        uiView.setNeedsLayout()
        uiView.layoutIfNeeded()
        //uiView.selectedRange = selectedRange
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    func sizeThatFits(_ proposal: ProposedViewSize, uiView: UITextView, context: Context) -> CGSize? {
            // Ensure a valid width is proposed by SwiftUI
            guard let width = proposal.width else { return nil }
            
            
            // Use the UITextView's sizeThatFits method for the calculation
            let calculatedSize = uiView.sizeThatFits(
                CGSize(width: width, height: .greatestFiniteMagnitude)
            )
            
            // Return the size that the UITextView calculated
        return CGSize(width: width, height: calculatedSize.height)
        }
    
    class Coordinator: NSObject, UITextViewDelegate {
        var parent: TextEditorView
        
        init(_ parent: TextEditorView) {
            self.parent = parent
        }
        
        func textViewDidChange(_ textView: UITextView) {
            parent.text = textView.attributedText
            //textView.invalidateIntrinsicContentSize()
        }
    }
    
}
