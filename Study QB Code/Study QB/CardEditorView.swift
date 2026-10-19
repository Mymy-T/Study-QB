//
//  CardEditorView.swift
//  Study QB
//
//  Created by Milo on 11/5/25.
//

import SwiftData
import SwiftUI
import UIKit

// TODO: Add bold, italic, and underline buttons to details Text Editor keyboard toolbar for easy access; utilize keyboard toolbar if so (in the future potentially, not an essential feature)
// TODO: Dismissing answerline keyboard

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
        var markedForReview: Bool
    }
    
    let geometry: GeometryProxy
    let existingCard: Card? // If this isn't nil, the user is editing the card; otherwise, the user is creating a new card
    @State private var dateReviewed = Date.now
    @State private var answer = ""
    @State private var details = NSAttributedString()
    @State private var category: Category = .lit
    @State private var personalNotes = ""
    @State private var markedForReview = false
    @State private var color: Color = .red.opacity(0.4)
    
    @FocusState private var editingAnswer: Bool
    @FocusState private var editingDetails: Bool
    @FocusState private var editingPersonalNotes: Bool
    
    @State private var refresh = false // Only modified to trigger view reload
    
    @State private var initialCategoryAssigned = false // This is true until the category of a saved card is loaded; when this setting is true, the color of the card can be editing by switching categories.
    
    var dateString: String {
        let dateFormatter = DateFormatter()
        dateFormatter.locale = locale
        dateFormatter.dateStyle = .medium
        dateFormatter.timeStyle = .short
        return dateFormatter.string(from: dateReviewed)
    }
    
    var dueDateString: String {
        var dueDate = Date()
        if let card = existingCard {
            dueDate = card.fsrsCard!.due
        }
        let dateFormatter = DateFormatter()
        dateFormatter.locale = locale
        dateFormatter.dateStyle = .medium
        dateFormatter.timeStyle = .none
        return dateFormatter.string(from: dueDate)
    }
    
    var unsavedChangesPresent: Bool {
        return !((initialState.dateReviewed == dateReviewed) && (initialState.answer == answer) && (initialState.details == AttributedString(details)) && (initialState.category == category) && (initialState.personalNotes == personalNotes) && (initialState.color == color) && (initialState.markedForReview == markedForReview))
        
    }
    
    var navigationTitle: String {
        return existingCard != nil ? (!unsavedChangesPresent ? "Edit/View Card" : "Edit/View Card (unsaved changes)") : "Create Card"
    }
    
    @State private var initialState = InitialState(dateReviewed: Date.now, answer: "", details: AttributedString(), category: .lit, personalNotes: "", color: .red.opacity(0.4), markedForReview: false)
    
    
    
    
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
    init(geometry: GeometryProxy, answer: String, details: AttributedString, category: Category) {
        self.geometry = geometry
        self.answer = answer
        self.category = category
        
        let i = colorIndexFromCategory(category)
        color = colors[i].opacity(0.4)
        
        var formattedDetails = details
        formattedDetails.font = UIFont.systemFont(ofSize: 17)
        self.details = NSAttributedString(formattedDetails)
        existingCard = nil
    }
    
    var body: some View {
        Form {
            Section("Answer Line") {
                TextField("", text: $answer, prompt: Text("Enter the answerline of the question"))
                    .focused($editingAnswer)
            }
            Section("Category") {
                Picker("Select a category", selection: $category) {
                    ForEach(Category.allCases) { category in
                        Text(category.rawValue)
                            .tag(category)
                    }.padding()
                }.onChange(of: category) { _, _ in
                    updateColorUponCategoryChange()
                }
                .buttonStyle(.plain)
            }
            Section("Select Color") {
                colorPickerView()
            }
            Section("Personal Notes") {
                VStack {
                    Text("Here, you can list the facts that you want to review. List easier clues closer to the top and places clues on separate lines to make use of the clue reveal feature when reviewing.")
                        .font(.caption)
                        .lineLimit(nil)
                        .fixedSize(horizontal: false, vertical: true)
                        .padding(.bottom, 5)
                    HStack {
                        Button {
                            markedForReview.toggle()
                        } label: {
                            Label {
                                Text("Marked For Review")
                            } icon: {
                                Image(systemName: markedForReview ? "star.fill" : "star")
                                    .foregroundStyle(.yellow)
                            }
                            
                        }
                        Spacer()
                    }.padding(.bottom, 5)
                    Divider()
                    ZStack {
                        TextEditor(text: $personalNotes)
                            .scrollDisabled(true)
                            .focused($editingPersonalNotes)
                        Text("Add any personal notes here.")
                            .opacity(personalNotes.trimmingCharacters(in: .whitespacesAndNewlines) == "" ? 0.3 : 0)
                    }
                }
            }
            
            // MARK: Details
                Section("Details") {
                    TextEditorView(text: $details, geometry: geometry) {
                        keyboardToolbarView()
                    }
                        .frame(maxWidth: .infinity)
                        .frame(minHeight: geometry.size.height * 0.1)
                        .focused($editingDetails)
                }
            
            // MARK: Last Reviewed
            Section("Last Reviewed") {
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
            
            // MARK: Scheduled Date
            Section("Scheduled Review Date (Spaced Repetition)") {
                Text("This card is scheduled to be reviewed on **\(dueDateString)**")
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
                ToolbarItemGroup(placement: .keyboard) {
                    HStack {
                        Spacer()
                        Button("Done") {
                            if editingPersonalNotes {
                                editingPersonalNotes = false
                            }
                            if editingAnswer {
                                editingAnswer = false
                            }
                        }
                    }
                }
            }
            .toolbarVisibility(.hidden, for: .tabBar)
    }
    
    // MARK: loadAndPreprocess()
    private func loadAndPreprocess() {
        if let existingCard = existingCard {
            answer = existingCard.answer
            details = NSAttributedString(existingCard.details)
            category = existingCard.category
            personalNotes = existingCard.personalNotes
            markedForReview = existingCard.markedForReview
            dateReviewed = existingCard.dateReviewed
            color = convertRGBAToColor(existingCard.rgba)
            initialCategoryAssigned = false
        }
        initialState = InitialState(dateReviewed: dateReviewed, answer: answer, details: AttributedString(details), category: category, personalNotes: personalNotes, color: color, markedForReview: markedForReview)
    }
    
    // MARK: save()
    private func save() {
        if let cardToEdit = existingCard {
            cardToEdit.answer = answer
            cardToEdit.details = AttributedString(details)
            cardToEdit.category = category
            cardToEdit.personalNotes = personalNotes
            cardToEdit.markedForReview = markedForReview
            cardToEdit.dateReviewed = dateReviewed
            cardToEdit.rgba = convertColorToRGBA(color)
        } else {
            let newCard = Card(dateReviewed: dateReviewed, answer: answer, details: AttributedString(details), category: category, personalNotes: personalNotes, rgba: convertColorToRGBA(color), markedForReview: markedForReview)
            modelContext.insert(newCard)
        }
    }
    
    // MARK: updateColorUponCategoryChange
    private func updateColorUponCategoryChange() {
        if existingCard != nil {
            if initialCategoryAssigned {
                let i = colorIndexFromCategory(category)
                color = colors[i].opacity(0.4)
            } else {
                if existingCard!.category == .lit {
                    // Since the initialization of category sets it to .lit, loading a category of .lit from an existing card won't cause the .onChange closure to run. Therefore, an exception needs to be made to set initialCategoryAssigned to true for cards with a saved category of .lit
                    let i = colorIndexFromCategory(category)
                    color = colors[i].opacity(0.4)
                }
                initialCategoryAssigned = true
            }
        } else {
            let i = colorIndexFromCategory(category)
            color = colors[i].opacity(0.4)
        }
    }
    
    // MARK: colorPickerView
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
    
    // MARK: keyboardToolbarView
    private func keyboardToolbarView() -> some View {
        HStack {
            Spacer()
            Button("Done") {
                if editingDetails {
                    editingDetails = false
                }
            }.foregroundStyle(.black)
                .padding()
        }.glassEffectIfAvaliable()
            .frame(width: 0.9*geometry.size.width)
            
    }
    
}

// MARK: Text Editor View
struct TextEditorView<ToolbarView: View>: UIViewRepresentable {
    @Binding var text: NSAttributedString
    var geometry: GeometryProxy
    let toolbar: () -> ToolbarView
    
    func makeUIView(context: Context) -> UITextView {
        let textView = UITextView()
        textView.isEditable = true
        textView.font = UIFont.systemFont(ofSize: 17) // Default SwiftUI font size
        textView.allowsEditingTextAttributes = true
        textView.isScrollEnabled = false
        textView.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        textView.delegate = context.coordinator
        textView.attributedText = text
        
        textView.textContainer.lineFragmentPadding = 0
        textView.textContainerInset = .zero
        
        let hostingVC = UIHostingController(rootView: toolbar())
        hostingVC.view.backgroundColor = .clear
        textView.inputAccessoryView = hostingVC.view
        textView.inputAccessoryView?.translatesAutoresizingMaskIntoConstraints = false
        
        return textView
    }
    
    func updateUIView(_ uiView: UITextView, context: Context) {
        uiView.setNeedsLayout()
        uiView.layoutIfNeeded()

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
        }
        
    }
    
}

struct GlassEffectModifier: ViewModifier {
    @ViewBuilder
       func body(content: Content) -> some View {
           if #available(iOS 26.0, *) {
               content.glassEffect()
           } else {
               content
           }
       }
}

extension View {
    func glassEffectIfAvaliable() -> some View {
        self.modifier(GlassEffectModifier())
    }
}
