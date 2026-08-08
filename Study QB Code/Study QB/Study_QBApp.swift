//
//  Study_QBApp.swift
//  Study QB
//
//  Created by Milo on 10/23/25.
//

import SwiftData
import SwiftUI

// TODO: Possibly add a way to save and load review configurations

@main
struct Study_QBApp: App {
    
    let container: ModelContainer
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .modelContainer(container)
        }
    }
    
    init() {
        NotificationManager.shared.configure()
        do {
            container = try ModelContainer(
                for: Card.self, BuzzGameStat.self, CardStat.self,
                migrationPlan: CardMigrationPlan.self
            )
        } catch {
            fatalError("Failed to initialize model container.")
        }
    }
    
}
