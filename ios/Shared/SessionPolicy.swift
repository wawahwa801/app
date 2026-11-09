import Foundation

struct PolicyState: Codable, Equatable {
    var lastChatAt: TimeInterval?
    var watchingSentReel = false
    var sentAuthor: String?
    var pendingAuthor: String?
    var candidate: ScreenKind?
    var current: ScreenKind = .other
    var lastNudgeAt: TimeInterval?
}

/// Decides when to nudge. A reel opened from a chat is fine until the user moves to another one.
///
/// Instagram's navigation icons have no text, so leaving a chat can't be seen directly, and a
/// reel counts as sent only if a chat was on screen moments before.
struct SessionPolicy {
    static let chatMemorySeconds: TimeInterval = 20
    static let nudgeRepeatSeconds: TimeInterval = 15

    var state = PolicyState()

    /// Returns the section to nudge about, if any. Blocks need two matching samples in a row, so
    /// one misread frame can't interrupt anything.
    mutating func observe(_ observation: Observation, now: TimeInterval, isAllowed: (Site) -> Bool) -> Site? {
        if observation.kind == .directMessages {
            state.lastChatAt = now
            state.watchingSentReel = false
            state.sentAuthor = nil
            state.pendingAuthor = nil
        }
        guard observation.kind == state.candidate else {
            state.candidate = observation.kind
            return nil
        }
        state.current = observation.kind
        guard case .shortForm(let site) = observation.kind else { return nil }
        if site == .instagram, isSentReel(observation, now: now) { return nil }
        if isAllowed(site) { return nil }
        if let last = state.lastNudgeAt, now - last < Self.nudgeRepeatSeconds { return nil }
        state.lastNudgeAt = now
        return site
    }

    private mutating func isSentReel(_ observation: Observation, now: TimeInterval) -> Bool {
        if observation.isFeed {
            state.watchingSentReel = false
            state.lastChatAt = nil
            return false
        }
        if !state.watchingSentReel {
            guard let chat = state.lastChatAt, now - chat <= Self.chatMemorySeconds else { return false }
            state.watchingSentReel = true
            state.lastChatAt = nil
            state.sentAuthor = observation.author
            return true
        }
        guard let author = observation.author else { return true }
        guard let sent = state.sentAuthor else {
            state.sentAuthor = author
            return true
        }
        if ScreenClassifier.isSameAuthor(sent, author) {
            state.pendingAuthor = nil
            return true
        }
        // A new author must be read twice, since one bad OCR pass could look like a new reel.
        if let pending = state.pendingAuthor, ScreenClassifier.isSameAuthor(pending, author) {
            state.watchingSentReel = false
            state.sentAuthor = nil
            state.pendingAuthor = nil
            return false
        }
        state.pendingAuthor = author
        return true
    }
}

enum Friction {
    static let waitSeconds = 60
    static let wrongAnswerLockSeconds = 10

    /// AIME answers are integers from 000 to 999; leading zeros are optional.
    static func isCorrect(_ input: String, answer: Int) -> Bool {
        let trimmed = input.trimmingCharacters(in: .whitespaces)
        guard (1...3).contains(trimmed.count), trimmed.allSatisfy({ $0.isASCII && $0.isNumber }) else { return false }
        return Int(trimmed) == answer
    }
}
