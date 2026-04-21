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

    var body: some View {
        HStack(alignment: .center, spacing: 14) {
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

            VStack(alignment: .leading, spacing: 4) {
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
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            RoundedRectangle(cornerRadius: 22, style: .continuous)
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
            RoundedRectangle(cornerRadius: 999, style: .continuous)
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
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
        }
        .overlay {
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .stroke(style.color.opacity(0.24), lineWidth: 1)
        }
        .shadow(color: style.color.opacity(0.18), radius: 20, y: 10)
        .skydownLuminousSweep(cornerRadius: 22, accent: style.color, alpha: 0.18)
        .accessibilityElement(children: .combine)
    }
}


#Preview {
    ToastView(message: "Profil gespeichert und direkt synchronisiert.", style: .success)
        .padding()
        .background(Color.black)
}
