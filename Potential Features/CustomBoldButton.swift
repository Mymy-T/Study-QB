//
//  FeaturesInProgress.swift
//  Study QB
//
//  Created by Milo on 12/16/25.
//

// MARK: The following code belongs to CardEditorView. These pieces of code are utilized in a feature that allows for the easy bolding of text using a keyboard toolbar. This code is not fully debugged yet, and italics and underlining aren't supported yet.

/// State variables for managing the bolding state
//    @State private var bold = false
//    @State private var boldButtonPressed = false

// Corresponding binding variables in TextEditorView struct
//    @Binding var bold: Bool
//    @Binding var boldButtonPressed: Bool

/// Add this to the TextEditorView struct to allow for a toolbar above the keyboard
//  let toolbar: () -> ToolbarView

/// To enable the complier to recognize ToolbarView as a valid type, change the TextEditorView struct definition to the following:
//struct TextEditorView<ToolbarView: View>: UIViewRepresentable

/// Then, within the makeUIView function of the TextEditorView struct, add the following:
//let hostingVC = UIHostingController(rootView: toolbar())
//hostingVC.view.backgroundColor = .clear
//textView.inputAccessoryView = hostingVC.view
//textView.inputAccessoryView?.translatesAutoresizingMaskIntoConstraints = false

/// This call shows the TextEditorView
//                    TextEditorView(text: $details, bold: $bold, boldButtonPressed: $boldButtonPressed, geometry: geometry) {
//                        keyboardToolbarView()
//                    }


/// This is the keyboard toolbar view function
//private func keyboardToolbarView() -> some View {
//    HStack {
//        Button {
//            boldButtonPressed = true
//        } label: {
//            Image(systemName: "bold")
//                .padding(5)
//                .tint(bold ? .blue : .gray)
//        }
//        Spacer()
//        Button("Done") {
//            if editingDetails {
//                editingDetails = false
//            }
//        }.padding(5)
//            .tint(.green)
//    }.padding(5)
//        .glassEffect()
//    .frame(width: geometry.size.width * 0.9)
//}

/// This snippet belongs in the updateUIView function in the TextEditorView struct
//        if boldButtonPressed {
//            context.coordinator.boldButtonPress(uiView)
//        }

/// These code snippets belong in the Coordinator class

//        func textViewDidChangeSelection(_ textView: UITextView) {
//            parent.bold = selectionIsBold(textView)
//        }
        
//        func selectionIsBold(_ textView: UITextView) -> Bool {
//            var selectionBold = true // Initially, before enumerate attribute is run
//            if let selectedRange = textView.selectedTextRange {
//                let nsRange = NSRange(location: textView.offset(from: textView.beginningOfDocument, to: selectedRange.start), length: textView.offset(from: selectedRange.start, to: selectedRange.end))
//
//                textView.attributedText.enumerateAttribute(.font, in: nsRange) { attribute, range, stop in
//                    if let font = attribute as? UIFont {
//                        if font.fontDescriptor.symbolicTraits.contains(.traitBold) == false {
//                            // If any part of the selection isn't bold, then the selection isn't considered bold
//                            selectionBold = false
//                            stop.pointee = true // Stop the enumeration
//                        }
//                    } else {
//                        // This shouldn't happen, this is if there is not font attribute
//                        selectionBold = false
//                        stop.pointee = true // Stop the enumeration
//                    }
//                }
//
//            } else {
//                selectionBold = false
//            }
//            return selectionBold
//        }
        
        
//        func boldButtonPress(_ textView: UITextView) {
//
//            parent.boldButtonPressed = false
//
//            if let selectedRange = textView.selectedTextRange {
//                if selectedRange.isEmpty == false {
//                    // Execute the following if some text is selected
//
//                    let attributedString = NSMutableAttributedString(attributedString: textView.attributedText)
//
//                    let nsRange = NSRange(location: textView.offset(from: textView.beginningOfDocument, to: selectedRange.start), length: textView.offset(from: selectedRange.start, to: selectedRange.end))
//
//                    if parent.bold {
////                        attributedString.enumerateAttribute(.font, in: nsRange) { attribute, range, stop in
////                            if let oldFont = attribute as? UIFont {
////                                let newFont = UIFont.systemFont(ofSize: oldFont.pointSize, weight: .regular)
////                                attributedString.addAttributes([.font: newFont], range: range)
////                            }
////                        }
//                        attributedString.addAttribute(.font, value: UIFont.systemFont(ofSize: 17, weight: .regular), range: nsRange)
//                        parent.bold = false
//                    } else {
////                        attributedString.enumerateAttribute(.font, in: nsRange) { attribute, range, stop in
////                            if let oldFont = attribute as? UIFont {
////                                let newFont = UIFont.systemFont(ofSize: oldFont.pointSize, weight: .bold)
////                                attributedString.addAttributes([.font: newFont], range: range)
////                            }
////                        }
//
//                        attributedString.addAttribute(.font, value: UIFont.systemFont(ofSize: 17, weight: .bold), range: nsRange)
//                        parent.bold = true
//                    }
//                    parent.text = NSAttributedString(attributedString: attributedString)
//
//                } else {
//                    // Otherwise, execute this if no text is selected (just a cursor)
//                    let newBoldValue = !parent.bold // Execute button action first
//                    var oldFont: UIFont
//                    if textView.font != nil {
//                        oldFont = textView.font!
//                    } else {
//                        oldFont = UIFont.systemFont(ofSize: 17)
//                    }
//                    if newBoldValue {
//                        textView.typingAttributes[.font] = UIFont.systemFont(ofSize: oldFont.pointSize, weight: .bold)
//                    } else {
//                        textView.typingAttributes[.font] = UIFont.systemFont(ofSize: oldFont.pointSize, weight: .regular)
//                    }
//
//                    parent.bold.toggle()
//
//                }
//            }
//        }
