//
//  Skydown_AppUITests.swift
//  Skydown AppUITests
//
//  Created by Yang D. Nash on 23.07.25.
//

import XCTest

final class Skydown_AppUITests: XCTestCase {

    override func setUpWithError() throws {
        // Put setup code here. This method is called before the invocation of each test method in the class.

        // In UI tests it is usually best to stop immediately when a failure occurs.
        continueAfterFailure = false

        // In UI tests it’s important to set the initial state - such as interface orientation - required for your tests before they run. The setUp method is a good place to do this.
    }

    override func tearDownWithError() throws {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }

    @MainActor
    func testExample() throws {
        // UI tests must launch the application that they test.
        let app = XCUIApplication()
        app.launch()

        // Use XCTAssert and related functions to verify your tests produce the correct results.
    }

    @MainActor
    func testLaunchPerformance() throws {
        // This measures how long it takes to launch your application.
        measure(metrics: [XCTApplicationLaunchMetric()]) {
            XCUIApplication().launch()
        }
    }

    @MainActor
    func testSettingsCanOpenFromMainShell() throws {
        let app = XCUIApplication()
        app.launch()

        let settingsButton = app.buttons["app.open_settings"].firstMatch
        if !settingsButton.waitForExistence(timeout: 6) {
            let openMusicButton = app.buttons["launch.open_music"].firstMatch
            XCTAssertTrue(
                openMusicButton.waitForExistence(timeout: 45),
                "Nach Intro/Landing sollte mindestens ein Einstiegspunkt verfuegbar sein."
            )
            openMusicButton.tap()
            XCTAssertTrue(
                settingsButton.waitForExistence(timeout: 30),
                "Der Settings-Button sollte nach dem Einstieg in die Main-Shell sichtbar werden."
            )
        }

        settingsButton.tap()

        let settingsRoot = app.descendants(matching: .any)["settings.root"].firstMatch
        XCTAssertTrue(
            settingsRoot.waitForExistence(timeout: 45),
            "Die Settings-Ansicht sollte ohne Crash erscheinen."
        )
    }

    @MainActor
    func testMusicCatalogCanOpenFromLanding() throws {
        let app = XCUIApplication()
        app.launch()

        let openMusicButton = app.buttons["launch.open_music"].firstMatch
        XCTAssertTrue(
            openMusicButton.waitForExistence(timeout: 45),
            "Der Music-Einstieg sollte nach Intro/Landing sichtbar sein."
        )
        openMusicButton.tap()

        let musicHub = app.descendants(matching: .any)["music.hub.root"].firstMatch
        XCTAssertTrue(
            musicHub.waitForExistence(timeout: 30),
            "Der Music-Hub sollte ohne Crash erscheinen."
        )

        let catalogButton = app.buttons["music.hub.open_catalog"].firstMatch
        XCTAssertTrue(
            catalogButton.waitForExistence(timeout: 10),
            "Der Katalog-Einstieg sollte im Music-Hub sichtbar sein."
        )
        catalogButton.tap()

        let catalogRoot = app.descendants(matching: .any)["music.catalog.root"].firstMatch
        XCTAssertTrue(
            catalogRoot.waitForExistence(timeout: 30),
            "Der Music-Katalog sollte ohne Crash erscheinen."
        )
    }

    @MainActor
    func testAppStoreScreenshots() throws {
        let app = XCUIApplication()
        app.launchArguments += [
            "-AppleLanguages",
            "(en)",
            "-AppleLocale",
            "en_US",
        ]
        app.launch()

        enterMainShellIfNeeded(app: app)

        // 01 Merch
        tapTab(app: app, index: 0)
        waitForUISettle()
        saveScreenshot(name: "01-merch")

        // 02 Music
        tapTab(app: app, index: 1)
        waitForUISettle()
        saveScreenshot(name: "02-music")

        // 03 Home
        tapTab(app: app, index: 2)
        waitForUISettle()
        saveScreenshot(name: "03-home")

        // 04 Video
        tapTab(app: app, index: 3)
        waitForUISettle()
        saveScreenshot(name: "04-video")

        // 05 Tools
        tapTab(app: app, index: 4)
        waitForUISettle()
        saveScreenshot(name: "05-tools")

        // 06 Settings
        let settingsButton = app.buttons["app.open_settings"].firstMatch
        XCTAssertTrue(settingsButton.waitForExistence(timeout: 20), "Settings button should be visible in shell.")
        settingsButton.tap()
        let settingsRoot = app.descendants(matching: .any)["settings.root"].firstMatch
        XCTAssertTrue(settingsRoot.waitForExistence(timeout: 30), "Settings screen should open.")
        waitForUISettle()
        saveScreenshot(name: "06-settings")
    }
}

private extension Skydown_AppUITests {
    @MainActor
    func enterMainShellIfNeeded(app: XCUIApplication) {
        let settingsButton = app.buttons["app.open_settings"].firstMatch
        if settingsButton.waitForExistence(timeout: 8) {
            return
        }

        let openMusicButton = app.buttons["launch.open_music"].firstMatch
        XCTAssertTrue(
            openMusicButton.waitForExistence(timeout: 60),
            "Landing open_music button should appear after intro."
        )
        openMusicButton.tap()
        XCTAssertTrue(
            settingsButton.waitForExistence(timeout: 30),
            "Main shell should appear after opening from landing."
        )
    }

    @MainActor
    func tapTab(app: XCUIApplication, index: Int) {
        let tabBar = app.tabBars.firstMatch
        XCTAssertTrue(tabBar.waitForExistence(timeout: 20), "Tab bar must be visible.")

        let buttons = tabBar.buttons.allElementsBoundByIndex
        XCTAssertGreaterThan(buttons.count, index, "Expected at least \(index + 1) tab buttons.")
        buttons[index].tap()
    }

    @MainActor
    func waitForUISettle() {
        RunLoop.current.run(until: Date().addingTimeInterval(1.8))
    }

    @MainActor
    func saveScreenshot(name: String) {
        let screenshot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: screenshot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)

        guard let outputDir = ProcessInfo.processInfo.environment["SKYDOWN_SCREENSHOT_DIR"] else {
            return
        }

        let fileManager = FileManager.default
        let outputURL = URL(fileURLWithPath: outputDir, isDirectory: true)
        try? fileManager.createDirectory(at: outputURL, withIntermediateDirectories: true)
        let fileURL = outputURL.appendingPathComponent("\(name).png")
        try? screenshot.pngRepresentation.write(to: fileURL, options: .atomic)
    }
}
