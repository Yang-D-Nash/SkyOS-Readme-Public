//
//  MusicSharedComponents.swift
//  Skydown App
//
//  Created by Codex on 27.03.26.
//

import SwiftUI
import UIKit

struct MusicBadge: View {
    let text: String
    let isAccent: Bool
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Text(text)
            .font(.caption.weight(.semibold))
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(
                isAccent
                ? AppColors.accent(for: colorScheme).opacity(0.15)
                : AppColors.secondaryBackground(for: colorScheme)
            )
            .foregroundColor(
                isAccent
                ? AppColors.accent(for: colorScheme)
                : AppColors.secondaryText(for: colorScheme)
            )
            .clipShape(Capsule())
    }
}

struct NicmaProducerSpotlightCard: View {
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top, spacing: 14) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("NICMA MUSIC")
                        .font(.title2.weight(.bold))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Producer-Seite mit Preisliste fuer Mixing, Mastering und Recording.")
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer()

                ZStack {
                    Circle()
                        .fill(AppColors.accentMystic(for: colorScheme).opacity(0.16))
                        .frame(width: 54, height: 54)

                    Image(systemName: "waveform.path.ecg")
                        .font(.title3)
                        .foregroundColor(AppColors.accentMystic(for: colorScheme))
                }
            }

            HStack(spacing: 8) {
                MusicBadge(text: "Mixing", isAccent: true)
                MusicBadge(text: "Mastering", isAccent: false)
                MusicBadge(text: "Studio Services", isAccent: false)
            }

            HStack {
                Text("Preise & Services oeffnen")
                    .font(.headline)
                    .foregroundColor(AppColors.accent(for: colorScheme))

                Spacer()

                Image(systemName: "arrow.right.circle.fill")
                    .font(.title3)
                    .foregroundColor(AppColors.accent(for: colorScheme))
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [
                    AppColors.cardBackground(for: colorScheme),
                    AppColors.secondaryBackground(for: colorScheme).opacity(0.92)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

struct BeatHubSpotlightCard: View {
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top, spacing: 14) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Beat Hub")
                        .font(.title2.weight(.bold))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Eigener Bereich fuer Beat-Uploads, oeffentliche Hubsounds und Admin-Freigaben.")
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer()

                ZStack {
                    Circle()
                        .fill(AppColors.accent(for: colorScheme).opacity(0.16))
                        .frame(width: 54, height: 54)

                    Image(systemName: "speaker.wave.3.fill")
                        .font(.title3)
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }
            }

            HStack(spacing: 8) {
                MusicBadge(text: "Upload", isAccent: true)
                MusicBadge(text: "Listen", isAccent: false)
                MusicBadge(text: "Admin Review", isAccent: false)
            }

            HStack {
                Text("Beat Hub oeffnen")
                    .font(.headline)
                    .foregroundColor(AppColors.accent(for: colorScheme))

                Spacer()

                Image(systemName: "arrow.right.circle.fill")
                    .font(.title3)
                    .foregroundColor(AppColors.accent(for: colorScheme))
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [
                    AppColors.cardBackground(for: colorScheme),
                    AppColors.secondaryBackground(for: colorScheme).opacity(0.92)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

struct VideoHubSpotlightCard: View {
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top, spacing: 14) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Videography")
                        .font(.title2.weight(.bold))
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Eigener Bereich fuer Skydown x 22 Videos mit Playback, Admin-Uploads und klaren Format-Hinweisen.")
                        .font(.subheadline)
                        .foregroundColor(AppColors.secondaryText(for: colorScheme))
                }

                Spacer()

                ZStack {
                    Circle()
                        .fill(AppColors.accentMystic(for: colorScheme).opacity(0.16))
                        .frame(width: 54, height: 54)

                    Image(systemName: "video.fill")
                        .font(.title3)
                        .foregroundColor(AppColors.accentMystic(for: colorScheme))
                }
            }

            HStack(spacing: 8) {
                MusicBadge(text: "Playback", isAccent: true)
                MusicBadge(text: "Admin Upload", isAccent: false)
                MusicBadge(text: "MP4 / MOV / M4V", isAccent: false)
            }

            HStack {
                Text("Videography oeffnen")
                    .font(.headline)
                    .foregroundColor(AppColors.accent(for: colorScheme))

                Spacer()

                Image(systemName: "arrow.right.circle.fill")
                    .font(.title3)
                    .foregroundColor(AppColors.accent(for: colorScheme))
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [
                    AppColors.cardBackground(for: colorScheme),
                    AppColors.secondaryBackground(for: colorScheme).opacity(0.92)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

struct MusicInstagramHubCard: View {
    let selectedArtist: String
    let destinations: [MusicInstagramDestination]
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Instagram")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Hol dir mehr Vibe direkt ueber den aktuellen Artist, 22 und Skydown.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            ForEach(destinations) { destination in
                if let url = destination.url {
                    Link(destination: url) {
                        HStack(spacing: 12) {
                            ZStack {
                                Circle()
                                    .fill(
                                        LinearGradient(
                                            colors: [
                                                AppColors.instagramStart(for: colorScheme),
                                                AppColors.instagramEnd(for: colorScheme)
                                            ],
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        )
                                    )
                                    .frame(width: 42, height: 42)

                                Image(systemName: "camera.fill")
                                    .font(.footnote.weight(.bold))
                                    .foregroundColor(.white)
                            }

                            VStack(alignment: .leading, spacing: 4) {
                                Text(destination.title)
                                    .font(.headline)
                                    .foregroundColor(AppColors.text(for: colorScheme))
                                    .frame(maxWidth: .infinity, alignment: .leading)

                                Text(destination.subtitle(selectedArtist: selectedArtist))
                                    .font(.subheadline)
                                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(
                            LinearGradient(
                                colors: [
                                    AppColors.secondaryBackground(for: colorScheme),
                                    AppColors.instagramStart(for: colorScheme).opacity(colorScheme == .dark ? 0.10 : 0.06),
                                    AppColors.instagramEnd(for: colorScheme).opacity(colorScheme == .dark ? 0.08 : 0.05)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 18)
                                .stroke(AppColors.instagramStart(for: colorScheme).opacity(0.14), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                    }
                    .buttonStyle(.plain)
                    .skydownTactileAction()
                }
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(AppColors.cardBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
    }
}

struct NicmaProducerPriceCard: View {
    let package: NicmaProducerPackage
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .top) {
                Text(package.title)
                    .font(.headline)
                    .foregroundColor(AppColors.text(for: colorScheme))

                Spacer()

                Text(package.price)
                    .font(.headline.weight(.bold))
                    .foregroundColor(AppColors.accent(for: colorScheme))
            }

            Text(package.detail)
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

struct NicmaUploadField: View {
    let title: String
    @Binding var text: String
    let colorScheme: ColorScheme
    var keyboard: UIKeyboardType = .default
    var autocapitalization: TextInputAutocapitalization = .sentences

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(AppColors.text(for: colorScheme))

            TextField(title, text: $text)
                .keyboardType(keyboard)
                .textInputAutocapitalization(autocapitalization)
                .padding(.horizontal, 14)
                .padding(.vertical, 14)
                .background(AppColors.secondaryBackground(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 18))
                .overlay(
                    RoundedRectangle(cornerRadius: 18)
                        .stroke(AppColors.accent(for: colorScheme).opacity(0.12), lineWidth: 1)
                )
        }
    }
}

struct NicmaSelectedFileRow: View {
    let file: NicmaSelectedFile
    let colorScheme: ColorScheme
    let onRemove: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(AppColors.accentMystic(for: colorScheme).opacity(0.16))
                    .frame(width: 42, height: 42)

                Image(systemName: "waveform")
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(file.fileName)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.text(for: colorScheme))
                    .lineLimit(1)

                Text(ByteCountFormatter.string(fromByteCount: file.fileSizeInBytes, countStyle: .file))
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            Spacer()

            Button(role: .destructive, action: onRemove) {
                Image(systemName: "xmark.circle.fill")
                    .font(.title3)
            }
        }
        .padding(12)
        .background(AppColors.secondaryBackground(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

struct NicmaProducerPackage: Identifiable {
    let id = UUID()
    let title: String
    let detail: String
    let price: String
}

extension View {
    func skydownKeyboardDismissToolbar() -> some View {
        toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Fertig") {
                    UIApplication.shared.skydownDismissKeyboard()
                }
            }
        }
    }

    func skydownDismissKeyboardOnTap() -> some View {
        simultaneousGesture(
            TapGesture().onEnded {
                UIApplication.shared.skydownDismissKeyboard()
            }
        )
    }
}

private extension UIApplication {
    func skydownDismissKeyboard() {
        sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }
}

struct MusicInstagramDestination: Identifiable {
    let id: String
    let title: String
    let handle: String
    let urlString: String
    let helper: String?

    var url: URL? {
        URL(string: urlString)
    }

    func subtitle(selectedArtist: String) -> String {
        if let helper {
            return "\(handle) • \(helper)"
        }

        if title == selectedArtist {
            return "\(handle) • Artist aktuell ausgewaehlt"
        }

        return handle
    }
}

let artistInstagramDestinations: [String: MusicInstagramDestination] = [
    "Yang D. Nash": MusicInstagramDestination(
        id: "artist_yang_d_nash",
        title: "Yang D. Nash",
        handle: "@y.d.nash",
        urlString: "https://www.instagram.com/y.d.nash/",
        helper: "Artist aktuell ausgewaehlt"
    ),
    "MAVE": MusicInstagramDestination(
        id: "artist_mave",
        title: "MAVE",
        handle: "@mave__official",
        urlString: "https://www.instagram.com/mave__official/",
        helper: "Artist aktuell ausgewaehlt"
    ),
    "ThaDude": MusicInstagramDestination(
        id: "artist_thadude",
        title: "ThaDude",
        handle: "@thadude_offizielle",
        urlString: "https://www.instagram.com/thadude_offizielle/",
        helper: "Artist aktuell ausgewaehlt"
    ),
    "Toprack941": MusicInstagramDestination(
        id: "artist_toprack941",
        title: "Toprack941",
        handle: "@toprack_941",
        urlString: "https://www.instagram.com/toprack_941/",
        helper: "Artist aktuell ausgewaehlt"
    ),
    "TANGAJOE007": MusicInstagramDestination(
        id: "artist_tangajoe007",
        title: "TANGAJOE007",
        handle: "@tangajoe007",
        urlString: "https://www.instagram.com/tangajoe007/",
        helper: "Artist aktuell ausgewaehlt"
    ),
    "JANNO": MusicInstagramDestination(
        id: "artist_janno",
        title: "JANNO",
        handle: "@janno_official_",
        urlString: "https://www.instagram.com/janno_official_/",
        helper: "Artist aktuell ausgewaehlt"
    )
]

let zweizweiInstagramDestination = MusicInstagramDestination(
    id: "brand_22_music",
    title: "22 Music",
    handle: "@zweizwei_music",
    urlString: "https://www.instagram.com/zweizwei_music/",
    helper: "Skydown x 22 Universe"
)

let skydownMusicInstagramDestination = MusicInstagramDestination(
    id: "brand_skydown",
    title: "Skydown Entertainment",
    handle: "@skydown_entertainment",
    urlString: "https://www.instagram.com/skydown_entertainment/",
    helper: "Label und Releases"
)

let nicmaInstagramDestination = MusicInstagramDestination(
    id: "brand_nicma_music",
    title: "NICMA MUSIC",
    handle: "@nicma.music",
    urlString: "https://www.instagram.com/nicma.music/",
    helper: "Producer und Studio"
)

let nicmaProducerPackages: [NicmaProducerPackage] = [
    NicmaProducerPackage(
        title: "Mixing",
        detail: "max. 24 Audio Files",
        price: "150 €"
    ),
    NicmaProducerPackage(
        title: "Mastering",
        detail: "2 stems",
        price: "70 €"
    ),
    NicmaProducerPackage(
        title: "Mastering",
        detail: "max. 5 stems",
        price: "90 €"
    ),
    NicmaProducerPackage(
        title: "Mixing + Mastering",
        detail: "max. 24 Audio Files",
        price: "200 €"
    ),
    NicmaProducerPackage(
        title: "Track Recording ohne Mix / Master",
        detail: "Recording Session",
        price: "120 €"
    ),
    NicmaProducerPackage(
        title: "Track Recording inkl. Mix / Master",
        detail: "Kompletter Recording-Flow",
        price: "250 €"
    ),
    NicmaProducerPackage(
        title: "8h Studio Zeit + Engineer",
        detail: "zzgl. Nachbearbeitung",
        price: "400 € + Nachbearbeitung"
    )
]
