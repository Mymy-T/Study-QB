//
//  SettingsView.swift
//  Study QB
//
//  Created by Milo on 6/26/26.
//

import SwiftData
import SwiftUI
import UIKit // added

struct SettingsView: View {
    @Environment(\.modelContext) var modelContext
    @Query var cards: [Card]

    @State private var showDeleteAllAlert = false
    @State private var showClearCacheAlert = false

    @AppStorage(SettingsKeys.notificationsEnabled) private var notificationsEnabled = false // added
    @AppStorage(SettingsKeys.reminderMode) private var reminderModeRaw = ReminderMode.daily.rawValue // added
    @AppStorage(SettingsKeys.reminderHour) private var reminderHour = 9 // added
    @AppStorage(SettingsKeys.reminderMinute) private var reminderMinute = 0 // added
    @AppStorage(SettingsKeys.selectedWeekdaysMask) private var selectedWeekdaysMask = 127 // added
    @AppStorage(SettingsKeys.reminderIntervalHours) private var reminderIntervalHours = 4 // added
    @State private var showNotificationsDeniedAlert = false // added

    var body: some View {
        NavigationStack {
            Form {
                Section("Notifications") { // added
                    Toggle("Study Reminders", isOn: $notificationsEnabled) // added
                        .onChange(of: notificationsEnabled) { _, newValue in // added
                            Task { await handleNotificationsToggle(newValue) } // added
                        } // added

                    if notificationsEnabled { // added
                        Picker("Reminder Type", selection: $reminderModeRaw) { // added
                            Text("Daily").tag(ReminderMode.daily.rawValue) // added
                            Text("Every N Hours").tag(ReminderMode.interval.rawValue) // added
                        } // added
                        .pickerStyle(.segmented) // added
                        .onChange(of: reminderModeRaw) { _, _ in NotificationManager.shared.rescheduleReminders() } // added

                        if reminderModeRaw == ReminderMode.daily.rawValue { // added
                            DatePicker("Time", selection: dailyTimeBinding, displayedComponents: .hourAndMinute) // added
                                .onChange(of: reminderHour) { _, _ in NotificationManager.shared.rescheduleReminders() } // added
                                .onChange(of: reminderMinute) { _, _ in NotificationManager.shared.rescheduleReminders() } // added

                            HStack { // added
                                ForEach(1...7, id: \.self) { weekday in // added
                                    Toggle(isOn: weekdayBinding(weekday)) { // added
                                        Text(weekdaySymbol(weekday)) // added
                                    } // added
                                    .toggleStyle(.button) // added
                                } // added
                            } // added
                            .onChange(of: selectedWeekdaysMask) { _, _ in NotificationManager.shared.rescheduleReminders() } // added
                        } else { // added
                            Stepper("Every \(reminderIntervalHours) hour\(reminderIntervalHours == 1 ? "" : "s")", // added
                                    value: $reminderIntervalHours, in: 1...12) // added
                                .onChange(of: reminderIntervalHours) { _, _ in NotificationManager.shared.rescheduleReminders() } // added
                        } // added
                    } // added
                } // added
                Section("Delete") {
                        
                    Button {
                        showClearCacheAlert = true
                    } label: {
                        Label("Delete Cached Tossup Data", systemImage: "trash")
                            .foregroundStyle(.red)
                    }
                    .buttonStyle(.borderless)
                    
                    
                    Button {
                        showDeleteAllAlert = true
                    } label: {
                        Label("DELETE ALL CARDS", systemImage: "trash")
                            .foregroundStyle(.red)
                    }
                    .buttonStyle(.borderless)
                    
                }
            }.navigationTitle("Settings")
                .alert("ARE YOU SURE?", isPresented: $showDeleteAllAlert) {
                    Button("Cancel", role: .cancel) { }
                    Button("Delete", role: .destructive) {
                        for card in cards {
                            modelContext.delete(card)
                        }
                    }
                } message: {
                    Text("Are you sure you want to delete ALL of your cards? This action is irreversible.")
                }
                .alert("Delete cached tossup data?", isPresented: $showClearCacheAlert) {
                    Button("Cancel", role: .cancel) { }
                    Button("Clear", role: .destructive) {
                        for card in cards {
                            card.cachedTossup = nil
                        }
                    }
                }
                .alert("Notifications Disabled", isPresented: $showNotificationsDeniedAlert) { // added
                    Button("Cancel", role: .cancel) { } // added
                    Button("Open Settings") { // added
                        if let url = URL(string: UIApplication.openSettingsURLString) { // added
                            UIApplication.shared.open(url) // added
                        } // added
                    } // added
                } message: { // added
                    Text("Study QB needs notification permission to send study reminders. You can enable this in the Settings app.") // added
                } // added
        }
    }

    private func handleNotificationsToggle(_ enabling: Bool) async { // added
        guard enabling else { // added
            NotificationManager.shared.rescheduleReminders() // added
            return // added
        } // added
        let granted = await NotificationManager.shared.requestAuthorization() // added
        if granted { // added
            NotificationManager.shared.rescheduleReminders() // added
        } else { // added
            notificationsEnabled = false // added
            showNotificationsDeniedAlert = true // added
        } // added
    } // added

    private var dailyTimeBinding: Binding<Date> { // added
        Binding( // added
            get: { // added
                Calendar.current.date(bySettingHour: reminderHour, minute: reminderMinute, second: 0, of: .now) ?? .now // added
            }, // added
            set: { newDate in // added
                let comps = Calendar.current.dateComponents([.hour, .minute], from: newDate) // added
                reminderHour = comps.hour ?? reminderHour // added
                reminderMinute = comps.minute ?? reminderMinute // added
            } // added
        ) // added
    } // added

    private func weekdayBinding(_ weekday: Int) -> Binding<Bool> { // added
        Binding( // added
            get: { (selectedWeekdaysMask & (1 << (weekday - 1))) != 0 }, // added
            set: { isOn in // added
                if isOn { // added
                    selectedWeekdaysMask |= (1 << (weekday - 1)) // added
                } else { // added
                    selectedWeekdaysMask &= ~(1 << (weekday - 1)) // added
                } // added
            } // added
        ) // added
    } // added

    private func weekdaySymbol(_ weekday: Int) -> String { // added
        ["S", "M", "T", "W", "T", "F", "S"][weekday - 1] // added
    } // added
}
