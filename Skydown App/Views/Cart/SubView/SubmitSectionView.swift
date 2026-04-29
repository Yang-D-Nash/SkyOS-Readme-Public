//
//  SubmitSectionView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 23.08.25.
//

import SwiftUI

struct SubmitSectionView: View {
    var isFormValid: Bool
    @Binding var isSubmitting: Bool
    @Binding var showConfirmationDialog: Bool
    var submitAction: () async -> Void  // async closure
    @Environment(\.colorScheme) private var colorScheme

    private var submitEnabled: Bool {
        isFormValid && !isSubmitting
    }

    var body: some View {
        Section {
            SkydownBrandActionButton(
                title: isSubmitting ? "Sende Bestellung..." : "Bestellung abschicken",
                accent: AppColors.accent(for: colorScheme),
                colorScheme: colorScheme,
                isEnabled: submitEnabled,
                isLoading: isSubmitting,
                font: .headline,
                cornerRadius: SkydownLayout.messageBubbleRadius,
                verticalPadding: 14,
                action: { showConfirmationDialog = true }
            )
            .confirmationDialog(
                "Bestätigung senden",
                isPresented: $showConfirmationDialog,
                titleVisibility: .visible
            ) {
                Button("Einverstanden") {
                    Task {
                        await submitAction()
                    }
                }
                Button("Abbrechen", role: .cancel) {}
            } message: {
                Text("Sie werden in den nächsten Minuten per E-Mail oder WhatsApp kontaktiert.")
            }
        }
    }
}
