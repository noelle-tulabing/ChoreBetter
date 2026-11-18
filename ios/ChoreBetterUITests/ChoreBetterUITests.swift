import XCTest

final class ChoreBetterUITests: XCTestCase {
    @MainActor
    func testCreateAndShareList() {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.buttons["listPicker"].waitForExistence(timeout: 10))
        app.buttons["listPicker"].tap()
        app.buttons["Create list"].tap()
        app.textFields["List name"].tap()
        app.textFields["List name"].typeText("Kitchen")
        app.buttons["Create"].tap()
        XCTAssertTrue(app.staticTexts["No chores here"].waitForExistence(timeout: 3))
        app.buttons["listPicker"].tap()
        app.buttons["Share current list"].tap()
        app.buttons.containing(.staticText, identifier: "Noelle").firstMatch.tap()
        XCTAssertFalse(app.buttons.containing(.staticText, identifier: "Noelle").firstMatch.isEnabled)
        app.buttons["Close"].tap()
        app.buttons["Profile"].tap()
        app.buttons.containing(.staticText, identifier: "Noelle").firstMatch.tap()
        XCTAssertTrue(app.buttons["listPicker"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["listPicker"].label.contains("Kitchen"))
        XCTAssertFalse(app.buttons["addChore"].exists)
    }

    @MainActor
    func testCompleteRedeemAndUndoGuard() {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.buttons["Complete Take out the trash"].waitForExistence(timeout: 10))
        app.buttons["Complete Take out the trash"].tap()
        app.buttons["Complete Feed the dog"].tap()
        XCTAssertEqual(app.staticTexts["balance"].label, "30 points")
        app.buttons["Rewards"].tap()
        app.buttons["Redeem: Choose movie night"].tap()
        XCTAssertTrue(app.alerts.firstMatch.waitForExistence(timeout: 3))
        app.alerts.buttons["Redeem"].tap()
        XCTAssertEqual(app.staticTexts["balance"].label, "0 points")
        app.buttons["Back"].tap()
        app.buttons["Uncheck Feed the dog"].tap()
        XCTAssertTrue(app.alerts["Couldn't update"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.alerts.staticTexts["Can't uncheck: these points have already been spent."].exists)
    }

    @MainActor
    func testAddEditAndMemberProfile() {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.buttons["addChore"].waitForExistence(timeout: 10))
        app.buttons["addChore"].tap()
        app.buttons["saveEntry"].tap()
        XCTAssertTrue(app.staticTexts["formError"].exists)
        app.textFields["entryName"].tap()
        app.textFields["entryName"].typeText("Water plants")
        app.buttons["saveEntry"].tap()
        XCTAssertTrue(app.buttons["Complete Water plants"].waitForExistence(timeout: 3))
        app.buttons["Edit Water plants"].tap()
        app.textFields["entryName"].tap()
        app.textFields["entryName"].typeText(" outside")
        app.buttons["saveEntry"].tap()
        XCTAssertTrue(app.buttons["Complete Water plants outside"].waitForExistence(timeout: 3))
        app.buttons["Profile"].tap()
        app.buttons.containing(.staticText, identifier: "Noelle").firstMatch.tap()
        XCTAssertTrue(app.buttons["Complete Do the dishes"].waitForExistence(timeout: 3))
        XCTAssertFalse(app.buttons["addChore"].exists)
        XCTAssertFalse(app.buttons["All chores"].exists)
        app.buttons["Complete Do the dishes"].tap()
        XCTAssertEqual(app.staticTexts["balance"].label, "15 points")
    }
}
