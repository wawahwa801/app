import AppIntents

/// The Shortcuts action behind the wall. The "When Instagram is opened" automation runs it each
/// time, including when Speedbump reopens Instagram, so it must stay silent during an unlock.
@available(iOS 26.0, *)
struct InstagramGateIntent: AppIntent {
    static let title: LocalizedStringResource = "Check Instagram Speedbump"
    static let description = IntentDescription("Shows the Speedbump screen if Instagram isn't unlocked.")
    static let supportedModes: IntentModes = [.background, .foreground(.dynamic)]

    @MainActor
    func perform() async throws -> some IntentResult {
        guard !SharedStore().isAllowed(.instagram), systemContext.currentMode.canContinueInForeground else {
            return .result()
        }
        do {
            try await continueInForeground(alwaysConfirm: false)
        } catch {
            // Staying silent leaves Instagram open, which is better than interrupting a chat
            // with an error.
            return .result()
        }
        WallRouter.shared.destination = .gate
        return .result()
    }
}
