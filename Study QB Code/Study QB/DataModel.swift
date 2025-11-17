//
//  DataModel.swift
//  Study QB
//
//  Created by Milo on 11/5/25.
//

import Foundation
import SwiftData

enum Category: String, CaseIterable, Codable, Identifiable {
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
    
    var id: String {
        self.rawValue
    }
    
}

enum  Difficulty: String, Codable, CaseIterable, Identifiable {
    case ms = "Middle School"
    case hs = "High School"
    case col = "College"
    
    var id: String {
        self.rawValue
    }
    
}

@Model
class Card {
    var dateReviewed: Date
    var answer: String
    var details: AttributedString
    var category: Category?
    var personalNotes: String
    var rgba: [Double]
    
    init(dateReviewed: Date, answer: String, details: AttributedString, category: Category?, personalNotes: String, rgba: [Double]) {
        self.dateReviewed = dateReviewed
        self.answer = answer
        self.details = details
        self.category = category
        self.personalNotes = personalNotes
        self.rgba = rgba
    }
}
