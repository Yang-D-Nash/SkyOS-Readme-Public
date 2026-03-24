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

    var body: some View {
        Section {
            Button {
                showConfirmationDialog = true
            } label: {
                Text(isSubmitting ? "Sende Bestellung..." : "Bestellung abschicken")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(isFormValid && !isSubmitting ? Color.blue : Color.gray)
                    .foregroundColor(.white)
                    .cornerRadius(10)
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
