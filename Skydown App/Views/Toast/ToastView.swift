//
//  ToastView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 22.08.25.
//

import SwiftUI

struct ToastView: View {
    let message: String
    let style: ToastStyle
    let onDismiss: (() -> Void)? = nil

    var body: some View {
        HStack(alignment: .center, spacing: SkydownLayout.stackSpacingRelaxed) {
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [
                                style.color.opacity(0.96),
                                style.secondaryColor.opacity(0.96)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )

                Image(systemName: style.icon)
                    .font(.headline.weight(.black))
                    .foregroundColor(.white)
            }
            .frame(width: 42, height: 42)

            VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingNano) {
                Text(style.title)
                    .font(.caption.weight(.bold))
                    .textCase(.uppercase)
                    .tracking(1.0)
                    .foregroundColor(.white.opacity(0.78))

                Text(message)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(.white)
                    .multilineTextAlignment(.leading)
                    .lineLimit(3)
            }

            Spacer(minLength: 0)

            if let onDismiss {
                Button {
                    onDismiss()
                } label: {
                    Image(systemName: "xmark")
                        .font(.caption.weight(.bold))
                        .foregroundColor(.white.opacity(0.92))
                        .frame(width: 28, height: 28)
                        .background(
                            Circle()
                                .fill(Color.white.opacity(0.14))
                        )
                        .overlay(
                            Circle()
                                .stroke(Color.white.opacity(0.18), lineWidth: 1)
                        )
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Hinweis schliessen")
                .skydownInteractiveFeedback()
            }
        }
        .padding(.horizontal, SkydownLayout.cardPadding)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [
                            style.secondaryColor.opacity(0.98),
                            Color.black.opacity(0.84),
                            style.color.opacity(0.24)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
        }
        .overlay(alignment: .leading) {
            RoundedRectangle(cornerRadius: SkydownLayout.fullCapsuleRadius, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [style.color, style.color.opacity(0.14)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
                .frame(width: 5)
                .padding(.vertical, 12)
                .padding(.leading, 9)
        }
        .overlay(alignment: .topTrailing) {
            Circle()
                .fill(style.color.opacity(0.22))
                .frame(width: 84, height: 84)
                .blur(radius: 16)
                .offset(x: 12, y: -14)
        }
        .overlay(alignment: .bottomLeading) {
            Capsule(style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [style.color.opacity(0.92), .white.opacity(0.56)],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .frame(height: 3)
                .padding(.horizontal, SkydownLayout.cardPadding)
                .padding(.bottom, 8)
        }
        .overlay {
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius, style: .continuous)
                .stroke(style.color.opacity(0.24), lineWidth: 1)
        }
        .shadow(color: style.color.opacity(0.18), radius: 20, y: 10)
        .skydownLuminousSweep(cornerRadius: SkydownLayout.cardCornerRadius, accent: style.color, alpha: 0.18)
        .onTapGesture {
            onDismiss?()
        }
        .accessibilityAction(.escape) {
            onDismiss?()
        }
        .accessibilityElement(children: .combine)
    }
}


#Preview {
    ToastView(message: "Profil gespeichert und direkt synchronisiert.", style: .success, onDismiss: nil)
        .padding()
        .background(Color.black)
}
