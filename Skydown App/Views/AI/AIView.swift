import SwiftUI
import UIKit

struct AIView: View {
    @StateObject private var viewModel: AIChatViewModel
    @ObservedObject private var featureFlags: FeatureFlagsService
    @EnvironmentObject private var authManager: AuthManager
    @Environment(\.colorScheme) private var colorScheme
    @FocusState private var isComposerFocused: Bool
    private let showsNavigation: Bool

    init(
        aiChatService: AIChatServicing = FirebaseFunctionsAIChatService(),
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
        .onDisappear {
            isComposerFocused = false
        }
    }

    private var content: some View {
        VStack(spacing: 0) {
            if featureFlags.isAIEnabled {
                if viewModel.messages.isEmpty {
                    VStack(alignment: .leading, spacing: 14) {
                        Spacer(minLength: showsNavigation ? 8 : 4)

                        AIEmptyStateHeader(colorScheme: colorScheme)
                        AIHeroCard(colorScheme: colorScheme, badges: aiHeroBadges)
                        aiSessionDeck

                        AIQuickPromptCard(
                            colorScheme: colorScheme,
                            prompts: viewModel.quickPrompts,
                            onPromptSelected: { prompt in
                                isComposerFocused = false
                                viewModel.sendPrompt(prompt)
                            }
                        )

                        AIVisualPromptCard(
                            colorScheme: colorScheme,
                            prompts: viewModel.visualPrompts,
                            onPromptSelected: { prompt in
                                isComposerFocused = false
                                viewModel.generateVisual(prompt)
                            }
                        )

                        Spacer(minLength: 12)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
                    .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                    .padding(.bottom, 6)
                } else {
                    ScrollViewReader { proxy in
                        let scrollToken = viewModel.messages.last.map { message in
                            "\(message.id.uuidString)-\(message.isStreaming)-\(message.text.count)-\(message.imageData?.count ?? 0)"
                        } ?? "chat-empty"

                        ScrollView {
                            LazyVStack(alignment: .leading, spacing: 10) {
                                aiSessionDeck

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
                        .onAppear {
                            DispatchQueue.main.async {
                                proxy.scrollTo("chat-end", anchor: .bottom)
                            }
                        }
                        .onChange(of: scrollToken) { _, _ in
                            DispatchQueue.main.async {
                                withAnimation(.easeOut(duration: 0.25)) {
                                    proxy.scrollTo("chat-end", anchor: .bottom)
                                }
                            }
                        }
                    }
                }
            } else {
                VStack {
                    Spacer(minLength: 24)
                    AIDisabledCard(colorScheme: colorScheme)
                        .padding(.horizontal, SkydownLayout.screenHorizontalPadding)
                    Spacer()
                }
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if featureFlags.isAIEnabled {
                AIComposerBar(
                    colorScheme: colorScheme,
                    draft: $viewModel.draft,
                    composerMode: $viewModel.composerMode,
                    textMode: $viewModel.textMode,
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
            secondaryAccent: AppColors.accentMystic(for: colorScheme)
        )
    }

    private var aiHeroBadges: [String] {
        [
            viewModel.composerMode.title,
            viewModel.composerMode == .visual ? "Visual Output" : viewModel.textMode.title,
            viewModel.messages.isEmpty ? "Neue Session" : "\(viewModel.messages.count) Steps"
        ]
    }

    private var aiSessionDeck: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 10) {
                AISessionSignalCard(
                    title: "Mode",
                    value: viewModel.composerMode.title,
                    detail: viewModel.composerMode == .visual ? "Bild-Generierung" : "Text-Produktion",
                    accent: AppColors.accent(for: colorScheme),
                    colorScheme: colorScheme
                )
                AISessionSignalCard(
                    title: "Focus",
                    value: viewModel.composerMode == .visual ? "Cinematic" : viewModel.textMode.title,
                    detail: viewModel.composerMode == .visual ? "Prompt -> Visual" : "Textmodus direkt steuerbar",
                    accent: AppColors.accentMystic(for: colorScheme),
                    colorScheme: colorScheme
                )
                AISessionSignalCard(
                    title: "Memory",
                    value: viewModel.messages.isEmpty ? "Neu" : "\(viewModel.messages.count) Steps",
                    detail: "Verlauf bleibt pro Konto aktiv",
                    accent: AppColors.spotify(for: colorScheme),
                    colorScheme: colorScheme
                )
            }
        }
    }
}

private struct AIEmptyStateHeader: View {
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Was brauchst du?")
                .font(.system(size: 28, weight: .black, design: .rounded))
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Kurz tippen und los.")
                .font(.subheadline.weight(.medium))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Text("Dein Verlauf wird pro Konto weitergefuehrt, damit der Bot nicht jedes Mal bei null startet.")
                .font(.footnote.weight(.semibold))
                .foregroundColor(AppColors.accent(for: colorScheme).opacity(0.9))
        }
    }
}

private struct AIHeroCard: View {
    let colorScheme: ColorScheme
    let badges: [String]

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 10) {
                Text("SkyOs Bot")
                    .font(.system(size: 28, weight: .black, design: .rounded))
                    .foregroundColor(AppColors.text(for: colorScheme))

                Text("Captions, Hooks, Skripte, Visuals.")
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(AppColors.secondaryText(for: colorScheme))
            }

            ZStack {
                Circle()
                    .fill(AppColors.accent(for: colorScheme).opacity(0.16))
                    .frame(width: 54, height: 54)

                Image(systemName: "sparkles")
                    .font(.title3.weight(.bold))
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
                    Text("SkyOs Bot pausiert")
                        .font(.headline)
                        .foregroundColor(AppColors.text(for: colorScheme))

                    Text("Voruebergehend nicht verfuegbar")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(AppColors.accent(for: colorScheme))
                }
            }
        }
        .padding(SkydownLayout.cardPadding)
        .background(AppColors.cardBackground(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius)
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius))
    }
}

private struct AIQuickPromptCard: View {
    let colorScheme: ColorScheme
    let prompts: [String]
    let onPromptSelected: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Prompts")
                .font(.headline)
                .foregroundColor(AppColors.text(for: colorScheme))

            Text("Schnell rein.")
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
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius))
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

            Text("Ein Prompt reicht.")
                .font(.subheadline)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(prompts) { prompt in
                        Button(action: { onPromptSelected(prompt.prompt) }, label: {
                            Text(prompt.label)
                                .font(.subheadline.weight(.semibold))
                                .multilineTextAlignment(.leading)
                                .foregroundColor(AppColors.text(for: colorScheme))
                                .padding(.horizontal, 14)
                                .padding(.vertical, 12)
                                .frame(width: 170, alignment: .leading)
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
                .stroke(AppColors.accent(for: colorScheme).opacity(0.14), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.cardCornerRadius))
    }
}

private struct AIMessageBubble: View {
    let message: AIChatMessage
    let colorScheme: ColorScheme
    @State private var showingShareSheet = false
    @State private var shareItems: [Any] = []
    @State private var copyLabel = "Kopieren"

    private var isUser: Bool {
        message.role == .user
    }

    var body: some View {
        HStack {
            if isUser { Spacer(minLength: 48) }

            VStack(alignment: .leading, spacing: 8) {
                Text(isUser ? "Du" : "SkyOs Bot")
                    .font(.caption.weight(.bold))
                    .foregroundColor(isUser ? .white.opacity(0.9) : AppColors.accent(for: colorScheme))

                if message.isStreaming && message.text.isEmpty {
                    HStack(spacing: 10) {
                        ProgressView()
                            .tint(AppColors.accent(for: colorScheme))

                        Text("SkyOs Bot antwortet gerade...")
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
                            .clipShape(RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius))
                            .padding(.top, 4)
                    }

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
                            .foregroundColor(AppColors.accent(for: colorScheme))

                            Button("Teilen / Speichern") {
                                shareItems = sharePayload()
                                showingShareSheet = true
                            }
                            .font(.caption.weight(.semibold))
                            .foregroundColor(AppColors.accentMystic(for: colorScheme))
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
                            : AppColors.accent(for: colorScheme).opacity(0.14),
                        lineWidth: 1
                    )
            )

            if !isUser { Spacer(minLength: 48) }
        }
        .sheet(isPresented: $showingShareSheet) {
            AIShareSheet(activityItems: shareItems)
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

    private func sharePayload() -> [Any] {
        var items: [Any] = [message.text]
        if let imageData = message.imageData,
           let image = UIImage(data: imageData) {
            items.append(image)
        }
        return items
    }
}

private struct AIComposerBar: View {
    let colorScheme: ColorScheme
    @Binding var draft: String
    @Binding var composerMode: AIComposerMode
    @Binding var textMode: AITextMode
    let isFocused: FocusState<Bool>.Binding
    let isSending: Bool
    let onReset: () -> Void
    let onSend: () -> Void
    @StateObject private var keyboardObserver = SkydownKeyboardObserver()

    private var trimmedDraft: String {
        draft.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var activeKeyboardInset: CGFloat {
        isFocused.wrappedValue ? keyboardObserver.bottomInset : 0
    }

    var body: some View {
        VStack(spacing: 0) {
            VStack(spacing: 10) {
                HStack(spacing: 10) {
                    Picker("Modus", selection: $composerMode) {
                        ForEach(AIComposerMode.allCases) { mode in
                            Text(mode.title).tag(mode)
                        }
                    }
                    .pickerStyle(.segmented)
                    .skydownSelectionFeedback(trigger: composerMode)

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
                }

                if composerMode == .text {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(AITextMode.allCases) { mode in
                                Button {
                                    textMode = mode
                                } label: {
                                    Text(mode.title)
                                        .font(.caption.weight(.bold))
                                        .foregroundColor(
                                            textMode == mode
                                                ? .white
                                                : AppColors.text(for: colorScheme)
                                        )
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 8)
                                        .background(
                                            Capsule()
                                                .fill(
                                                    textMode == mode
                                                        ? AppColors.accent(for: colorScheme)
                                                        : AppColors.secondaryBackground(for: colorScheme)
                                                )
                                        )
                                }
                                .buttonStyle(.plain)
                                .skydownTactileAction()
                            }
                        }
                    }
                    .skydownSelectionFeedback(trigger: textMode)
                }

                HStack(alignment: .bottom, spacing: 10) {
                    TextField(
                        composerMode == .text
                            ? textMode.placeholder
                            : "Zum Beispiel: Dunkles Cover-Art fuer einen neuen Release.",
                        text: $draft,
                        axis: .vertical
                    )
                    .lineLimit(1...4)
                    .focused(isFocused)
                    .submitLabel(.done)
                    .onSubmit {
                        isFocused.wrappedValue = false
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 12)
                    .background(
                        RoundedRectangle(cornerRadius: SkydownLayout.buttonCornerRadius)
                            .fill(AppColors.secondaryBackground(for: colorScheme))
                    )
                    .foregroundColor(AppColors.text(for: colorScheme))

                    HStack(spacing: 8) {
                        if isFocused.wrappedValue {
                            Button {
                                isFocused.wrappedValue = false
                            } label: {
                                Image(systemName: "keyboard.chevron.compact.down")
                                    .font(.system(size: 18, weight: .bold))
                                    .foregroundColor(AppColors.text(for: colorScheme))
                                    .frame(width: 38, height: 38)
                                    .background(
                                        Circle()
                                            .fill(AppColors.secondaryBackground(for: colorScheme))
                                    )
                            }
                            .buttonStyle(.plain)
                            .skydownTactileAction()
                            .transition(.scale.combined(with: .opacity))
                        }

                        Button(action: {
                            isFocused.wrappedValue = false
                            onSend()
                        }, label: {
                            Image(systemName: composerMode == .text ? "arrow.up.circle.fill" : "sparkles")
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
                    .animation(.easeOut(duration: 0.16), value: isFocused.wrappedValue)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 10)
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
        .padding(.bottom, activeKeyboardInset)
    }
}

private struct AIBadge: View {
    let text: String
    let colorScheme: ColorScheme

    var body: some View {
        SkydownMetaLabel(
            text: text,
            tint: AppColors.accent(for: colorScheme)
        )
    }
}

private struct AISessionSignalCard: View {
    let title: String
    let value: String
    let detail: String
    let accent: Color
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(title.uppercased())
                .font(.caption2.weight(.bold))
                .foregroundColor(AppColors.secondaryText(for: colorScheme))

            Text(value)
                .font(.subheadline.weight(.bold))
                .foregroundColor(AppColors.text(for: colorScheme))
                .lineLimit(1)

            Text(detail)
                .font(.caption)
                .foregroundColor(AppColors.secondaryText(for: colorScheme))
                .lineLimit(1)
        }
        .frame(width: 142, alignment: .leading)
        .padding(.horizontal, 12)
        .padding(.vertical, 11)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(AppColors.secondaryBackground(for: colorScheme))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(accent.opacity(0.16), lineWidth: 1)
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
