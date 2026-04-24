//
//  HelpView.swift
//  Study QB
//
//  Created by Milo on 1/2/26.
//

import SwiftUI

struct HelpView: View {
    
    @Environment(\.dismiss) var dismiss
    
    let minScale = 0.3
    
    var body: some View {
        VStack(alignment: .center) {
            Spacer()
            Text("Usage Details")
                .font(.largeTitle)
                .bold()
            Spacer()
            VStack(spacing: 30) {
                Text("In the 'Analyze' tab, you can input a keyword or answerline, and the app will try to find common clues in quizbowl questions with the entered answerline. The quizbowl questions are obtained from the QBReader website at www.qbreader.org. You can select certain categories to narrow your search, as well as the difficulties that best suit your needs. Please make sure that the keyword or answerline you input exactly matches the desired answerline (don't have typos or spelling errors). After a response is generated, you can save it to a card for later review.")
                    .minimumScaleFactor(minScale)
                
                Text("In the 'Manage Cards' tab, you can view all of your saved and created cards. You can click on a card to view its details and any personal notes you have left for yourself when reviewing. Long press on a card to delete it.")
                    .minimumScaleFactor(minScale)
                
                Text("In the 'Review Cards' tab, you will be able to review the cards you have saved. You can customize your reviewing session. For example, you can select specific cards to review.")
                    .minimumScaleFactor(minScale)
            }
            Spacer()
            Button {
                dismiss()
            } label: {
                Text("Got it!")
                    .foregroundStyle(.white)
                    .bold()
                    .padding()
                    .background(
                        RoundedRectangle(cornerRadius: 10)
                            .fill(Color.blue.gradient)
                    )
            }
        }.padding()
    }
}
