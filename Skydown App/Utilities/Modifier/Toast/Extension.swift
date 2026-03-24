//
//  Extension.swift
//  Skydown App
//
//  Created by Yang D. Nash on 22.08.25.
//

import SwiftUI

extension View {
    func fancyToast(isPresented: Binding<Bool>, message: String, style: ToastStyle = .info) -> some View {
        self.modifier(ToastModifier(isPresented: isPresented, message: message, style: style))
    }
}
