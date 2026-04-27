//
//  NicmaProducerView.swift
//  Skydown App
//
//  Created by Codex on 27.03.26.
//

import SwiftUI

struct NicmaProducerView: View {
    @EnvironmentObject private var authManager: AuthManager
    @ObservedObject private var artistPagesStore = ArtistPagesStore.shared
    let onBack: (() -> Void)?
    @State private var selectedProfile: String

    init(initialProfile: String = "NICMA STUDIO", onBack: (() -> Void)? = nil) {
        self.onBack = onBack
        _selectedProfile = State(initialValue: initialProfile)
    }

    var body: some View {
        ArtistPageView(
            authManager: authManager,
            store: artistPagesStore,
            brand: .nicma,
            artistName: selectedProfile,
            onBack: onBack,
            onNicmaProfileChange: { selectedProfile = $0 }
        )
    }
}
