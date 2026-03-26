//
//  PolicyView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 29.07.25.
//

import SwiftUI

struct PolicyView: View {
    let title: String
    let text: String

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(title)
                        .font(.title2.bold())
                        .frame(maxWidth: .infinity, alignment: .leading)

                    Text(text)
                        .font(.body)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .textSelection(.enabled)
                }
                .padding(20)
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}
