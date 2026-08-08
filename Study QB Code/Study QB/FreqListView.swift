//
//  FreqListView.swift
//  Study QB
//
//  Created by Milo on 4/7/26.
//

import SwiftUI

struct FreqListView: View {
    
//    @Environment(\.dismiss) var dismiss
    
    @Binding var awaitingFreqList: Bool
    let geometry: GeometryProxy
    let freqListItems: [FreqListItem]
    let cards: [Card]

    @State private var hideExisting = true

    var displayedItems: [FreqListItem] {
        guard hideExisting else { return freqListItems }
        return freqListItems.filter { !cardExists(forAnswer: $0.answer, in: cards) }
    }

    var body: some View {
        VStack {
            Toggle("Hide cards I already have", isOn: $hideExisting)
                .frame(width: geometry.size.width*0.7)
                .padding(.bottom, 4)
            HStack {
                Text("**Answer**")
                Spacer()
                Text("**Count**")
            }.frame(width: geometry.size.width*0.7)
            Divider()
                .frame(width: geometry.size.width*0.8)
            if awaitingFreqList {
                VStack {
                    ProgressView()
                    Text("This may take a moment...")
                }
            }
            ScrollView {
                ForEach(Array(displayedItems.enumerated()), id: \.offset) { number, item in
                    HStack {
                        Text("\(number + 1)  ")
                            .fontWeight(.ultraLight)
                        Text("\(item.answer)")
                        Spacer()
                        Text("\(item.count)")
                    }.padding(.vertical, 4)
                    .frame(width: geometry.size.width*0.7)
                    Divider()
                        .frame(width: geometry.size.width*0.8)
                }
            }.scrollIndicators(.hidden)
            
            Text("Only questions with the selected difficulties (from the previous view) are shown. You can find more lists and options at https://www.qbreader.org/db/frequency-list/")
                .font(.caption)
            
        }.padding()
    }
}
