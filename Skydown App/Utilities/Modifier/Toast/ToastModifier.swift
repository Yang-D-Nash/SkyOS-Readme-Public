//
//  ToastModifier.swift
//  Skydown App
//
//  Created by Yang D. Nash on 22.08.25.
//

import SwiftUI

struct ToastModifier: ViewModifier {
    @Binding var isPresented: Bool
    let message: String
    let style: ToastStyle

    func body(content: Content) -> some View {
        ZStack(alignment: .bottom) {
            content

            if isPresented {
                ToastView(message: message, style: style)
                    .padding(.horizontal, 14)
                    .padding(.bottom, 18)
                    .transition(
                        .move(edge: .bottom)
                        .combined(with: .opacity)
                        .combined(with: .scale(scale: 0.96, anchor: .bottom))
                    )
                .animation(SkydownMotion.statusTransition, value: isPresented)
                .task(id: message + style.title) {
                    SkydownHaptics.announce(message)
                    #if canImport(UIKit)
                    switch style {
                    case .success:
                        SkydownHaptics.notification(.success)
                    case .warning:
                        SkydownHaptics.notification(.warning)
                    case .error:
                        SkydownHaptics.notification(.error)
                    case .info:
                        SkydownHaptics.impact(.soft)
                    }
                    #endif

                    let duration = min(max(2.4, Double(message.count) / 22.0), 5.2)
                    try? await Task.sleep(for: .seconds(duration))
                    guard !Task.isCancelled else { return }
                    await MainActor.run {
                        withAnimation(SkydownMotion.statusTransition) {
                            isPresented = false
                        }
                    }
                }
            }
        }
    }
}
