import XCTest

final class ClassifierTests: XCTestCase {
    private func box(_ text: String, _ x: Double, _ y: Double) -> TextBox { TextBox(text: text, x: x, y: y) }

    func testReelsFeedHeading() {
        let result = ScreenClassifier.classify([box("Reels", 0.05, 0.06), box("Add comment…", 0.1, 0.9)])
        XCTAssertEqual(result.kind, .shortForm(.instagram))
        XCTAssertTrue(result.isFeed)
    }

    func testReelFromChatHasNoHeading() {
        let result = ScreenClassifier.classify([box("alice", 0.15, 0.82), box("Follow", 0.4, 0.82), box("Add comment…", 0.1, 0.92)])
        XCTAssertEqual(result.kind, .shortForm(.instagram))
        XCTAssertFalse(result.isFeed)
        XCTAssertEqual(result.author, "alice")
    }

    func testChatWinsEvenWhenAMessageMentionsReels() {
        let result = ScreenClassifier.classify([box("Reels", 0.5, 0.1), box("Add comment", 0.2, 0.85), box("Message…", 0.2, 0.93)])
        XCTAssertEqual(result.kind, .directMessages)
    }

    func testCommentsOverAReelAreNotAReel() {
        let result = ScreenClassifier.classify([box("Comments", 0.4, 0.3), box("Add a comment…", 0.1, 0.9)])
        XCTAssertEqual(result.kind, .other)
    }

    func testShortsRail() {
        let result = ScreenClassifier.classify([box("Dislike", 0.88, 0.45), box("Share", 0.88, 0.6), box("Remix", 0.88, 0.7)])
        XCTAssertEqual(result.kind, .shortForm(.youtube))
    }

    func testLongVideoRowIsNotShorts() {
        let result = ScreenClassifier.classify([
            box("Shorts are ruining my attention span", 0.05, 0.32),
            box("Dislike", 0.3, 0.4), box("Share", 0.5, 0.4), box("Remix", 0.72, 0.4), box("Download", 0.85, 0.4),
            box("Shorts", 0.25, 0.95),
        ])
        XCTAssertEqual(result.kind, .other)
    }

    func testOrdinaryTextIsOther() {
        XCTAssertEqual(ScreenClassifier.classify([box("reels are fun", 0.1, 0.5)]).kind, .other)
        XCTAssertEqual(ScreenClassifier.classify([]).kind, .other)
    }

    func testAuthorMatchingToleratesMisreads() {
        XCTAssertTrue(ScreenClassifier.isSameAuthor("sam.bakes.daily", "sam.bakes.dai1y"))
        XCTAssertFalse(ScreenClassifier.isSameAuthor("sam.bakes.daily", "travelwithjo"))
    }
}

final class PolicyTests: XCTestCase {
    private let chat = Observation(kind: .directMessages)
    private let other = Observation(kind: .other)
    private let reel = Observation(kind: .shortForm(.instagram), author: "alice")
    private let nextReel = Observation(kind: .shortForm(.instagram), author: "travelwithjo")
    private let feed = Observation(kind: .shortForm(.instagram), isFeed: true)
    private let shorts = Observation(kind: .shortForm(.youtube), isFeed: true)

    /// Feeds observations 1.5 seconds apart, and returns what each one decided.
    private func run(_ policy: inout SessionPolicy, _ items: [Observation], from start: TimeInterval = 0, allowed: Bool = false) -> [Site?] {
        items.enumerated().map { index, item in
            policy.observe(item, now: start + Double(index) * 1.5) { _ in allowed }
        }
    }

    func testSentReelPlays() {
        var policy = SessionPolicy()
        XCTAssertTrue(run(&policy, [chat, chat, reel, reel, reel, reel]).allSatisfy { $0 == nil })
    }

    func testMovingToAnotherReelNudges() {
        var policy = SessionPolicy()
        let results = run(&policy, [chat, chat, reel, reel, nextReel, nextReel, nextReel])
        // The new author has to be read twice, so the alert comes on the second read.
        XCTAssertTrue(results[0...4].allSatisfy { $0 == nil })
        XCTAssertEqual(results[5], .some(.instagram))
    }

    func testOneMisreadAuthorDoesNotNudge() {
        var policy = SessionPolicy()
        XCTAssertTrue(run(&policy, [chat, chat, reel, reel, nextReel, reel, reel]).allSatisfy { $0 == nil })
    }

    func testSheetOverSentReelDoesNotNudge() {
        var policy = SessionPolicy()
        XCTAssertTrue(run(&policy, [chat, chat, reel, reel, other, other, reel, reel]).allSatisfy { $0 == nil })
    }

    func testFeedNudgesEvenRightAfterChat() {
        var policy = SessionPolicy()
        XCTAssertEqual(run(&policy, [chat, chat, feed, feed]).last, .some(.instagram))
    }

    func testReelLongAfterChatNudges() {
        var policy = SessionPolicy()
        _ = run(&policy, [chat, chat])
        XCTAssertEqual(run(&policy, [reel, reel], from: 100).last, .some(.instagram))
    }

    func testSingleMisreadFrameDoesNotNudge() {
        var policy = SessionPolicy()
        XCTAssertTrue(run(&policy, [other, shorts, other, other]).allSatisfy { $0 == nil })
    }

    func testNudgesRepeatButNotEveryFrame() {
        var policy = SessionPolicy()
        let results = run(&policy, Array(repeating: shorts, count: 22))
        XCTAssertEqual(results.compactMap { $0 }.count, 3)
    }

    func testUnlockedSectionIsQuiet() {
        var policy = SessionPolicy()
        XCTAssertTrue(run(&policy, [shorts, shorts, shorts], allowed: true).allSatisfy { $0 == nil })
    }

    func testStateSurvivesRestart() throws {
        var policy = SessionPolicy()
        _ = run(&policy, [chat, chat, reel, reel])
        let saved = try JSONDecoder().decode(PolicyState.self, from: JSONEncoder().encode(policy.state))
        var restored = SessionPolicy(state: saved)
        XCTAssertNil(restored.observe(reel, now: 6) { _ in false })
    }

    func testAnswers() {
        XCTAssertTrue(Friction.isCorrect("042", answer: 42))
        XCTAssertTrue(Friction.isCorrect("42", answer: 42))
        XCTAssertFalse(Friction.isCorrect("0042", answer: 42))
        XCTAssertFalse(Friction.isCorrect("4x", answer: 42))
        XCTAssertFalse(Friction.isCorrect("", answer: 42))
    }
}
