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

    var body: some View {
        Section("Deine Kontaktdaten") {
            TextField("Name*", text: $name)
            TextField("E-Mail*", text: $email)
                .keyboardType(.emailAddress)
                .textInputAutocapitalization(.never)
            TextField("WhatsApp Nummer (optional)", text: $whatsApp)
                .keyboardType(.phonePad)
        }
    }
}
