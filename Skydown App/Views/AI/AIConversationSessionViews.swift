import SwiftUI

struct AIConversationSessionStrip: View {
    let title: String
    let subtitle: String
    let accent: Color
    let colorScheme: ColorScheme
    let isBusy: Bool
    let canDelete: Bool
    let showsManagementActions: Bool
    let onOpenSessions: () -> Void
    let onCreateNewChat: () -> Void
    let onRefreshChat: () -> Void
    let onDeleteChat: () -> Void

    init(
        title: String,
        subtitle: String,
        accent: Color,
        colorScheme: ColorScheme,
        isBusy: Bool,
        canDelete: Bool = false,
        showsManagementActions: Bool = false,
        onOpenSessions: @escaping () -> Void,
        onCreateNewChat: @escaping () -> Void,
        onRefreshChat: @escaping () -> Void = { },
        onDeleteChat: @escaping () -> Void = { }
    ) {
        self.title = title
        self.subtitle = subtitle
        self.accent = accent
        self.colorScheme = colorScheme
        self.isBusy = isBusy
        self.canDelete = canDelete
        self.showsManagementActions = showsManagementActions
        self.onOpenSessions = onOpenSessions
        self.onCreateNewChat = onCreateNewChat
        self.onRefreshChat = onRefreshChat
        self.onDeleteChat = onDeleteChat
    }

    var body: some View {
        HStack(spacing: 9) {
            Button(action: onOpenSessions) {
                HStack(spacing: 10) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(title)
                            .font(.subheadline.weight(.bold))
                            .foregroundColor(AppColors.text(for: colorScheme))
                            .lineLimit(1)
                        Text(subtitle)
                            .font(.caption.weight(.semibold))
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                            .lineLimit(1)
                    }

                    Spacer(minLength: 0)

                    Image(systemName: "chevron.down")
                        .font(.caption.weight(.bold))
                        .foregroundColor(accent)
                }
                .padding(.horizontal, 15)
                .padding(.vertical, 13)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    LinearGradient(
                        colors: [
                            AppColors.cardBackground(for: colorScheme).opacity(0.96),
                            AppColors.secondaryBackground(for: colorScheme).opacity(0.86)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 26, style: .continuous)
                        .stroke(accent.opacity(0.14), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(isBusy)

            if showsManagementActions {
                AIConversationToolbarButton(
                    systemName: "arrow.clockwise",
                    title: "Chat aktualisieren",
                    accent: accent,
                    colorScheme: colorScheme,
                    action: onRefreshChat
                )
                .disabled(isBusy)
            }

            AIConversationToolbarButton(
                systemName: "plus",
                title: "Neuer Chat",
                accent: accent,
                colorScheme: colorScheme,
                action: onCreateNewChat
            )
            .disabled(isBusy)

            if showsManagementActions {
                AIConversationToolbarButton(
                    systemName: "trash",
                    title: "Chat loeschen",
                    accent: .red,
                    isDestructive: true,
                    colorScheme: colorScheme,
                    action: onDeleteChat
                )
                .disabled(isBusy || !canDelete)
                .opacity(canDelete ? 1 : 0.46)
            }
        }
    }
}

private struct AIConversationToolbarButton: View {
    let systemName: String
    let title: String
    let accent: Color
    var isDestructive: Bool = false
    let colorScheme: ColorScheme
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.subheadline.weight(.black))
                .foregroundColor(isDestructive ? .red : AppColors.text(for: colorScheme))
                .frame(width: 46, height: 46)
                .background(.ultraThinMaterial)
                .overlay(
                    Circle()
                        .stroke(accent.opacity(0.14), lineWidth: 1)
                )
                .clipShape(Circle())
        }
        .buttonStyle(.plain)
        .skydownTactileAction()
        .accessibilityLabel(title)
    }
}

struct AIConversationSessionsSheet: View {
    let title: String
    let accent: Color
    let colorScheme: ColorScheme
    let sessions: [AIScriptHistorySessionSummary]
    let activeSessionID: UUID?
    let isBusy: Bool
    @Binding var renameDraft: String
    let onCreateNewChat: () -> Void
    let onSelectSession: (UUID) -> Void
    let onRenameActiveSession: () -> Void
    let onDeleteActiveSession: () -> Void

    private var activeSession: AIScriptHistorySessionSummary? {
        sessions.first { $0.id == activeSessionID }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    Button(action: onCreateNewChat) {
                        HStack(spacing: 10) {
                            Image(systemName: "plus.circle.fill")
                                .font(.subheadline.weight(.bold))
                            Text("Neuer Chat")
                                .font(.subheadline.weight(.bold))
                            Spacer(minLength: 0)
                        }
                        .foregroundColor(accent)
                        .padding(15)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(AppColors.secondaryBackground(for: colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .disabled(isBusy)

                    if let activeSession {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("Aktiver Chat")
                                .font(.caption2.weight(.black))
                                .foregroundColor(accent)

                            TextField(activeSession.title, text: $renameDraft)
                                .textInputAutocapitalization(.sentences)
                                .disableAutocorrection(true)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 12)
                                .background(AppColors.secondaryBackground(for: colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))

                            HStack(spacing: 10) {
                                Button(role: .destructive, action: onDeleteActiveSession) {
                                    Text("Loeschen")
                                        .font(.caption.weight(.bold))
                                }
                                .disabled(isBusy)

                                Spacer(minLength: 0)

                                Button(action: onRenameActiveSession) {
                                    Text("Umbenennen")
                                        .font(.caption.weight(.bold))
                                }
                                .disabled(isBusy || renameDraft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                            }
                        }
                    }

                    VStack(alignment: .leading, spacing: 10) {
                        Text("Chats")
                            .font(.caption2.weight(.black))
                            .foregroundColor(accent)

                        if sessions.isEmpty {
                            Text("Noch kein Verlauf.")
                                .font(.subheadline.weight(.semibold))
                                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                        } else {
                            ForEach(sessions) { session in
                                Button(action: { onSelectSession(session.id) }) {
                                    AIConversationSessionRow(
                                        session: session,
                                        isSelected: session.id == activeSessionID,
                                        accent: accent,
                                        colorScheme: colorScheme
                                    )
                                }
                                .buttonStyle(.plain)
                                .disabled(isBusy)
                            }
                        }
                    }
                }
                .padding(.horizontal, 18)
                .padding(.top, 18)
                .padding(.bottom, 28)
            }
            .background(AppColors.primaryBackground(for: colorScheme).ignoresSafeArea())
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

private struct AIConversationSessionRow: View {
    let session: AIScriptHistorySessionSummary
    let isSelected: Bool
    let accent: Color
    let colorScheme: ColorScheme

    private var countLabel: String {
        switch session.promptCount {
        case 0:
            return "Neu"
        case 1:
            return "1 Anfrage"
        default:
            return "\(session.promptCount) Anfragen"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(session.title)
                .font(.subheadline.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))
                .lineLimit(1)

            Text(session.preview.isEmpty ? "Noch keine Antwort in diesem Chat." : session.preview)
                .font(.caption.weight(.semibold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                .multilineTextAlignment(.leading)
                .lineLimit(2)

            Text(countLabel)
                .font(.caption2.weight(.bold))
                .foregroundColor(isSelected ? accent : AppColors.secondaryText(for: colorScheme))
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            (isSelected ? accent.opacity(0.08) : AppColors.secondaryBackground(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(isSelected ? accent.opacity(0.16) : Color.clear, lineWidth: 1)
        )
    }
}
