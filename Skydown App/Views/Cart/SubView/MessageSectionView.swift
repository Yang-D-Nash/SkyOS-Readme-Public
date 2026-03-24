//
//  MessageSectionView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.08.25.
//

import SwiftUI

struct MessageSectionView: View {
    @Binding var message: String

    var body: some View {
        Section("Nachricht (optional)") {
            TextEditor(text: $message)
                .frame(minHeight: 100)
        }
    }
}
