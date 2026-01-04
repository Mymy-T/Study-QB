//
//  MenuViews.swift
//  Study QB
//
//  Created by Milo on 1/4/26.
//

import SwiftUI


// MARK: CategorySelectionMenu
struct CategorySelectionMenu<LabelView: View>: View {
    @Binding var categories: [Category]
    let labelView: LabelView
    
    var body: some View {
        ScrollView {
            Menu {
                Section("Categories") {
                    ForEach(Category.allCases) { category in
                        CategorySelectionButton(buttonCategory: category, categories: $categories)
                    }
                }
                
                Section("Other Controls") {
                    Button("Select All") {
                        categories.removeAll()
                        for category in Category.allCases {
                            categories.append(category)
                        }
                    }
                    Button("Remove All") {
                        categories.removeAll()
                    }
                }
            } label: {
                labelView
            }
            .menuActionDismissBehavior(.disabled)
        }.fixedSize()
        .onAppear {
            if categories.count == 0 {
                for category in Category.allCases {
                    categories.append(category)
                }
            }
        }
    }
}

func defaultCategoryMenuLabel() -> some View {
    return Label("Categories", systemImage: "chevron.up.chevron.down")
        .foregroundStyle(.white)
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 10)
                .fill(Color.green.gradient)
        )
}


// MARK: CategorySelectionButton
struct CategorySelectionButton: View {
    let buttonCategory: Category
    //let showingCategoryColor: Bool
    @Binding var categories: [Category]
    
    var body: some View {
        Button {
            if categories.contains(buttonCategory) {
                categories.removeAll { category in
                    category == buttonCategory
                }
            } else {
                categories.append(buttonCategory)
            }
        } label: {
            //HStack {
                Image(systemName: categories.contains(buttonCategory) ? "checkmark.square" : "square")
                Text(buttonCategory.rawValue)
        }
    }
}

// MARK: DifficultySelectionMenu
struct DifficultySelectionMenu: View {
    @Binding var difficulties: Set<Int>
    
    var body: some View {
        ScrollView {
            Menu {
                Section("General Difficulties") {
                    Button("Middle School") {
                        difficulties.removeAll()
                        difficulties.insert(1)
                    }
                    Button("High School") {
                        difficulties.removeAll()
                        for i in 2...5 {
                            difficulties.insert(i)
                        }
                    }
                    Button("College") {
                        difficulties.removeAll()
                        for i in 6...9 {
                            difficulties.insert(i)
                        }
                    }
                }
                
                Section("Specific Difficulties") {
                    ForEach(0..<11) { difficulty in
                        DifficultySelectionButton(buttonDifficulty: difficulty, difficulties: $difficulties)
                    }
                }
                
                Section("Other Controls") {
                    Button("Select All") {
                        difficulties.removeAll()
                        for i in 0...10 {
                            difficulties.insert(i)
                        }
                    }
                    Button("Remove All") {
                        difficulties.removeAll()
                    }
                }
            } label: {
                Label("Difficulties", systemImage: "chevron.up.chevron.down")
                    .foregroundStyle(.white)
                    .padding()
                    .background(
                        RoundedRectangle(cornerRadius: 10)
                            .fill(Color.red.gradient)
                    )
            }.menuActionDismissBehavior(.disabled)
            
            
        }.fixedSize()
    }
    
}

// MARK: DifficultySelectionButton
struct DifficultySelectionButton: View {
   let buttonDifficulty: Int
   @Binding var difficulties: Set<Int>
    
    let difficultyDict: [Int: String] = [
        0: "Pop Culture",
        1: "Middle School",
        2: "Easy High School",
        3: "Regular High School",
        4: "Hard High School",
        5: "National High School",
        6: "Easy College",
        7: "Medium College",
        8: "Regionals College",
        9: "Nationals College",
        10: "Open"
    ]
    
    let buttonWidth: CGFloat = 40
   
   var body: some View {
       Button {
           if difficulties.contains(buttonDifficulty) {
               difficulties.remove(buttonDifficulty)
           } else {
               difficulties.insert(buttonDifficulty)
           }
       } label: {
           HStack {
               Image(systemName: difficulties.contains(buttonDifficulty) ? "checkmark.square" : "square")
               Text("\(buttonDifficulty): \(difficultyDict[buttonDifficulty]!)")
           }
       }
   }
}

