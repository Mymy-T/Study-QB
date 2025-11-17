//
//  Study_QBApp.swift
//  Study QB
//
//  Created by Milo on 10/23/25.
//

import Firebase
import SwiftData
import SwiftUI

// TODO: Set max character limit for answerline, or add shrinking capabilities in ManageCardView

@main
struct Study_QBApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .modelContainer(for: Card.self)
        }
    }
    
    init() {
        FirebaseApp.configure()
    }
    
}
