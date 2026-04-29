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
        self.onRefreshChat = onRefreshChat
        self.onDeleteChat = onDeleteChat
    }

    var body: some View {
        HStack(spacing: SkydownLayout.stackSpacingSnug) {
            Button(action: onOpenSessions) {
                HStack(spacing: SkydownLayout.stackSpacingPill) {
                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingHairline) {
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
                    RoundedRectangle(cornerRadius: SkydownLayout.sheetHeroRadius, style: .continuous)
                        .stroke(accent.opacity(0.14), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.sheetHeroRadius, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(isBusy)

            if showsManagementActions {
                AIConversationToolbarButton(
                    systemName: "arrow.clockwise",
                    title: AppLocalized.text("ai.sessions.a11y.refresh", fallback: "Refresh chat"),
                    accent: accent,
                    colorScheme: colorScheme,
                    action: onRefreshChat
                )
                .disabled(isBusy)
            }

            if showsManagementActions {
                AIConversationToolbarButton(
                    systemName: "trash",
                    title: AppLocalized.text("ai.sessions.a11y.delete", fallback: "Delete chat"),
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
    let onSelectSession: (UUID) -> Void
    let onRenameActiveSession: () -> Void
    let onDeleteActiveSession: () -> Void

    private var activeSession: AIScriptHistorySessionSummary? {
        sessions.first { $0.id == activeSessionID }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingSection) {
                    if let activeSession {
                        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                            Text(AppLocalized.text("ai.sessions.active_chat", fallback: "Active chat"))
                                .font(.caption2.weight(.black))
                                .foregroundColor(accent)

                            TextField(activeSession.title, text: $renameDraft)
                                .textInputAutocapitalization(.sentences)
                                .disableAutocorrection(true)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 12)
                                .background(AppColors.secondaryBackground(for: colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous))

                            HStack(spacing: SkydownLayout.stackSpacingPill) {
                                Button(role: .destructive, action: onDeleteActiveSession) {
                                    Text(AppLocalized.text("ai.sessions.delete", fallback: "Delete"))
                                        .font(.caption.weight(.bold))
                                }
                                .disabled(isBusy)

                                Spacer(minLength: 0)

                                Button(action: onRenameActiveSession) {
                                    Text(AppLocalized.text("ai.sessions.rename", fallback: "Rename"))
                                        .font(.caption.weight(.bold))
                                }
                                .disabled(isBusy || renameDraft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                            }
                        }
                    }

                    VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingPill) {
                        Text(AppLocalized.text("ai.sessions.list_title", fallback: "Chats"))
                            .font(.caption2.weight(.black))
                            .foregroundColor(accent)

                        if sessions.isEmpty {
                            Text(AppLocalized.text("ai.sessions.empty", fallback: "No history yet."))
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
            return AppLocalized.text("ai.sessions.status.new", fallback: "New")
        case 1:
            return AppLocalized.text("ai.sessions.status.one_request", fallback: "1 request")
        default:
            return String(
                format: AppLocalized.text("ai.sessions.status.n_requests", fallback: "%d requests"),
                session.promptCount
            )
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: SkydownLayout.stackSpacingDense) {
            Text(session.title)
                .font(.subheadline.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))
                .lineLimit(1)

            Text(
                session.preview.isEmpty
                    ? AppLocalized.text("ai.sessions.preview.empty", fallback: "No reply in this chat yet.")
                    : session.preview
            )
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
                .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous))
        )
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.elevatedPanelRadius, style: .continuous)
                .stroke(isSelected ? accent.opacity(0.16) : Color.clear, lineWidth: 1)
        )
    }
}
