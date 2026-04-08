import SwiftUI
import UIKit

struct AgentView: View {
    @StateObject private var viewModel: AgentChatViewModel
    @ObservedObject private var featureFlags: FeatureFlagsService
    @EnvironmentObject private var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme
    @FocusState private var isComposerFocused: Bool
    private let showsNavigation: Bool

    init(
        agentChatService: AgentChatServicing = FirebaseFunctionsAgentService(),
        featureFlags: FeatureFlagsService,
        showsNavigation: Bool = true
    ) {
        self.showsNavigation = showsNavigation
        _viewModel = StateObject(
            wrappedValue: AgentChatViewModel(service: agentChatService)
        )
        _featureFlags = ObservedObject(wrappedValue: featureFlags)
    }

    var body: some View {
        Group {
            if showsNavigation {
                NavigationStack {
                    content
                        .navigationTitle("Agent")
                        .skydownNavigationChrome(colorScheme: colorScheme)
                }
            } else {
                content
            }
        }
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Fertig") {
                    isComposerFocused = false
                }
            }
        }
        .fancyToast(
            isPresented: $viewModel.showToast,
            message: viewModel.toastMessage,
            style: viewModel.toastStyle
        )
        .task {
            viewModel.configureUser(user: authManager.userSession)
        }
        .onChange(of: authManager.userSession?.id) { _, _ in
            viewModel.configureUser(user: authManager.userSession)
        }
    }

    private var content: some View {
        VStack(spacing: 0) {
            if featureFlags.isAIEnabled {
                if viewModel.messages.isEmpty {
                    VStack(alignment: .leading, spacing: 14) {
                        Spacer(minLength: showsNavigation ? 8 : 4)

                        AgentEmptyStateHeader(colorScheme: colorScheme)

                        AgentQuickPromptCard(
                            colorScheme: colorScheme,
                            prompts: viewModel.quickPrompts,
                            onPromptSelected: viewModel.sendPrompt
                        )

                        Spacer(minLength: 12)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
                    .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                    .padding(.bottom, 6)
                } else {
                    ScrollViewReader { proxy in
                        ScrollView {
                            LazyVStack(alignment: .leading, spacing: 10) {
                                ForEach(viewModel.messages) { message in
                                    AgentMessageBubble(
                                        message: message,
                                        colorScheme: colorScheme
                                    )
                                    .id(message.id)
                                }

                                Color.clear
                                    .frame(height: 4)
                                    .id("agent-chat-end")
                            }
                            .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                            .padding(.top, showsNavigation ? 6 : 2)
                            .padding(.bottom, 6)
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .scrollIndicators(.hidden)
                        .scrollDismissesKeyboard(.interactively)
                        .simultaneousGesture(
                            TapGesture().onEnded {
                                isComposerFocused = false
                            }
                        )
                        .onChange(of: viewModel.messages.count) { _, _ in
                            withAnimation(.easeOut(duration: 0.25)) {
                                proxy.scrollTo("agent-chat-end", anchor: .bottom)
                            }
                        }
                    }
                }
            } else {
                VStack {
                    Spacer(minLength: 24)
                    AgentDisabledCard(colorScheme: colorScheme)
                        .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                    Spacer()
                }
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if featureFlags.isAIEnabled {
                AgentComposerBar(
                    colorScheme: colorScheme,
                    draft: $viewModel.draft,
                    selectedMode: $viewModel.selectedMode,
                    shouldTriggerAutomation: $viewModel.shouldTriggerAutomation,
                    canTriggerAutomation: viewModel.canTriggerAutomation,
                    isFocused: $isComposerFocused,
                    isSending: viewModel.isSending,
                    onReset: viewModel.resetConversation,
                    onSend: viewModel.sendDraft
                )
            }
        }
        .task {
            await featureFlags.refresh()
        }
        .background(backgroundGradient.ignoresSafeArea())
    }

    private var backgroundGradient: LinearGradient {
        AppColors.screenGradient(
            for: colorScheme,
            secondaryAccent: AppColors.accentHighlight(for: colorScheme)
        )
    }
}

private struct AgentEmptyStateHeader: View {
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Wobei soll ich dich strukturieren?")
                .font(.system(size: 28, weight: .black, design: .rounded))
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Nutze den Agent fuer Briefings, Shotlists, Release-Plaene und klare naechste Schritte. Schreib direkt unten los oder starte mit einem Prompt.")
                .font(.subheadline.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Text("Auch der Agent fuehrt den Verlauf pro Konto weiter, damit Briefings und To-dos anschlussfaehig bleiben.")
                .font(.footnote.weight(.semibold))
                .foregroundColor(AppColors.accentMystic(for: colorScheme).opacity(0.9))
        }
    }
}

private struct AgentHeroCard: View {
    let colorScheme: ColorScheme
    let badges: [String]

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 10) {
                Text("Skydown x 22 Agent")
                    .font(.system(size: 28, weight: .black, design: .rounded))
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text("Fuer Briefings, Release-Plaene, Shotlists und naechste Schritte. Du bleibst im selben Flow, nur strukturierter.")
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            ZStack {
                Circle()
                    .fill(AppColors.accentMystic(for: colorScheme).opacity(0.16))
                    .frame(width: 54, height: 54)

                Image(systemName: "bolt.fill")
                    .font(.title3.weight(.bold))
                    .foregroundColor(AppColors.accentMystic(for: colorScheme))
            }
        }
        .padding(20)
        .background(cardBackground)
        .overlay(
            RoundedRectangle(cornerRadius: 26)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 26))
        .shadow(color: .black.opacity(colorScheme == .dark ? 0.24 : 0.08), radius: 18, y: 8)
        .overlay(alignment: .bottomLeading) {
            HStack(spacing: 10) {
                ForEach(badges, id: \.self) { badge in
                    AgentBadge(text: badge, colorScheme: colorScheme)
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 18)
        }
    }

    private var cardBackground: some View {
        LinearGradient(
            colors: [
                AppColors.cardBackground(for: colorScheme),
                AppColors.secondaryBackground(for: colorScheme).opacity(0.92)
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}

private struct AgentDisabledCard: View {
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(AppColors.accentMystic(for: colorScheme).opacity(0.14))
                        .frame(width: 50, height: 50)

                    Image(systemName: "lock.fill")
                        .foregroundColor(AppColors.accentMystic(for: colorScheme))
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text("X22 Agent pausiert")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Voruebergehend nicht verfuegbar")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.accentMystic(for: colorScheme))
                }
            }
        }
        .padding(SkydownLayout.cardPadding)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius))
    }
}

private struct AgentQuickPromptCard: View {
    let colorScheme: ColorScheme
    let prompts: [String]
    let onPromptSelected: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Schnell planen")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Starte mit einer konkreten Aufgabe und lass dir direkt Struktur bauen.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(prompts, id: \.self) { prompt in
                        Button(action: { onPromptSelected(prompt) }, label: {
                            Text(prompt)
                                .font(.subheadline.weight(.semibold))
                                .multilineTextAlignment(.leading)
                                .foregroundColor(AppColors.text(for: colorScheme))
                                .padding(.horizontal, 14)
                                .padding(.vertical, 12)
                                .frame(width: 230, alignment: .leading)
                                .background(
                                    RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius)
                                        .fill(AppColors.primaryBackground(for: colorScheme).opacity(0.88))
                                )
                        })
                        .buttonStyle(.plain)
                        .skydownTactileAction()
                    }
                }
            }
        }
        .padding(SkydownLayout.cardPadding)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius))
    }
}

private struct AgentMessageBubble: View {
    let message: AgentChatMessage
    let colorScheme: ColorScheme
    @State private var showingShareSheet = false
    @State private var copyLabel = "Kopieren"

    private var isUser: Bool {
        message.role == .user
    }

    var body: some View {
        HStack {
            if isUser { Spacer(minLength: 48) }

            VStack(alignment: .leading, spacing: 8) {
                Text(isUser ? "Du" : "X22 Agent")
                    .font(.caption.weight(.bold))
                    .foregroundColor(isUser ? .white.opacity(0.9) : AppColors.accentMystic(for: colorScheme))

                if message.isStreaming && message.text.isEmpty {
                    HStack(spacing: 10) {
                        ProgressView()
                            .tint(AppColors.accentMystic(for: colorScheme))

                        Text("X22 Agent plant gerade...")
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                } else {
                    Text(message.text)
                        .font(.body)
                        .foregroundColor(isUser ? .white : AppColors.text(for: colorScheme))

                    if !isUser {
                        HStack(spacing: 10) {
                            Button(copyLabel) {
                                UIPasteboard.general.string = message.text
                                copyLabel = "Kopiert"
                                DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) {
                                    copyLabel = "Kopieren"
                                }
                            }
                            .font(.caption.weight(.semibold))
                            .foregroundColor(AppColors.accentMystic(for: colorScheme))

                            Button("Teilen") {
                                showingShareSheet = true
                            }
                            .font(.caption.weight(.semibold))
                            .foregroundColor(AppColors.accent(for: colorScheme))
                        }
                        .padding(.top, 8)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .frame(maxWidth: 360, alignment: .leading)
            .background(bubbleBackground)
            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius))
            .overlay(
                RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                    .stroke(
                        isUser
                            ? Color.clear
                            : AppColors.accentMystic(for: colorScheme).opacity(0.14),
                        lineWidth: 1
                    )
            )

            if !isUser { Spacer(minLength: 48) }
        }
        .sheet(isPresented: $showingShareSheet) {
            AIShareSheet(activityItems: [message.text])
        }
    }

    private var bubbleBackground: some View {
        Group {
            if isUser {
                LinearGradient(
                    colors: [
                        AppColors.accent(for: colorScheme),
                        AppColors.accentMystic(for: colorScheme)
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            } else {
                LinearGradient(
                    colors: [
                        AppColors.cardBackground(for: colorScheme),
                        AppColors.secondaryBackground(for: colorScheme).opacity(0.94)
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            }
        }
    }
}

private struct AgentComposerBar: View {
    let colorScheme: ColorScheme
    @Binding var draft: String
    @Binding var selectedMode: AgentExecutionMode
    @Binding var shouldTriggerAutomation: Bool
    let canTriggerAutomation: Bool
    let isFocused: FocusState<Bool>.Binding
    let isSending: Bool
    let onReset: () -> Void
    let onSend: () -> Void

    private var trimmedDraft: String {
        draft.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var body: some View {
        VStack(spacing: 0) {
            if !AgentExecutionMode.allCases.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(AgentExecutionMode.allCases) { mode in
                            Button {
                                selectedMode = mode
                            } label: {
                                Text(mode.title)
                                    .font(.caption.weight(.bold))
                                    .foregroundColor(
                                        selectedMode == mode
                                            ? .white
                                            : AppColors.text(for: colorScheme)
                                    )
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 8)
                                    .background(
                                        Capsule()
                                            .fill(
                                                selectedMode == mode
                                                    ? AppColors.accentMystic(for: colorScheme)
                                                    : AppColors.secondaryBackground(for: colorScheme)
                                            )
                                    )
                            }
                            .buttonStyle(.plain)
                            .skydownTactileAction()
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 10)
                }
            }

            if canTriggerAutomation {
                HStack {
                    Button {
                        shouldTriggerAutomation.toggle()
                    } label: {
                        Label(
                            shouldTriggerAutomation ? "n8n aktiv" : "An n8n senden",
                            systemImage: shouldTriggerAutomation ? "bolt.fill" : "bolt.badge.clock"
                        )
                        .font(.caption.weight(.bold))
                        .foregroundColor(
                            shouldTriggerAutomation
                                ? .white
                                : AppColors.accentMystic(for: colorScheme)
                        )
                        .padding(.horizontal, 14)
                        .padding(.vertical, 9)
                        .background(
                            Capsule()
                                .fill(
                                    shouldTriggerAutomation
                                        ? AppColors.accentMystic(for: colorScheme)
                                        : AppColors.accentMystic(for: colorScheme).opacity(0.12)
                                )
                        )
                    }
                    .buttonStyle(.plain)
                    .skydownTactileAction()

                    Spacer(minLength: 0)
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
            }

            HStack(alignment: .bottom, spacing: 10) {
                TextField(
                    selectedMode.placeholder,
                    text: $draft,
                    axis: .vertical
                )
                .lineLimit(1...4)
                .focused(isFocused)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(
                    RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius)
                        .fill(AppColors.secondaryBackground(for: colorScheme))
                )
                .foregroundColor(AppColors.text(for: colorScheme))

                HStack(spacing: 8) {
                    Button(action: onReset) {
                        Image(systemName: "arrow.counterclockwise")
                            .font(.subheadline.weight(.bold))
                            .foregroundColor(AppColors.text(for: colorScheme))
                            .frame(width: 38, height: 38)
                            .background(
                                Circle()
                                    .fill(AppColors.secondaryBackground(for: colorScheme))
                            )
                    }
                    .buttonStyle(.plain)
                    .skydownTactileAction()
                    .disabled(isSending)

                    Button(action: {
                        isFocused.wrappedValue = false
                        onSend()
                    }, label: {
                        Image(systemName: "arrow.up.circle.fill")
                            .font(.title3.weight(.bold))
                            .foregroundColor(.white)
                            .frame(width: 44, height: 44)
                            .background(
                                Circle()
                                    .fill(
                                        LinearGradient(
                                            colors: [
                                                AppColors.accent(for: colorScheme),
                                                AppColors.accentMystic(for: colorScheme)
                                            ],
                                            startPoint: .leading,
                                            endPoint: .trailing
                                        )
                                    )
                            )
                    })
                    .buttonStyle(.plain)
                    .skydownTactileAction()
                    .disabled(trimmedDraft.isEmpty || isSending)
                    .opacity(trimmedDraft.isEmpty || isSending ? 0.6 : 1)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, canTriggerAutomation || !AgentExecutionMode.allCases.isEmpty ? 8 : 10)
            .padding(.bottom, 10)
            .background(
                Rectangle()
                    .fill(AppColors.primaryBackground(for: colorScheme).opacity(0.96))
                    .ignoresSafeArea()
            )
            .overlay(alignment: .top) {
                Divider().opacity(0.25)
            }
        }
    }
}

private struct AgentBadge: View {
    let text: String
    let colorScheme: ColorScheme

    var body: some View {
        Text(text)
            .font(.caption.weight(.bold))
            .foregroundColor(AppColors.accentMystic(for: colorScheme))
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                Capsule()
                    .fill(AppColors.accentMystic(for: colorScheme).opacity(0.12))
            )
    }
}
