//
//  Functions.swift
//  Study QB
//
//  Created by Milo on 11/14/25.
//

import Foundation
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

func colorIndexFromCategory(_ category: Category) -> Int {
    var categoryColorDict: [Category: Int] = [:]
    var i = 0
    for category in Category.allCases {
        categoryColorDict[category] = i
        i += 1
    }
    return categoryColorDict[category]!
}
