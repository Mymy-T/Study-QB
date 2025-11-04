//
//  Study_QBApp.swift
//  Study QB
//
//  Created by Milo on 10/23/25.
//

import Firebase
import SwiftUI

@main
struct Study_QBApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
    
    init() {
        FirebaseApp.configure()
    }
    
}
