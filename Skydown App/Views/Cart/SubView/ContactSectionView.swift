//
//  ContactSectionView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.08.25.
//

import SwiftUI

struct ContactSectionView: View {
    @Binding var name: String
    @Binding var email: String
    @Binding var whatsApp: String
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Section("Deine Kontaktdaten") {
            SkydownPremiumTextInput(
                title: "Name*",
                text: $name,
                colorScheme: colorScheme,
                systemImage: "person.fill",
                accent: AppColors.accent(for: colorScheme)
            )
            SkydownPremiumTextInput(
                title: "E-Mail*",
                text: $email,
                colorScheme: colorScheme,
                systemImage: "envelope.fill",
                accent: AppColors.accent(for: colorScheme),
                keyboardType: .emailAddress,
                autocapitalization: .never
            )
            SkydownPremiumTextInput(
                title: "WhatsApp Nummer (optional)",
                text: $whatsApp,
                colorScheme: colorScheme,
                systemImage: "phone.fill",
                accent: AppColors.accent(for: colorScheme),
                keyboardType: .phonePad
            )
        }
    }
}
