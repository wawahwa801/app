import Foundation

enum Site: String, Codable, CaseIterable {
    case instagram, youtube

    var sectionName: String {
        switch self {
        case .instagram: return "Instagram Reels"
        case .youtube: return "YouTube Shorts"
        }
    }
}

/// A piece of recognised text. Coordinates run from 0 to 1, with the origin at the top left.
struct TextBox: Equatable {
    let text: String
    let x: Double
    let y: Double
}

enum ScreenKind: Codable, Equatable {
    case shortForm(Site)
    case directMessages
    case other
}

struct Observation: Equatable {
    var kind: ScreenKind
    /// The feed itself is open, so this can't be a reel opened from a chat.
    var isFeed = false
    /// The author shown on screen, used to notice a move to a different reel.
    var author: String?
}

/// Works out what's on screen from text alone, because a broadcast can't see which app is open.
///
/// These rules describe the apps' layouts as best known without a device to check on. Calibrate
/// them against real screenshots before relying on them.
enum ScreenClassifier {
    static func classify(_ boxes: [TextBox]) -> Observation {
        // Chats come first, so that nothing a message says can ever make a chat look blocked.
        if isChat(boxes) { return Observation(kind: .directMessages) }
        if isShorts(boxes) { return Observation(kind: .shortForm(.youtube), isFeed: true) }
        if isCommentSheet(boxes) { return Observation(kind: .other) }

        let hasReelsHeading = boxes.contains { normalized($0.text) == "reels" && $0.y < 0.15 }
        let hasReelControls = boxes.contains { normalized($0.text).hasPrefix("add comment") && $0.y > 0.8 }
        if hasReelsHeading || hasReelControls {
            return Observation(kind: .shortForm(.instagram), isFeed: hasReelsHeading, author: reelAuthor(boxes))
        }
        return Observation(kind: .other)
    }

    private static func isChat(_ boxes: [TextBox]) -> Bool {
        boxes.contains { box in
            let text = normalized(box.text)
            return (text.hasPrefix("message") && box.y > 0.8) || (text == "messages" && box.y < 0.15)
        }
    }

    private static func isCommentSheet(_ boxes: [TextBox]) -> Bool {
        boxes.contains { normalized($0.text) == "comments" && $0.y < 0.6 }
    }

    /// Shorts stack their action labels down the right edge. Long videos lay the same labels out
    /// in a horizontal row under the player, which this rejects.
    private static func isShorts(_ boxes: [TextBox]) -> Bool {
        let labels = Set(["dislike", "share", "remix"])
        let rail = boxes.filter { $0.x > 0.7 && labels.contains(normalized($0.text)) }
        guard Set(rail.map { normalized($0.text) }).count >= 2 else { return false }
        let heights = rail.map(\.y)
        return (heights.max()! - heights.min()!) > 0.04
    }

    private static func reelAuthor(_ boxes: [TextBox]) -> String? {
        guard let follow = boxes.first(where: { normalized($0.text) == "follow" && $0.y > 0.6 }) else { return nil }
        return boxes.first { $0.x < follow.x && abs($0.y - follow.y) < 0.02 }.map { normalized($0.text) }
    }

    static func normalized(_ text: String) -> String {
        text.lowercased()
            .replacingOccurrences(of: "…", with: "")
            .replacingOccurrences(of: "...", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// OCR misreads letters, so authors count as different only if they share few letter pairs.
    static func isSameAuthor(_ first: String, _ second: String) -> Bool {
        func pairs(_ text: String) -> Set<String> {
            let letters = Array(text)
            guard letters.count > 1 else { return [text] }
            return Set((0..<(letters.count - 1)).map { String(letters[$0...($0 + 1)]) })
        }
        let a = pairs(first), b = pairs(second)
        let overlap = Double(a.intersection(b).count) / Double(max(a.union(b).count, 1))
        return overlap >= 0.5
    }
}
