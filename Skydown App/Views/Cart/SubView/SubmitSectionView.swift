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
            Button {
                showConfirmationDialog = true
            } label: {
                Text(isSubmitting ? "Sende Bestellung..." : "Bestellung abschicken")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(
                        submitEnabled
                            ? AppColors.accent(for: colorScheme)
                            : AppColors.secondaryBackground(for: colorScheme)
                    )
                    .foregroundColor(
                        submitEnabled
                            ? .white
                            : AppColors.secondaryText(for: colorScheme)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .stroke(
                                submitEnabled
                                    ? Color.clear
                                    : AppColors.accent(for: colorScheme).opacity(0.12),
                                lineWidth: 1
                            )
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            }
            .disabled(!isFormValid || isSubmitting)
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
