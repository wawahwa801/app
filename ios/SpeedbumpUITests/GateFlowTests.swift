import XCTest

final class GateFlowTests: XCTestCase {
    func testSetupScreenExplainsAutomation() {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.staticTexts["Put a wall in front of Instagram".uppercased()].waitForExistence(timeout: 10)
            || app.staticTexts["Put a wall in front of Instagram"].exists)
        attach(app, "setup")
        app.swipeUp()
        XCTAssertTrue(app.buttons["Start watching my screen"].waitForExistence(timeout: 10))
        attach(app, "setup-broadcast")
    }

    func testAimeWallAcceptsOnlyTheRightAnswer() throws {
        let app = XCUIApplication()
        app.launch()
        setMode(app, "Solve an AIME-style problem")
        app.open(URL(string: "speedbump://wall?site=youtube")!)
        XCTAssertTrue(app.staticTexts["YouTube Shorts"].waitForExistence(timeout: 10))
        let submit = app.buttons["Submit"]
        XCTAssertTrue(submit.waitForExistence(timeout: 10))
        XCTAssertFalse(submit.isEnabled, "Submit must need an answer")

        // No problem in the bundle has 1 as its answer.
        let field = app.textFields["Answer"]
        field.tap()
        field.typeText("1")
        submit.tap()
        XCTAssertTrue(app.staticTexts["That's not it."].waitForExistence(timeout: 10))
        attach(app, "aime-wrong")

        let url = try XCTUnwrap(Bundle(for: Self.self).url(forResource: "aime-problems", withExtension: "json"))
        let problems = try XCTUnwrap(JSONSerialization.jsonObject(with: Data(contentsOf: url)) as? [[String: Any]])
        XCTAssertFalse(problems.contains { $0["answer"] as? Int == 1 })
        // Simple text queries are limited to 128 characters, and problems run longer.
        let shown = try XCTUnwrap(problems.first {
            app.staticTexts.matching(NSPredicate(format: "label == %@", $0["text"] as! String)).firstMatch.exists
        })
        let answer = shown["answer"] as! Int

        XCTAssertTrue(submit.waitForExistence(timeout: 30), "Lock never ended")
        field.tap()
        field.typeText(String(format: "%03d", answer))
        submit.tap()
        // The settings screen stays in the hierarchy behind the wall, so wait for the wall to go.
        let gone = expectation(for: NSPredicate(format: "exists == false"), evaluatedWith: submit)
        wait(for: [gone], timeout: 20)
        attach(app, "aime-right")
        XCTAssertFalse(app.staticTexts["That's not it."].exists, "The right answer was rejected")
        setMode(app, "Wait one minute")
    }

    private func setMode(_ app: XCUIApplication, _ label: String) {
        let row = app.staticTexts["Friction"].firstMatch
        for _ in 0..<5 where !row.isHittable { app.swipeUp() }
        row.tap()
        XCTAssertTrue(app.buttons[label].waitForExistence(timeout: 10))
        app.buttons[label].tap()
    }

    func testFullInstagramGoesThroughTheWall() {
        let app = XCUIApplication()
        app.launch()
        setMode(app, "Wait one minute")
        app.open(URL(string: "speedbump://gate")!)
        XCTAssertTrue(app.buttons["Messages"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons["Full Instagram"].exists)
        attach(app, "gate")

        app.buttons["Full Instagram"].tap()
        let wait = app.buttons["Wait…"]
        XCTAssertTrue(wait.waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["Instagram"].exists, "Full Instagram must not be labelled Reels")
        XCTAssertFalse(wait.isEnabled)
        attach(app, "wall")

        app.buttons["Go back"].tap()
        XCTAssertTrue(app.buttons["Messages"].waitForExistence(timeout: 5))
        app.buttons["Not now"].tap()
        XCTAssertFalse(app.buttons["Messages"].waitForExistence(timeout: 2))
    }

    func testWaitWallOpensAfterAMinute() {
        let app = XCUIApplication()
        app.launch()
        setMode(app, "Wait one minute")
        app.open(URL(string: "speedbump://gate")!)
        XCTAssertTrue(app.buttons["Full Instagram"].waitForExistence(timeout: 10))
        app.buttons["Full Instagram"].tap()
        let proceed = app.buttons["Continue"]
        XCTAssertFalse(proceed.waitForExistence(timeout: 45), "Unlocked too early")
        XCTAssertTrue(proceed.waitForExistence(timeout: 25), "Never unlocked")
        proceed.tap()
        // The settings screen behind the wall has its own "Instagram Reels" toggle, so look for
        // text that only the wall and gate show.
        XCTAssertTrue(app.navigationBars["Speedbump"].waitForExistence(timeout: 20))
        XCTAssertFalse(proceed.exists)
        XCTAssertFalse(app.buttons["Full Instagram"].exists)
        attach(app, "after-unlock")
    }

    private func attach(_ app: XCUIApplication, _ name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
