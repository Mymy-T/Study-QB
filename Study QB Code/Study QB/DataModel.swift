//
//  DataModel.swift
//  Study QB
//
//  Created by Milo on 11/5/25.
//

import Foundation
import SwiftData
import SwiftFSRS
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

typealias Card = CardSchemaV3.Card
typealias FSRSCard = SwiftFSRS.Card

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

enum CardSchemaV3: VersionedSchema {
    
    static var versionIdentifier = Schema.Version(1, 2, 0)
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
        var fsrsCard: FSRSCard? // Initialized as optional to support data migration; it will actually never be nil, due to the default in init and the custom data migration
        
        init(dateReviewed: Date, answer: String, details: AttributedString, category: Category, personalNotes: String, rgba: [Double], markedForReview: Bool = false, fsrsCard: FSRSCard = FSRSCard()) {
            self.dateReviewed = dateReviewed
            self.answer = answer
            self.details = details
            self.category = category
            self.personalNotes = personalNotes
            self.rgba = rgba
            self.markedForReview = markedForReview
            self.fsrsCard = fsrsCard
        }
        
        
    }
    
}

enum CardMigrationPlan: SchemaMigrationPlan {
    static var schemas: [any VersionedSchema.Type] {
        [CardSchemaV1.self, CardSchemaV2.self, CardSchemaV3.self]
    }
    
    static let migrateV1toV2 = MigrationStage.lightweight(fromVersion: CardSchemaV1.self, toVersion: CardSchemaV2.self)
    static let migrateV2toV3 = MigrationStage.custom(
        fromVersion: CardSchemaV2.self,
        toVersion: CardSchemaV3.self,
        willMigrate: { context in
        },
        didMigrate: { context in
            let cards = try context.fetch(FetchDescriptor<CardSchemaV3.Card>())
            for card in cards {
                // Manually ensure the default is set if the migration left it empty
                card.fsrsCard = FSRSCard()
            }
            try context.save()
        }
    )
    
    static var stages: [MigrationStage] {
        [migrateV1toV2, migrateV2toV3]
    }
}
