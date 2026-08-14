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

// MARK: cardExists
func cardExists(forAnswer answer: String, in cards: [Card]) -> Bool {
    let trimmed = answer.trimmingCharacters(in: .whitespacesAndNewlines)
    guard trimmed.count >= 3 else {
        return cards.contains { $0.answer.caseInsensitiveCompare(trimmed) == .orderedSame }
    }
    return cards.contains { card in
        let cardAnswer = card.answer.trimmingCharacters(in: .whitespacesAndNewlines)
        guard cardAnswer.count >= 3 else {
            return cardAnswer.caseInsensitiveCompare(trimmed) == .orderedSame
        }
        return trimmed.localizedCaseInsensitiveContains(cardAnswer)
            || cardAnswer.localizedCaseInsensitiveContains(trimmed)
    }
}

let colors: [Color] = [.red, .orange, .yellow, .green, .teal, .blue, .indigo, .purple, .pink, .brown]

protocol CardLike {
    var answer: String { get }
    var personalNotes: String { get }
}

typealias Card = CardSchemaV4.Card
typealias FSRSCard = SwiftFSRS.Card

// MARK: computePostReviewCard
func computePostReviewCard(card: Card, rating: Rating, reviewTime: Date = Date()) -> FSRSCard {
    let scheduler = LongTermScheduler()
    let review = scheduler.schedule(
        card: card.fsrsCard!,
        algorithm: .v5,
        reviewRating: rating,
        reviewTime: reviewTime
    )
    var postCard = review.postReviewCard
    if rating == .again {
        postCard.due = reviewTime
    }
    return postCard
}

// MARK: scheduleReview
func scheduleReview(card: Card, rating: Rating, modelContext: ModelContext) {
    card.fsrsCard = computePostReviewCard(card: card, rating: rating)
    do {
        try modelContext.save()
    } catch {
        print("Error saving FSRS review card")
    }
}

// MARK: BuzzGameStat
@Model
class BuzzGameStat {
    var date: Date
    var points: Int
    var numberCorrect: Int
    var numberIncorrect: Int

    init(date: Date = Date(), points: Int, numberCorrect: Int, numberIncorrect: Int) {
        self.date = date
        self.points = points
        self.numberCorrect = numberCorrect
        self.numberIncorrect = numberIncorrect
    }
}

// MARK: CardStat
@Model
class CardStat {
    var answer: String
    var category: Category
    var timesSeen: Int
    var timesCorrect: Int
    var totalBuzzPositionPercent: Double

    init(answer: String, category: Category, timesSeen: Int = 0, timesCorrect: Int = 0, totalBuzzPositionPercent: Double = 0) {
        self.answer = answer
        self.category = category
        self.timesSeen = timesSeen
        self.timesCorrect = timesCorrect
        self.totalBuzzPositionPercent = totalBuzzPositionPercent
    }

    var averageBuzzPositionPercent: Double {
        timesSeen > 0 ? totalBuzzPositionPercent / Double(timesSeen) : 0
    }
}

// MARK: SchemaV1
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

// MARK: SchemaV2
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

// MARK: SchemaV3
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

// MARK: SchemaV4
enum CardSchemaV4: VersionedSchema {

    static var versionIdentifier = Schema.Version(1, 3, 0)
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
        var fsrsCard: FSRSCard?
        var cachedTossup: String? = nil

        init(dateReviewed: Date, answer: String, details: AttributedString, category: Category, personalNotes: String, rgba: [Double], markedForReview: Bool = false, fsrsCard: FSRSCard = FSRSCard(), cachedTossup: String? = nil) {
            self.dateReviewed = dateReviewed
            self.answer = answer
            self.details = details
            self.category = category
            self.personalNotes = personalNotes
            self.rgba = rgba
            self.markedForReview = markedForReview
            self.fsrsCard = fsrsCard
            self.cachedTossup = cachedTossup
        }

    }

}

// MARK: SchemaV5
enum CardSchemaV5: VersionedSchema {

    static var versionIdentifier = Schema.Version(1, 4, 0)
    static var models: [any PersistentModel.Type] {
            [CardSchemaV4.Card.self, BuzzGameStat.self, CardStat.self]
        }

}

// MARK: CardMigrationPlan
enum CardMigrationPlan: SchemaMigrationPlan {
    static var schemas: [any VersionedSchema.Type] {
        [CardSchemaV1.self, CardSchemaV2.self, CardSchemaV3.self, CardSchemaV4.self, CardSchemaV5.self]
    }

    static let migrateV1toV2 = MigrationStage.lightweight(fromVersion: CardSchemaV1.self, toVersion: CardSchemaV2.self)
    static let migrateV3toV4 = MigrationStage.lightweight(fromVersion: CardSchemaV3.self, toVersion: CardSchemaV4.self)
    static let migrateV4toV5 = MigrationStage.lightweight(fromVersion: CardSchemaV4.self, toVersion: CardSchemaV5.self)
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
        [migrateV1toV2, migrateV2toV3, migrateV3toV4, migrateV4toV5]
    }
}
