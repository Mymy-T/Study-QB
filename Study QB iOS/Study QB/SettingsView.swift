//
//  SettingsView.swift
//  Study QB
//
//  Created by Milo on 6/26/26.
//

import SwiftData
import SwiftUI
import UIKit  

struct SettingsView: View {
    @Environment(\.modelContext) var modelContext
    @Query var cards: [Card]

    @State private var showDeleteAllAlert = false
    @State private var showClearCacheAlert = false
    @State private var deleteAllConfirmationText = ""

    @AppStorage(SettingsKeys.notificationsEnabled) private var notificationsEnabled = false  
    @AppStorage(SettingsKeys.reminderMode) private var reminderModeRaw = ReminderMode.daily.rawValue  
    @AppStorage(SettingsKeys.reminderHour) private var reminderHour = 9  
    @AppStorage(SettingsKeys.reminderMinute) private var reminderMinute = 0  
    @AppStorage(SettingsKeys.selectedWeekdaysMask) private var selectedWeekdaysMask = 127  
    @AppStorage(SettingsKeys.reminderIntervalHours) private var reminderIntervalHours = 4  
    @State private var showNotificationsDeniedAlert = false  

    var body: some View {
        NavigationStack {
            Form {
                Section("Notifications") {  
                    Toggle("Study Reminders", isOn: $notificationsEnabled)  
                        .onChange(of: notificationsEnabled) { _, newValue in  
                            Task { await handleNotificationsToggle(newValue) }  
                        }  

                    if notificationsEnabled {  
                        Picker("Reminder Type", selection: $reminderModeRaw) {  
                            Text("Daily").tag(ReminderMode.daily.rawValue)  
                            Text("Every N Hours").tag(ReminderMode.interval.rawValue)  
                        }  
                        .pickerStyle(.segmented)  
                        .onChange(of: reminderModeRaw) { _, _ in NotificationManager.shared.rescheduleReminders() }  

                        if reminderModeRaw == ReminderMode.daily.rawValue {  
                            DatePicker("Time", selection: dailyTimeBinding, displayedComponents: .hourAndMinute)  
                                .onChange(of: reminderHour) { _, _ in NotificationManager.shared.rescheduleReminders() }  
                                .onChange(of: reminderMinute) { _, _ in NotificationManager.shared.rescheduleReminders() }  

                            HStack {  
                                ForEach(1...7, id: \.self) { weekday in  
                                    Toggle(isOn: weekdayBinding(weekday)) {  
                                        Text(weekdaySymbol(weekday))  
                                    }  
                                    .toggleStyle(.button)  
                                }  
                            }  
                            .onChange(of: selectedWeekdaysMask) { _, _ in NotificationManager.shared.rescheduleReminders() }  
                        } else {  
                            Stepper("Every \(reminderIntervalHours) hour\(reminderIntervalHours == 1 ? "" : "s")",  
                                    value: $reminderIntervalHours, in: 1...12)  
                                .onChange(of: reminderIntervalHours) { _, _ in NotificationManager.shared.rescheduleReminders() }  
                        }  
                    }  
                }  
                Section("Delete") {
                        
                    Button {
                        showClearCacheAlert = true
                    } label: {
                        Label("Delete Cached Tossup Data", systemImage: "trash")
                            .foregroundStyle(.red)
                    }
                    .buttonStyle(.borderless)
                    
                    
                    Button {
                        deleteAllConfirmationText = ""
                        showDeleteAllAlert = true
                    } label: {
                        Label("DELETE ALL CARDS", systemImage: "trash")
                            .foregroundStyle(.red)
                    }
                    .buttonStyle(.borderless)
                    
                }
            }.navigationTitle("Settings")
                .alert("ARE YOU SURE?", isPresented: $showDeleteAllAlert) {
                    TextField("Type \"delete\" to confirm", text: $deleteAllConfirmationText)
                        .textInputAutocapitalization(.never)
                    Button("Cancel", role: .cancel) {
                        deleteAllConfirmationText = ""
                    }
                    Button("Delete", role: .destructive) {
                        for card in cards {
                            modelContext.delete(card)
                        }
                        deleteAllConfirmationText = ""
                    }
                    .disabled(deleteAllConfirmationText.lowercased() != "delete")
                } message: {
                    Text("Are you sure you want to delete ALL of your cards? This action is irreversible. Type \"delete\" to confirm.")
                }
                .alert("Delete cached tossup data?", isPresented: $showClearCacheAlert) {
                    Button("Cancel", role: .cancel) { }
                    Button("Clear", role: .destructive) {
                        for card in cards {
                            card.cachedTossup = nil
                        }
                    }
                }
                .alert("Notifications Disabled", isPresented: $showNotificationsDeniedAlert) {  
                    Button("Cancel", role: .cancel) { }  
                    Button("Open Settings") {  
                        if let url = URL(string: UIApplication.openSettingsURLString) {  
                            UIApplication.shared.open(url)  
                        }  
                    }  
                } message: {  
                    Text("Study QB needs notification permission to send study reminders. You can enable this in the Settings app.")  
                }  
        }
    }

    private func handleNotificationsToggle(_ enabling: Bool) async {  
        guard enabling else {  
            NotificationManager.shared.rescheduleReminders()  
            return  
        }  
        let granted = await NotificationManager.shared.requestAuthorization()  
        if granted {  
            NotificationManager.shared.rescheduleReminders()  
        } else {  
            notificationsEnabled = false  
            showNotificationsDeniedAlert = true  
        }  
    }  

    private var dailyTimeBinding: Binding<Date> {  
        Binding(  
            get: {  
                Calendar.current.date(bySettingHour: reminderHour, minute: reminderMinute, second: 0, of: .now) ?? .now  
            },  
            set: { newDate in  
                let comps = Calendar.current.dateComponents([.hour, .minute], from: newDate)  
                reminderHour = comps.hour ?? reminderHour  
                reminderMinute = comps.minute ?? reminderMinute  
            }  
        )  
    }  

    private func weekdayBinding(_ weekday: Int) -> Binding<Bool> {  
        Binding(  
            get: { (selectedWeekdaysMask & (1 << (weekday - 1))) != 0 },  
            set: { isOn in  
                if isOn {  
                    selectedWeekdaysMask |= (1 << (weekday - 1))  
                } else {  
                    selectedWeekdaysMask &= ~(1 << (weekday - 1))  
                }  
            }  
        )  
    }  

    private func weekdaySymbol(_ weekday: Int) -> String {  
        ["S", "M", "T", "W", "T", "F", "S"][weekday - 1]  
    }  
}
