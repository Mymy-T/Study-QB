//
//  DataModel.swift
//  Study QB
//
//  Created by Milo on 11/5/25.
//

import Foundation
import SwiftData
import SwiftUI

enum Category: String, CaseIterable, Codable, Identifiable, Comparable {
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
    
    static func < (lhs: Category, rhs: Category) -> Bool {
        return lhs.rawValue < rhs.rawValue
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

let colors: [Color] = [.red, .orange, .yellow, .green, .teal, .blue, .indigo, .purple, .pink, .brown]

@Model
class Card {
    var dateReviewed: Date
    var answer: String
    var details: AttributedString
    var category: Category
    var personalNotes: String
    var rgba: [Double]
    var categoryString: String {
        return category.rawValue
    }
    
//    var categoryString: String {
//        category.rawValue
//    }
    
    init(dateReviewed: Date, answer: String, details: AttributedString, category: Category, personalNotes: String, rgba: [Double]) {
        self.dateReviewed = dateReviewed
        self.answer = answer
        self.details = details
        self.category = category
        self.personalNotes = personalNotes
        self.rgba = rgba
    }
}
