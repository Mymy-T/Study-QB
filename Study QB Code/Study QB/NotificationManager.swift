//
//  NotificationManager.swift
//  Study QB
//
//  Created by Milo on 7/11/26.
//

import Foundation
import UserNotifications

// TODO: Make notification button options appear immediately instead of on long press (default mode)

enum ReminderMode: Int { 
    case daily = 0 
    case interval = 1 
} 

enum SettingsKeys { 
    static let notificationsEnabled = "notificationsEnabled" 
    static let reminderMode = "reminderMode" 
    static let reminderHour = "reminderHour" 
    static let reminderMinute = "reminderMinute" 
    static let selectedWeekdaysMask = "selectedWeekdaysMask" 
    static let reminderIntervalHours = "reminderIntervalHours" 
} 

final class NotificationManager: NSObject { // Singleton
    static let shared = NotificationManager()
    private override init() {} // Override init of NSObject and make it private to maintain singleton

    private let center = UNUserNotificationCenter.current()

    private static let intervalIdentifier = "studyReminder.interval"
    private static let snoozeIdentifier = "studyReminder.snooze"

    // Category and action identifiers for the notification buttons
    static let reminderCategoryIdentifier = "studyReminder.category"
    static let studyNowActionIdentifier = "studyReminder.action.studyNow"
    static let snoozeActionIdentifier = "studyReminder.action.snooze"

    // Callws once at app launch; sets the delegate and registers the action buttons with the system.
    func configure() {
        center.delegate = self

        let studyNow = UNNotificationAction(
            identifier: Self.studyNowActionIdentifier,
            title: "Study Now",
            options: [.foreground] // Opens the app
        )
        let snooze = UNNotificationAction(
            identifier: Self.snoozeActionIdentifier,
            title: "Snooze - 1 Hour",
            options: []
        )
        let category = UNNotificationCategory(
            identifier: Self.reminderCategoryIdentifier,
            actions: [studyNow, snooze],
            intentIdentifiers: []
        )
        center.setNotificationCategories([category])
    }

    private static func weekdayIdentifier(_ weekday: Int) -> String { 
        "studyReminder.weekday.\(weekday)" 
    } 

    private static let allWeekdayIdentifiers = (1...7).map { weekdayIdentifier($0) } 

    func requestAuthorization() async -> Bool { 
        do { 
            return try await center.requestAuthorization(options: [.alert, .sound]) 
        } catch { 
            return false 
        } 
    } 

    func currentAuthorizationStatus() async -> UNAuthorizationStatus { 
        return await center.notificationSettings().authorizationStatus
    }

    func rescheduleReminders() { 
        let defaults = UserDefaults.standard 

        center.removePendingNotificationRequests(withIdentifiers: Self.allWeekdayIdentifiers + [Self.intervalIdentifier])

        guard defaults.bool(forKey: SettingsKeys.notificationsEnabled) else { return } // No need to reschedule if notifications are disabled

        let modeRaw = defaults.object(forKey: SettingsKeys.reminderMode) as? Int ?? ReminderMode.daily.rawValue 
        let mode = ReminderMode(rawValue: modeRaw) ?? .daily 

        switch mode { 
        case .daily: 
            scheduleDaily( 
                hour: defaults.object(forKey: SettingsKeys.reminderHour) as? Int ?? 9, 
                minute: defaults.object(forKey: SettingsKeys.reminderMinute) as? Int ?? 0, 
                weekdaysMask: defaults.object(forKey: SettingsKeys.selectedWeekdaysMask) as? Int ?? 127 // Default toggles notifications on for all days
            )
        case .interval: 
            scheduleInterval(hours: defaults.object(forKey: SettingsKeys.reminderIntervalHours) as? Int ?? 4) 
        } 
    } 

    private func scheduleDaily(hour: Int, minute: Int, weekdaysMask: Int) { 
        for weekday in 1...7 where (weekdaysMask & (1 << (weekday - 1))) != 0 { // Check if the bit supposed to be representing weekday (1 << (weekday - 1)) is actually turned on in weekdays mask
            var comps = DateComponents()
            comps.hour = hour 
            comps.minute = minute 
            comps.weekday = weekday 
            let trigger = UNCalendarNotificationTrigger(dateMatching: comps, repeats: true) 
            let request = UNNotificationRequest( 
                identifier: Self.weekdayIdentifier(weekday), 
                content: Self.makeContent(), 
                trigger: trigger 
            ) 
            center.add(request) 
        } 
    } 

    private func scheduleInterval(hours: Int) { 
        let seconds = max(TimeInterval(hours) * 3600, 60) // The scheduled interval must be a minimum of 60 seconds ahead
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: seconds, repeats: true)
        let request = UNNotificationRequest( 
            identifier: Self.intervalIdentifier, 
            content: Self.makeContent(), 
            trigger: trigger 
        ) 
        center.add(request) 
    } 

    private static func makeContent() -> UNMutableNotificationContent { 
        let content = UNMutableNotificationContent() 
        content.title = "Time to study!" 
        content.body = "Let's review a couple cards now."
        content.sound = .default
        content.categoryIdentifier = reminderCategoryIdentifier // Attach action buttons
        return content
    } 

    func pendingRequests() async -> [UNNotificationRequest] {
        await center.pendingNotificationRequests()
    }
}

extension NotificationManager: UNUserNotificationCenterDelegate {
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async {
        switch response.actionIdentifier {
        case Self.snoozeActionIdentifier:
            scheduleSnooze()
        default: // Self.studyNowActionIdentifier
            break // App already opened by action
        }
    }

    // Schedules reminder an hour later
    private func scheduleSnooze() {
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 3600, repeats: false)
        let request = UNNotificationRequest(
            identifier: Self.snoozeIdentifier,
            content: Self.makeContent(),
            trigger: trigger
        )
        center.add(request)
    }
}
