import SwiftUI

struct AgentView: View {
    @StateObject private var viewModel: AgentChatViewModel
    @ObservedObject private var featureFlags: FeatureFlagsService
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
    }

    private var content: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 16) {
                    if showsNavigation {
                        AgentHeroCard(
                            colorScheme: colorScheme,
                            badges: featureFlags.isAIEnabled
                                ? ["X22 Agent", "Workflow"]
                                : ["X22 Agent", "Kurz pausiert"]
                        )
                    }

                    if featureFlags.isAIEnabled {
                        if showsNavigation {
                            AgentQuickPromptCard(
                                colorScheme: colorScheme,
                                prompts: viewModel.quickPrompts,
                                onPromptSelected: viewModel.sendPrompt
                            )
                        }

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
                    } else {
                        AgentDisabledCard(colorScheme: colorScheme)
                    }
                }
            }
            .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
            .padding(.top, showsNavigation ? SkydownLayout.screenTopPadding : 8)
            .padding(.bottom, 12)
            .scrollIndicators(.hidden)
            .scrollDismissesKeyboard(.interactively)
            .simultaneousGesture(
                TapGesture().onEnded {
                    isComposerFocused = false
                }
            )
            .safeAreaInset(edge: .bottom, spacing: 0) {
                if featureFlags.isAIEnabled {
                    AgentComposerBar(
                        colorScheme: colorScheme,
                        draft: $viewModel.draft,
                        isFocused: $isComposerFocused,
                        isSending: viewModel.isSending,
                        onReset: viewModel.resetConversation,
                        onSend: viewModel.sendDraft
                    )
                }
            }
            .onChange(of: viewModel.messages.count) { _, _ in
                if featureFlags.isAIEnabled {
                    withAnimation(.easeOut(duration: 0.25)) {
                        proxy.scrollTo("agent-chat-end", anchor: .bottom)
                    }
                }
            }
            .task {
                await featureFlags.refresh()
            }
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

private struct AgentHeroCard: View {
    let colorScheme: ColorScheme
    let badges: [String]

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 10) {
                Text("Skydown x 22 Agent")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.text(for: colorScheme))
            }

            ZStack {
                Circle()
                    .fill(AppColors.accentMystic(for: colorScheme).opacity(0.16))
                    .frame(width: 58, height: 58)

                Image(systemName: "bolt.fill")
                    .font(.title2)
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
        .padding(18)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.16), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

private struct AgentQuickPromptCard: View {
    let colorScheme: ColorScheme
    let prompts: [String]
    let onPromptSelected: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Agent starten")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(prompts, id: \.self) { prompt in
                        Button(action: { onPromptSelected(prompt) }) {
                            Text(prompt)
                                .font(.subheadline.weight(.semibold))
                                .multilineTextAlignment(.leading)
                                .foregroundColor(AppColors.text(for: colorScheme))
                                .padding(.horizontal, 14)
                                .padding(.vertical, 12)
                                .frame(width: 230, alignment: .leading)
                                .background(
                                    RoundedRectangle(cornerRadius: 18)
                                        .fill(AppColors.primaryBackground(for: colorScheme).opacity(0.88))
                                )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .padding(18)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accentMystic(for: colorScheme).opacity(0.16), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

private struct AgentMessageBubble: View {
    let message: AgentChatMessage
    let colorScheme: ColorScheme

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
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .frame(maxWidth: 320, alignment: .leading)
            .background(bubbleBackground)
            .clipShape(RoundedRectangle(cornerRadius: 24))
            .overlay(
                RoundedRectangle(cornerRadius: 24)
                    .stroke(
                        isUser
                            ? Color.clear
                            : AppColors.accentMystic(for: colorScheme).opacity(0.14),
                        lineWidth: 1
                    )
            )

            if !isUser { Spacer(minLength: 48) }
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
    let isFocused: FocusState<Bool>.Binding
    let isSending: Bool
    let onReset: () -> Void
    let onSend: () -> Void
    @State private var showingComposer = false

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 10) {
                Button(action: { showingComposer = true }) {
                    Text(draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "Aufgabe" : "Aufgabe weiter")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(
                            RoundedRectangle(cornerRadius: 18)
                                .fill(AppColors.secondaryBackground(for: colorScheme))
                        )
                }
                .buttonStyle(.plain)

                Button(action: onReset) {
                    Image(systemName: "arrow.counterclockwise")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))
                        .frame(width: 42, height: 42)
                        .background(
                            Circle()
                                .fill(AppColors.secondaryBackground(for: colorScheme))
                        )
                }
                .buttonStyle(.plain)
                .disabled(isSending)
            }
            .padding(.horizontal, 16)
            .padding(.top, 14)
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
        .sheet(isPresented: $showingComposer) {
            NavigationStack {
                VStack(spacing: 16) {
                    TextField(
                        "Zum Beispiel: Release-Briefing fuer Freitag.",
                        text: $draft,
                        axis: .vertical
                    )
                    .lineLimit(4...8)
                    .focused(isFocused)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 14)
                    .background(
                        RoundedRectangle(cornerRadius: 18)
                            .fill(AppColors.secondaryBackground(for: colorScheme))
                    )
                    .foregroundColor(AppColors.text(for: colorScheme))

                    HStack {
                        Spacer()

                        Button(action: {
                            isFocused.wrappedValue = false
                            onSend()
                            showingComposer = false
                        }) {
                            Label("Senden", systemImage: "arrow.up.circle.fill")
                                .font(.headline)
                                .foregroundColor(.white)
                                .padding(.horizontal, 18)
                                .padding(.vertical, 12)
                                .background(
                                    Capsule()
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
                        }
                        .buttonStyle(.plain)
                        .disabled(draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isSending)
                        .opacity(draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isSending ? 0.6 : 1)
                    }

                    Spacer()
                }
                .padding(20)
                .background(AppColors.primaryBackground(for: colorScheme).ignoresSafeArea())
                .navigationTitle("X22 Aufgabe")
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button("Schliessen") {
                            isFocused.wrappedValue = false
                            showingComposer = false
                        }
                    }
                }
            }
            .presentationDetents([.medium, .large])
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
