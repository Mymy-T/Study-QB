//
//  Functions.swift
//  Study QB
//
//  Created by Milo on 11/14/25.
//

import Foundation
import SwiftData
import SwiftUI

func convertRGBAToColor(_ rgba: [Double]) -> Color {
    return Color(red: rgba[0], green: rgba[1], blue: rgba[2]).opacity(rgba[3])
}


func convertColorToRGBA(_ color: Color) -> [Double] {
    let uiColor = UIColor(color)
    var r: CGFloat = 0
    var g: CGFloat = 0
    var b: CGFloat = 0
    var a: CGFloat = 0
    if uiColor.getRed(&r, green: &g, blue: &b, alpha: &a) {
        return [Double(r), Double(g), Double(b), Double(a)]
    } else {
        print("Error in getting components of uicolor")
        return [255, 255, 255, 1] // white
    }
}

func colorForNormalizedScore(_ value: Double) -> Color {
    let clamped = min(max(value, 0), 1)
    // Hue 0.33 (green) at 0 -> 0.0 (red) at 1, passing through yellow/orange
    let hue = 0.33 * (1 - clamped)
    return Color(hue: hue, saturation: 0.85, brightness: 0.85)
}

func colorIndexFromCategory(_ category: Category) -> Int {
    var categoryColorDict: [Category: Int] = [:]
    var i = 0
    for category in Category.allCases {
        categoryColorDict[category] = i
        i += 1
    }
    return categoryColorDict[category]!
}

// Fuzzy match funcs
func fuzzyMatch(typed: String, correct: String) -> Bool {
    let t = _normalizeQuery(typed)
    let c = _normalizeQuery(correct)
    guard !t.isEmpty else {
        return false
    }
    if c.contains(t) || t.contains(c) {
        if c.count > 4 && t.count > 4 && abs(t.count - c.count) <= 10 { // TODO: Tune this, or at least allow user to change the default loaded card, or ensure that there's a match on qb reader or something
            return true
        }
    }
    let dist = _levenshtein(t, c)
    return dist <= max(1, c.count / 4)
}

func _normalizeQuery(_ s: String) -> String {
    var r = s.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)
    r = String(r.unicodeScalars.filter(CharacterSet.alphanumerics.contains))
    for article in ["the ", "a ", "an "] {
        if r.hasPrefix(article) {
            r = String(r.dropFirst(article.count)) // Neglects specific answerlines such as 'The Invisible Man' not being acceptable for 'Invisible Man'
        }
    }
    r = r.filter {
        $0.isLetter || $0.isNumber || $0.isWhitespace
    }
    return r.trimmingCharacters(in: .whitespaces)
}

func _levenshtein(_ a: String, _ b: String) -> Int {
    let a = Array(a), b = Array(b)
    if a.isEmpty { return b.count }
    if b.isEmpty { return a.count }
    var dp = Array(0...b.count)
    for i in 1...a.count {
        let prev = dp
        dp[0] = i
        for j in 1...b.count {
            dp[j] = a[i-1] == b[j-1] ? prev[j-1] : 1 + min(prev[j-1], prev[j], dp[j-1])
        }
    }
    return dp[b.count]
}
