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

// Following structs for representing api json data returned from frequency-list
struct FreqListItem: Codable, Identifiable {
    var count: Int
    var answer: String
    var id = UUID()
    
    enum CodingKeys: String, CodingKey {
        case count, answer
    }
    
}

struct FrequencyList: Codable {
    var frequencyList: [FreqListItem]
}

let colors: [Color] = [.red, .orange, .yellow, .green, .teal, .blue, .indigo, .purple, .pink, .brown]

protocol CardLike {
    var answer: String { get }
    var personalNotes: String { get }
}

typealias Card = CardSchemaV2.Card

enum CardSchemaV1: VersionedSchema {
    
    static var versionIdentifier = Schema.Version(1, 0, 0)
    static var models: [any PersistentModel.Type] {
            [Card.self]
        }
    
    @Model
    class Card: CardLike {
        var dateReviewed: Date
        var answer: String
        var details: AttributedString
        var category: Category
        var personalNotes: String
        var rgba: [Double]
        var categoryString: String {
            return category.rawValue
        }
        
        init(dateReviewed: Date, answer: String, details: AttributedString, category: Category, personalNotes: String, rgba: [Double]) {
            self.dateReviewed = dateReviewed
            self.answer = answer
            self.details = details
            self.category = category
            self.personalNotes = personalNotes
            self.rgba = rgba
        }
        
        
    }
    
}

enum CardSchemaV2: VersionedSchema {
    
    static var versionIdentifier = Schema.Version(1, 1, 0)
    static var models: [any PersistentModel.Type] {
            [Card.self]
        }
    
    @Model
    class Card: CardLike {
        var dateReviewed: Date
        var answer: String
        var details: AttributedString
        var category: Category
        var personalNotes: String
        var rgba: [Double]
        var categoryString: String {
            return category.rawValue
        }
        var markedForReview: Bool = false
        
        init(dateReviewed: Date, answer: String, details: AttributedString, category: Category, personalNotes: String, rgba: [Double], markedForReview: Bool = false) {
            self.dateReviewed = dateReviewed
            self.answer = answer
            self.details = details
            self.category = category
            self.personalNotes = personalNotes
            self.rgba = rgba
            self.markedForReview = markedForReview
        }
        
        
    }
    
}

enum CardMigrationPlan: SchemaMigrationPlan {
    static var schemas: [any VersionedSchema.Type] {
        [CardSchemaV1.self, CardSchemaV2.self]
    }
    
    static let migrateV1toV2 = MigrationStage.lightweight(fromVersion: CardSchemaV1.self, toVersion: CardSchemaV2.self)
    
    static var stages: [MigrationStage] {
        [migrateV1toV2]
    }
}
