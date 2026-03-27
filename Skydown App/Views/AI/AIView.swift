import SwiftUI

struct AIView: View {
    @StateObject private var viewModel: AIChatViewModel
    @ObservedObject private var featureFlags: FeatureFlagsService
    @Environment(\.colorScheme) private var colorScheme
    @FocusState private var isComposerFocused: Bool
    private let showsNavigation: Bool

    init(
        aiChatService: AIChatServicing = FirebaseAIChatService(),
        featureFlags: FeatureFlagsService,
        showsNavigation: Bool = true
    ) {
        self.showsNavigation = showsNavigation
        _viewModel = StateObject(
            wrappedValue: AIChatViewModel(service: aiChatService)
        )
        _featureFlags = ObservedObject(wrappedValue: featureFlags)
    }

    var body: some View {
        Group {
            if showsNavigation {
                NavigationStack {
                    content
                        .navigationTitle("Bot")
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
                        AIHeroCard(
                            colorScheme: colorScheme,
                            badges: featureFlags.isAIEnabled
                                ? ["X22 Bot", "Creative Assist"]
                                : ["X22 Bot", "Kurz pausiert"]
                        )
                    }

                    if featureFlags.isAIEnabled {
                        if showsNavigation {
                            AIQuickPromptCard(
                                colorScheme: colorScheme,
                                prompts: viewModel.quickPrompts,
                                onPromptSelected: viewModel.sendPrompt
                            )

                            AIVisualPromptCard(
                                colorScheme: colorScheme,
                                prompts: viewModel.visualPrompts,
                                onPromptSelected: viewModel.generateVisual
                            )
                        }

                        ForEach(viewModel.messages) { message in
                            AIMessageBubble(
                                message: message,
                                colorScheme: colorScheme
                            )
                            .id(message.id)
                        }

                        Color.clear
                            .frame(height: 4)
                            .id("chat-end")
                    } else {
                        AIDisabledCard(colorScheme: colorScheme)
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, showsNavigation ? 20 : 8)
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
                    AIComposerBar(
                        colorScheme: colorScheme,
                        draft: $viewModel.draft,
                        composerMode: $viewModel.composerMode,
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
                        proxy.scrollTo("chat-end", anchor: .bottom)
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
        LinearGradient(
            colors: [
                AppColors.primaryBackground(for: colorScheme),
                AppColors.accent(for: colorScheme).opacity(0.16),
                AppColors.accentMystic(for: colorScheme).opacity(0.12),
                AppColors.primaryBackground(for: colorScheme)
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }
}

private struct AIHeroCard: View {
    let colorScheme: ColorScheme
    let badges: [String]

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 10) {
                Text("Skydown x 22 Bot")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.text(for: colorScheme))
            }

            ZStack {
                Circle()
                    .fill(AppColors.accent(for: colorScheme).opacity(0.16))
                    .frame(width: 58, height: 58)

                Image(systemName: "sparkles")
                    .font(.title2)
                    .foregroundColor(AppColors.accent(for: colorScheme))
            }
        }
        .padding(20)
        .background(cardBackground)
        .overlay(
            RoundedRectangle(cornerRadius: 26)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.18), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 26))
        .shadow(color: .black.opacity(colorScheme == .dark ? 0.24 : 0.08), radius: 18, y: 8)
        .overlay(alignment: .bottomLeading) {
            HStack(spacing: 10) {
                ForEach(badges, id: \.self) { badge in
                    AIBadge(text: badge, colorScheme: colorScheme)
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

private struct AIDisabledCard: View {
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(AppColors.accent(for: colorScheme).opacity(0.14))
                        .frame(width: 50, height: 50)

                    Image(systemName: "lock.fill")
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text("X22 Bot pausiert")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Voruebergehend nicht verfuegbar")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }
            }
        }
        .padding(18)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.16), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

private struct AIQuickPromptCard: View {
    let colorScheme: ColorScheme
    let prompts: [String]
    let onPromptSelected: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Bot starten")
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
                .stroke(AppColors.accent(for: colorScheme).opacity(0.16), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

private struct AIVisualPromptCard: View {
    let colorScheme: ColorScheme
    let prompts: [AIVisualPrompt]
    let onPromptSelected: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Visuals")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(prompts) { prompt in
                        Button(action: { onPromptSelected(prompt.prompt) }) {
                            Text(prompt.label)
                                .font(.subheadline.weight(.semibold))
                                .multilineTextAlignment(.leading)
                                .foregroundColor(AppColors.text(for: colorScheme))
                                .padding(.horizontal, 14)
                                .padding(.vertical, 12)
                                .frame(width: 170, alignment: .leading)
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
                .stroke(AppColors.accent(for: colorScheme).opacity(0.16), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

private struct AIMessageBubble: View {
    let message: AIChatMessage
    let colorScheme: ColorScheme

    private var isUser: Bool {
        message.role == .user
    }

    var body: some View {
        HStack {
            if isUser { Spacer(minLength: 48) }

            VStack(alignment: .leading, spacing: 8) {
                Text(isUser ? "Du" : "X22 Bot")
                    .font(.caption.weight(.bold))
                    .foregroundColor(isUser ? .white.opacity(0.9) : AppColors.accent(for: colorScheme))

                if message.isStreaming && message.text.isEmpty {
                    HStack(spacing: 10) {
                        ProgressView()
                            .tint(AppColors.accent(for: colorScheme))

                        Text("X22 Bot antwortet gerade...")
                            .font(.subheadline)
                            .foregroundColor(AppColors.secondaryText(for: colorScheme))
                    }
                } else {
                    Text(message.text)
                        .font(.body)
                        .foregroundColor(isUser ? .white : AppColors.text(for: colorScheme))

                    if let imageData = message.imageData,
                       let image = UIImage(data: imageData) {
                        Image(uiImage: image)
                            .resizable()
                            .scaledToFill()
                            .frame(maxWidth: .infinity)
                            .frame(height: 220)
                            .clipShape(RoundedRectangle(cornerRadius: 18))
                            .padding(.top, 4)
                    }
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
                            : AppColors.accent(for: colorScheme).opacity(0.14),
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

private struct AIComposerBar: View {
    let colorScheme: ColorScheme
    @Binding var draft: String
    @Binding var composerMode: AIComposerMode
    let isFocused: FocusState<Bool>.Binding
    let isSending: Bool
    let onReset: () -> Void
    let onSend: () -> Void
    @State private var showingComposer = false

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 10) {
                Button(action: { showingComposer = true }) {
                    Text(draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "Bot Prompt" : "Prompt weiter")
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
                    Picker("Modus", selection: $composerMode) {
                        ForEach(AIComposerMode.allCases) { mode in
                            Text(mode.title).tag(mode)
                        }
                    }
                    .pickerStyle(.segmented)

                    TextField(
                        composerMode == .text
                            ? "Zum Beispiel: Teaser fuer den naechsten Drop."
                            : "Zum Beispiel: Dunkles Cover-Art fuer einen neuen Release.",
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
                            Label(
                                composerMode == .text ? "Senden" : "Visual generieren",
                                systemImage: "arrow.up.circle.fill"
                            )
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
                .navigationTitle(composerMode == .text ? "Bot Prompt" : "Visual Prompt")
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

private struct AIBadge: View {
    let text: String
    let colorScheme: ColorScheme

    var body: some View {
        Text(text)
            .font(.caption.weight(.bold))
            .foregroundColor(AppColors.accent(for: colorScheme))
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                Capsule()
                    .fill(AppColors.accent(for: colorScheme).opacity(0.12))
            )
    }
}

#Preview {
    let services = AppServices()

    AIView(
        aiChatService: services.aiChatService,
        featureFlags: services.featureFlags
    )
}
