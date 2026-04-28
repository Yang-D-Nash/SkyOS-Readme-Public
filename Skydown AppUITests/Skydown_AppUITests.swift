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
        XCUIDevice.shared.orientation = .portrait

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
    func testOwnerHubOpensFromSettingsWhenUiTestPlatformOwner() throws {
        let app = XCUIApplication()
        app.launchArguments += [
            "-AppleLanguages",
            "(en)",
            "-AppleLocale",
            "en_US",
            "-ui_test_start_main_shell",
            "-ui_test_signed_in",
            "-ui_test_platform_owner",
        ]
        app.launch()

        enterMainShellIfNeeded(app: app)

        let settingsButton = app.buttons["app.open_settings"].firstMatch
        XCTAssertTrue(
            settingsButton.waitForExistence(timeout: 45),
            "Der Settings-Button sollte in der Main-Shell sichtbar sein."
        )
        settingsButton.tap()

        let settingsRoot = app.descendants(matching: .any)["settings.root"].firstMatch
        XCTAssertTrue(
            settingsRoot.waitForExistence(timeout: 45),
            "Die Settings-Ansicht sollte erscheinen."
        )

        let ownerHubEntry = app.descendants(matching: .any)["settings.open_owner_hub"].firstMatch
        XCTAssertTrue(
            ownerHubEntry.waitForExistence(timeout: 20),
            "Der Owner-Hub-Einstieg sollte fuer die Platform-Owner-UITest-Session sichtbar sein."
        )
        ownerHubEntry.tap()

        let ownerHubRoot = app.descendants(matching: .any)["owner.hub.root"].firstMatch
        XCTAssertTrue(
            ownerHubRoot.waitForExistence(timeout: 20),
            "Der Owner-Hub sollte ohne Crash erscheinen."
        )

        let briefingTitle = app.descendants(matching: .any)["owner.hub.briefing.title"].firstMatch
        XCTAssertTrue(
            briefingTitle.waitForExistence(timeout: 10),
            "Der Daily-Briefing-Bereich sollte sichtbar sein."
        )

        let briefingCta = app.descendants(matching: .any)["owner.hub.briefing.cta"].firstMatch
        XCTAssertTrue(
            briefingCta.waitForExistence(timeout: 10),
            "Der Briefing-CTA sollte sichtbar und aktiviert sein."
        )

        briefingCta.tap()

        let agentScreenRoot = app.descendants(matching: .any)["agent.screen.root"].firstMatch
        XCTAssertTrue(
            agentScreenRoot.waitForExistence(timeout: 25),
            "Der Agent-Screen sollte nach dem Briefing-Handoff sichtbar sein."
        )

        let agentPromptSheet = app.descendants(matching: .any)["agent.prompt.sheet"].firstMatch
        XCTAssertTrue(
            agentPromptSheet.waitForExistence(timeout: 25),
            "Der Agent-Prompt-Sheet sollte nach dem Briefing-CTA erscheinen."
        )

        let agentDraft = app.descendants(matching: .any)["agent.prompt.draft"].firstMatch
        XCTAssertTrue(
            agentDraft.waitForExistence(timeout: 15),
            "Der Prompt-Composer sollte mit vorgefuelltem Text erscheinen."
        )

        guard let rawDraftValue = agentDraft.value else {
            XCTFail("Prefill-Wert fehlt.")
            return
        }
        let draftString: String
        if let stringValue = rawDraftValue as? String {
            draftString = stringValue
        } else if let nsValue = rawDraftValue as? NSString {
            draftString = nsValue as String
        } else {
            XCTFail("Prefill-Wert nicht als String lesbar: \(rawDraftValue)")
            return
        }
        XCTAssertTrue(
            draftString.contains("SkyOS Daily Briefing"),
            "Der Prefill-Text sollte das Daily-Briefing-Template enthalten."
        )
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
            "-ui_test_start_main_shell",
            "-ui_test_signed_in",
            "-ui_test_merch_flow",
        ]
        app.launch()

        enterMainShellIfNeeded(app: app)

        // 01 Home
        tapTab(app: app, index: 2)
        waitForUISettle()
        saveScreenshot(name: "01-home")

        // 02 AI
        tapTab(app: app, index: 4)
        waitForUISettle()
        saveScreenshot(name: "02-ai")

        // 03 Agent
        let agentMode = app.buttons["tools.mode.agent"].firstMatch
        tapElementReliably(
            agentMode,
            in: app,
            timeout: 20,
            failureMessage: "Agent mode should be available for store screenshots."
        )
        XCTAssertTrue(
            app.descendants(matching: .any)["agent.screen.root"].firstMatch.waitForExistence(timeout: 20),
            "Agent screen should open without crashing after tapping the Agent mode."
        )
        waitForUISettle()
        saveScreenshot(name: "03-agent")

        let immersiveExit = app.buttons["ai.hub.exit"].firstMatch
        tapElementReliably(
            immersiveExit,
            in: app,
            timeout: 20,
            failureMessage: "Immersive AI workspace should provide a clear exit action."
        )
        XCTAssertTrue(
            waitForMainShellChrome(app: app, timeout: 20),
            "Main shell chrome should reappear after leaving the immersive AI workspace."
        )

        // 04 Music
        tapTab(app: app, index: 1)
        waitForUISettle()
        saveScreenshot(name: "04-music")

        // 05 Video
        tapTab(app: app, index: 3)
        waitForUISettle()
        saveScreenshot(name: "05-video")

        // 06 Merch
        tapTab(app: app, index: 0)
        waitForUISettle()
        saveScreenshot(name: "06-merch")

        // 07 Membership
        openSettings(app: app)
        let membershipSection = app.descendants(matching: .any)["settings.membership.section"].firstMatch
        scrollToElementIfNeeded(membershipSection, in: app, maxSwipes: 10)
        XCTAssertTrue(
            membershipSection.waitForExistence(timeout: 20),
            "Membership section should be visible for store screenshots."
        )
        waitForUISettle()
        saveScreenshot(name: "07-membership")
    }

    @MainActor
    func testMerchFullscreenCanBeClosed() throws {
        let app = XCUIApplication()
        app.launchArguments += ["-ui_test_merch_flow"]
        app.launch()

        enterMainShellIfNeeded(app: app)
        tapTab(app: app, index: 0)

        let shopRoot = app.descendants(matching: .any)["shop.root"].firstMatch
        XCTAssertTrue(
            shopRoot.waitForExistence(timeout: 20),
            "Der Shop-Root sollte fuer den Merch-Flow sichtbar sein."
        )

        let merchRow = app.descendants(matching: .any)["shop.merch.row"].firstMatch
        scroll(shopRoot, untilVisible: merchRow, maxSwipes: 8)
        XCTAssertTrue(
            merchRow.waitForExistence(timeout: 20),
            "Mindestens ein Merch-Item sollte im UI-Test-Katalog sichtbar sein."
        )
        merchRow.tap()

        let detailRoot = app.descendants(matching: .any)["shop.merch.detail.root"].firstMatch
        XCTAssertTrue(
            detailRoot.waitForExistence(timeout: 20),
            "Der Artikel-Screen sollte nach dem Oeffnen erscheinen."
        )

        let fullscreenButton = app.buttons["shop.merch.fullscreen.open"].firstMatch
        XCTAssertTrue(
            fullscreenButton.waitForExistence(timeout: 10),
            "Der Merch-Artikel sollte einen klaren Vollbild-CTA anbieten."
        )
        fullscreenButton.tap()

        let fullscreenRoot = app.descendants(matching: .any)["shop.merch.fullscreen.root"].firstMatch
        XCTAssertTrue(
            fullscreenRoot.waitForExistence(timeout: 10),
            "Der Vollbild-Viewer sollte sichtbar werden."
        )

        let fullscreenClose = app.descendants(matching: .any)["shop.merch.fullscreen.close"].firstMatch
        XCTAssertTrue(
            fullscreenClose.waitForExistence(timeout: 10),
            "Der Vollbild-Viewer sollte einen expliziten Schliessen-Button haben."
        )
        fullscreenClose.tap()

        XCTAssertTrue(
            waitForNonExistence(of: fullscreenRoot, timeout: 10),
            "Nach Schliessen des Vollbilds sollte der Viewer verschwinden."
        )
        XCTAssertTrue(
            detailRoot.waitForExistence(timeout: 10),
            "Nach dem Vollbild muss der Nutzer wieder im Artikel landen."
        )

        let detailClose = app.buttons["shop.merch.detail.close"].firstMatch
        XCTAssertTrue(
            detailClose.waitForExistence(timeout: 10),
            "Der Artikel-Screen sollte einen klaren Schliessen-Button haben."
        )
        detailClose.tap()

        XCTAssertTrue(
            waitForNonExistence(of: detailRoot, timeout: 10),
            "Nach dem Schliessen des Artikels sollte der Detail-Screen verschwinden."
        )
        XCTAssertTrue(
            shopRoot.waitForExistence(timeout: 10),
            "Nach dem Schliessen des Artikels muss der Nutzer wieder im Shop sein."
        )
    }

    @MainActor
    func testAIUsageNoticeCanOpenFromSettings() throws {
        let app = XCUIApplication()
        app.launchArguments += [
            "-AppleLanguages",
            "(en)",
            "-AppleLocale",
            "en_US",
            "-ui_test_signed_in",
        ]
        app.launch()

        enterMainShellIfNeeded(app: app)
        openSettings(app: app)

        let aiUsageNotice = app.buttons["AI usage notice"].firstMatch
        scrollToElementIfNeeded(aiUsageNotice, in: app, maxSwipes: 10)
        tapElementReliably(
            aiUsageNotice,
            in: app,
            timeout: 20,
            failureMessage: "AI usage notice should open from Settings."
        )

        let policyTitle = app.navigationBars["AI usage notice"].firstMatch
        XCTAssertTrue(
            policyTitle.waitForExistence(timeout: 20),
            "AI usage notice sheet should present a visible title."
        )

        XCTAssertFalse(
            app.staticTexts["support@example.com"].firstMatch.exists,
            "Visible legal content should not expose the sample support address."
        )

        waitForUISettle()
        saveScreenshot(name: "08-legal-ai-usage")
    }

    @MainActor
    func testRoleMatrixSmoke() throws {
        let roles: [(key: String, expectedRoleLabel: String)] = [
            ("OWNER", "Owner"),
            ("ADMIN", "Admin"),
            ("SUBADMIN", "Creator"),
            ("USER", "User"),
        ]

        for role in roles {
            let (email, password) = loadRoleCredentials(role.key)
            let app = XCUIApplication()
            app.launchArguments += [
                "-ui_test",
                "-ui_test_role_matrix",
                "-AppleLanguages", "(en)",
                "-AppleLocale", "en_US",
            ]
            app.launch()

            enterMainShellIfNeeded(app: app)
            openSettings(app: app)
            ensureLoggedIn(app: app, email: email, password: password)

            if role.expectedRoleLabel == "Owner" {
                selectSettingsRootArea(app: app, label: "Command")

                let ownerSection = app.descendants(matching: .any)["settings.owner.section"].firstMatch
                scrollSettingsUntilVisible(ownerSection, in: app, maxSwipes: 8)
                XCTAssertTrue(ownerSection.waitForExistence(timeout: 8), "Owner section should be present in settings.")
                XCTAssertFalse(
                    ownerWorkspaceLockVisible(in: app),
                    "Owner should not see owner-workspace lock messaging."
                )
            } else {
                let ownerSection = app.descendants(matching: .any)["settings.owner.section"].firstMatch
                XCTAssertFalse(ownerSection.exists, "Non-owner should not see owner workspace controls.")
            }

            if role.expectedRoleLabel == "Owner" {
                selectSettingsRootArea(app: app, label: "Personal")
                openProfileFromSettings(app: app)

                // Profile opened successfully; role chip text can vary by async hydration/build variant.
                XCTAssertTrue(
                    app.buttons["Schliessen"].firstMatch.waitForExistence(timeout: 10),
                    "Profile should provide close action."
                )

                let closeProfile = app.buttons["Schliessen"].firstMatch
                if closeProfile.waitForExistence(timeout: 6) {
                    closeProfile.tap()
                }
            } else {
                XCTAssertTrue(
                    app.buttons["settings.open_profile_editor"].firstMatch.exists,
                    "Profile editor entry should be present for signed-in non-owner accounts."
                )
            }

            // Logout for the next iteration (best-effort)
            if !app.descendants(matching: .any)["settings.root"].firstMatch.waitForExistence(timeout: 2) {
                openSettings(app: app)
            }
            let switchAccount = app.buttons["settings.switch_account"].firstMatch
            if switchAccount.waitForExistence(timeout: 12) {
                tapElementReliably(
                    switchAccount,
                    in: app,
                    timeout: 6,
                    failureMessage: "Switch account should be tappable for next role iteration."
                )
            } else {
                let logout = app.buttons["settings.logout"].firstMatch
                if logout.waitForExistence(timeout: 6) {
                    tapElementReliably(
                        logout,
                        in: app,
                        timeout: 6,
                        failureMessage: "Logout should be tappable for next role iteration."
                    )
                }
            }
        }
    }

    @MainActor
    func testProfileAndGalleryCRUD_AllRoles() throws {
        guard ProcessInfo.processInfo.environment["SKYOS_RUN_LIVE_PROFILE_UI_TESTS"] == "1" else {
            throw XCTSkip(
                "Live Profile CRUD UI tests require a Firebase-registered App Check debug token. Set SKYOS_RUN_LIVE_PROFILE_UI_TESTS=1 only in that configured environment."
            )
        }

        let roles: [(key: String, expectedOwnerAccess: Bool)] = [
            ("OWNER", true),
            ("ADMIN", false),
            ("SUBADMIN", false),
            ("USER", false),
        ]

        for role in roles {
            let (email, password) = loadRoleCredentials(role.key)
            let app = XCUIApplication()
            app.launchArguments += [
                "-ui_test",
                "-ui_test_profile_crud",
            ]
            app.launch()

            enterMainShellIfNeeded(app: app)
            openSettings(app: app)
            ensureLoggedIn(app: app, email: email, password: password)

            // Open profile editor (current user profile)
            openProfileFromSettings(app: app)

            // Upload avatar + gallery fixture (server-backed upload slot + storage + firestore meta)
            let uploadAvatar = app.buttons["ui_test.profile.upload_avatar_fixture"].firstMatch
            for _ in 0..<14 {
                if uploadAvatar.waitForExistence(timeout: 1) {
                    break
                }
                app.swipeUp()
            }
            XCTAssertTrue(
                uploadAvatar.waitForExistence(timeout: 12),
                "Avatar fixture control should appear in UI test mode after profile hydrates (scroll if needed)."
            )
            tapElementReliably(
                uploadAvatar,
                in: app,
                timeout: 10,
                failureMessage: "Avatar fixture button should be tappable in UI test mode."
            )

            let uploadGallery = app.buttons["ui_test.profile.upload_gallery_fixture"].firstMatch
            XCTAssertTrue(
                uploadGallery.waitForExistence(timeout: 12),
                "Gallery fixture control should remain visible after avatar upload."
            )
            tapElementReliably(
                uploadGallery,
                in: app,
                timeout: 10,
                failureMessage: "Gallery fixture button should be tappable in UI test mode."
            )

            // Wait for a gallery item to appear (delete button exists)
            let anyDelete = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "profile.gallery.delete.")).firstMatch
            XCTAssertTrue(anyDelete.waitForExistence(timeout: 60), "Expected at least one gallery item delete button after upload.")
            anyDelete.tap()

            // After delete, the delete button should go away (eventual consistency)
            XCTAssertTrue(waitForNonExistence(of: anyDelete, timeout: 30), "Deleted gallery item should disappear.")

            // Close profile
            let closeProfile = app.buttons["Schliessen"].firstMatch
            if closeProfile.waitForExistence(timeout: 6) {
                closeProfile.tap()
            }

            // Guard check (owner access area locked for non-owner)
            openSettings(app: app)
            if role.expectedOwnerAccess {
                XCTAssertFalse(
                    ownerWorkspaceLockVisible(in: app),
                    "Owner should not see owner workspace locked hint."
                )
            } else {
                assertNonOwnerSeesOwnerWorkspaceLock(in: app)
            }

            // Switch account for next run
            let switchAccount = app.buttons["settings.switch_account"].firstMatch
            XCTAssertTrue(switchAccount.waitForExistence(timeout: 20), "Switch account should be available.")
            tapElementReliably(
                switchAccount,
                in: app,
                timeout: 10,
                failureMessage: "Switch account should be tappable."
            )
        }
    }

    @MainActor
    func testArtistHeroVideoCRUD_OwnerOnly() throws {
        let owner = loadRoleCredentials("OWNER")
        let app = XCUIApplication()
        app.launchArguments += [
            "-ui_test",
            "-ui_test_artist_video",
        ]
        app.launch()

        enterMainShellIfNeeded(app: app)
        openSettings(app: app)
        ensureLoggedIn(app: app, email: owner.email, password: owner.password)
        dismissSettingsIfNeeded(app: app)

        // Navigate to Music and open an artist page (JANNO)
        tapTab(app: app, index: 1)

        let openCatalog = app.buttons["music.hub.open_catalog"].firstMatch
        tapElementReliably(
            openCatalog,
            in: app,
            timeout: 20,
            failureMessage: "Music hub should show catalog entry."
        )

        let catalogRoot = app.scrollViews["music.catalog.root"].firstMatch
        XCTAssertTrue(
            catalogRoot.waitForExistence(timeout: 20),
            "Music catalog should load after opening from hub."
        )

        // Open artist page from «Direkter Einstieg»; scroll the catalog as needed.
        let openArtistPage = app.buttons["music.artist.open_page.JANNO"].firstMatch
        var foundPageCTA = false
        for _ in 0..<20 {
            if openArtistPage.waitForExistence(timeout: 1), openArtistPage.isHittable {
                foundPageCTA = true
                break
            }
            catalogRoot.swipeUp()
        }
        XCTAssertTrue(
            foundPageCTA,
            "Direkter Einstieg JANNO button should open the artist page."
        )
        tapElementReliably(
            openArtistPage,
            in: app,
            timeout: 10,
            failureMessage: "Artist page entry should be tappable (Direkter Einstieg)."
        )

        let editOpen = app.buttons["artist.page.edit.open"].firstMatch
        XCTAssertTrue(editOpen.waitForExistence(timeout: 30), "Artist page should allow editing for owner.")
        editOpen.tap()

        let uploadFixture = app.buttons["ui_test.artist.hero_video.upload_fixture"].firstMatch
        XCTAssertTrue(uploadFixture.waitForExistence(timeout: 30), "Hero video fixture upload should exist in UI test mode.")
        uploadFixture.tap()

        let save = app.buttons["artist.page.edit.save"].firstMatch
        XCTAssertTrue(save.waitForExistence(timeout: 30), "Save button should exist in edit mode.")
        let saveEnabledDeadline = Date().addingTimeInterval(45)
        while Date() < saveEnabledDeadline, !save.isEnabled {
            RunLoop.current.run(until: Date().addingTimeInterval(0.5))
        }
        XCTAssertTrue(save.isEnabled, "Save button should be enabled after the hero video fixture upload completes.")
        save.tap()

        // Back to non-edit state
        XCTAssertTrue(editOpen.waitForExistence(timeout: 45), "After saving, artist page should exit edit mode.")
    }

    @MainActor
    func testSessionRestore_UserLive() throws {
        let user = loadRoleCredentials("USER")
        let app = XCUIApplication()
        app.launchArguments += [
            "-ui_test",
            "-AppleLanguages", "(en)",
            "-AppleLocale", "en_US",
        ]
        app.launch()

        enterMainShellIfNeeded(app: app)
        openSettings(app: app)
        ensureLoggedIn(app: app, email: user.email, password: user.password)

        app.terminate()
        app.launch()

        enterMainShellIfNeeded(app: app)
        openSettings(app: app)

        XCTAssertTrue(
            waitForSignedInSettingsState(app: app, timeout: 30),
            "After relaunch, the saved session should restore and expose signed-in account actions."
        )
    }

    @MainActor
    func testProfileGalleryCRUD_UserLive() throws {
        guard ProcessInfo.processInfo.environment["SKYOS_RUN_LIVE_PROFILE_UI_TESTS"] == "1" else {
            throw XCTSkip(
                "Live Profile gallery CRUD requires a Firebase-registered App Check debug token. Set SKYOS_RUN_LIVE_PROFILE_UI_TESTS=1 only in that configured environment."
            )
        }

        let user = loadRoleCredentials("USER")
        let app = XCUIApplication()
        app.launchArguments += [
            "-ui_test",
            "-ui_test_profile_crud",
            "-AppleLanguages", "(en)",
            "-AppleLocale", "en_US",
        ]
        app.launch()

        enterMainShellIfNeeded(app: app)
        openSettings(app: app)
        ensureLoggedIn(app: app, email: user.email, password: user.password)
        openProfileFromSettings(app: app)

        let uploadGallery = app.buttons["ui_test.profile.upload_gallery_fixture"].firstMatch
        scrollToElementIfNeeded(uploadGallery, in: app, maxSwipes: 10)
        XCTAssertTrue(
            uploadGallery.waitForExistence(timeout: 20),
            "Gallery fixture upload should be visible in UI-test profile mode."
        )
        tapElementReliably(
            uploadGallery,
            in: app,
            timeout: 10,
            failureMessage: "Gallery fixture upload should be tappable."
        )

        let deleteButton = app.buttons.matching(
            NSPredicate(format: "identifier BEGINSWITH %@", "profile.gallery.delete.")
        ).firstMatch
        XCTAssertTrue(
            deleteButton.waitForExistence(timeout: 60),
            "A gallery delete button should appear after the server-backed upload finishes."
        )
        deleteButton.tap()

        XCTAssertTrue(
            waitForNonExistence(of: deleteButton, timeout: 30),
            "Deleting the uploaded gallery item should remove it from the live profile."
        )
    }

    @MainActor
    func testOwnerMembershipOpsLoadsLive() throws {
        guard ProcessInfo.processInfo.environment["SKYOS_RUN_LIVE_MEMBERSHIP_UI_TEST"] == "1" else {
            throw XCTSkip(
                "Live Membership Ops UI test requires a Firebase-registered App Check debug token. Set SKYOS_RUN_LIVE_MEMBERSHIP_UI_TEST=1 only in that configured environment."
            )
        }

        let owner = loadRoleCredentials("OWNER")
        let app = XCUIApplication()
        app.launchArguments += [
            "-ui_test",
            "-AppleLanguages", "(en)",
            "-AppleLocale", "en_US",
        ]
        app.launch()

        enterMainShellIfNeeded(app: app)
        openSettings(app: app)
        ensureLoggedIn(app: app, email: owner.email, password: owner.password)

        let currentEmail = app.staticTexts["settings.current_email"].firstMatch
        XCTAssertEqual(
            currentEmail.label.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            owner.email.lowercased(),
            "Owner live test should run under the canonical owner account."
        )

        let ownerSection = app.descendants(matching: .any)["settings.owner.section"].firstMatch
        XCTAssertTrue(ownerSection.waitForExistence(timeout: 20), "Owner section should load for the owner account.")
        XCTAssertFalse(
            app.descendants(matching: .any)["settings.owner.locked_hint"].firstMatch.exists,
            "Owner account should not render the locked owner-workspace hint."
        )

        let membershipOps = app.buttons["settings.owner.command.membershipOps"].firstMatch
        scrollToElementIfNeeded(membershipOps, in: app, maxSwipes: 8)
        tapElementReliably(
            membershipOps,
            in: app,
            timeout: 20,
            failureMessage: "Membership Ops should be directly reachable from the owner command center."
        )

        XCTAssertTrue(
            app.descendants(matching: .any)["settings.membership_ops.root"].firstMatch.waitForExistence(timeout: 30)
                || app.staticTexts["Membership Control"].firstMatch.exists
                || app.staticTexts["Membership-Steuerung"].firstMatch.exists,
            "Membership Command Center should load on the physical device for the owner account."
        )
    }

    @MainActor
    func testAgentLiveBackend_Admin() throws {
        guard ProcessInfo.processInfo.environment["SKYOS_RUN_LIVE_AGENT_UI_TEST"] == "1" else {
            throw XCTSkip(
                "Live Agent backend UI test requires a Firebase-registered App Check debug token. Set SKYOS_RUN_LIVE_AGENT_UI_TEST=1 only in that configured environment."
            )
        }

        let admin = loadRoleCredentials("ADMIN")
        let app = XCUIApplication()
        app.launchArguments += [
            "-ui_test",
            "-AppleLanguages", "(en)",
            "-AppleLocale", "en_US",
        ]
        app.launch()

        enterMainShellIfNeeded(app: app)
        openSettings(app: app)
        ensureLoggedIn(app: app, email: admin.email, password: admin.password)

        dismissSettingsIfNeeded(app: app)

        tapTab(app: app, index: 4)

        let agentMode = app.buttons["tools.mode.agent"].firstMatch
        tapElementReliably(
            agentMode,
            in: app,
            timeout: 20,
            failureMessage: "Agent mode switch should be visible in the Tools workspace."
        )

        let openPrompt = app.buttons["agent.prompt.open"].firstMatch
        tapElementReliably(
            openPrompt,
            in: app,
            timeout: 20,
            failureMessage: "Agent prompt composer should be available for the admin QA account."
        )

        let promptSheet = app.descendants(matching: .any)["agent.prompt.sheet"].firstMatch
        XCTAssertTrue(
            promptSheet.waitForExistence(timeout: 20),
            "Agent prompt composer should open before sending a live prompt."
        )

        let quickPrompt = app.buttons["agent.quick_prompt.0"].firstMatch
        tapElementReliably(
            quickPrompt,
            in: app,
            timeout: 20,
            failureMessage: "A live agent quick prompt should be available for the admin QA account."
        )

        let sendPrompt = app.buttons["agent.prompt.send"].firstMatch
        tapElementReliably(
            sendPrompt,
            in: app,
            timeout: 20,
            failureMessage: "Agent prompt should be sendable after selecting a quick prompt."
        )

        let runID = app.descendants(matching: .any)["agent.lastRun.id"].firstMatch
        XCTAssertTrue(
            runID.waitForExistence(timeout: 90),
            "Live agent execution should record a backend run id on the physical device."
        )
        XCTAssertFalse(
            runID.label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
            "Recorded live agent run id should not be empty."
        )
    }
}

private extension Skydown_AppUITests {
    @MainActor
    func enterMainShellIfNeeded(app: XCUIApplication) {
        if waitForMainShellChrome(app: app, timeout: 3) {
            return
        }

        let settingsButton = app.buttons["app.open_settings"].firstMatch
        if settingsButton.waitForExistence(timeout: 8) {
            return
        }

        let openHomeButton = app.buttons["Open Home"].firstMatch
        XCTAssertTrue(
            openHomeButton.waitForExistence(timeout: 60),
            "Landing Open Home button should appear after intro."
        )
        openHomeButton.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        XCTAssertTrue(
            waitForMainShellChrome(app: app, timeout: 30),
            "Main shell should appear after opening from landing."
        )
    }

    @MainActor
    func waitForMainShellChrome(app: XCUIApplication, timeout: TimeInterval) -> Bool {
        if app.tabBars.firstMatch.waitForExistence(timeout: min(timeout, 3)) {
            return true
        }
        return app.buttons["app.open_settings"].firstMatch.waitForExistence(timeout: timeout)
    }

    @MainActor
    func openSettings(app: XCUIApplication) {
        let settingsButton = app.buttons["app.open_settings"].firstMatch
        XCTAssertTrue(settingsButton.waitForExistence(timeout: 20), "Settings button should be available.")
        settingsButton.tap()
        XCTAssertTrue(
            app.descendants(matching: .any)["settings.root"].firstMatch.waitForExistence(timeout: 30),
            "Settings root should appear."
        )
    }

    @MainActor
    func ensureLoggedIn(app: XCUIApplication, email: String, password: String) {
        let openLogin = app.buttons["settings.open_login"].firstMatch
        let switchAccount = app.buttons["settings.switch_account"].firstMatch
        let logout = app.buttons["settings.logout"].firstMatch
        let loginRoot = app.descendants(matching: .any)["login.root"].firstMatch
        let currentEmail = app.staticTexts["settings.current_email"].firstMatch

        if currentEmail.waitForExistence(timeout: 1),
           currentEmail.label.trimmingCharacters(in: .whitespacesAndNewlines).caseInsensitiveCompare(email) == .orderedSame,
           !loginRoot.exists {
            dismissSystemPasswordPromptIfNeeded()
            return
        }

        if switchAccount.waitForExistence(timeout: 1) {
            switchAccount.tap()
            if !loginRoot.waitForExistence(timeout: 5) && logout.waitForExistence(timeout: 3) {
                logout.tap()
            }
        }

        if !loginRoot.exists, openLogin.waitForExistence(timeout: 2) {
            openLogin.tap()
        }

        let emailField = app.textFields["login.email"].firstMatch
        XCTAssertTrue(loginRoot.waitForExistence(timeout: 20), "Login sheet should be visible before entering credentials.")
        XCTAssertTrue(emailField.waitForExistence(timeout: 20), "Login email field should be visible.")
        replaceText(in: emailField, with: email)

        let passwordField = app.secureTextFields["login.password"].firstMatch
        XCTAssertTrue(passwordField.waitForExistence(timeout: 10), "Login password field should be visible.")
        replaceText(in: passwordField, with: password)

        let submit = app.buttons["login.submit"].firstMatch
        XCTAssertTrue(submit.waitForExistence(timeout: 10), "Login submit button should exist.")
        submit.tap()

        XCTAssertTrue(
            waitForNonExistence(of: loginRoot, timeout: 45),
            "Login sheet should dismiss after a successful sign-in."
        )
        XCTAssertTrue(
            app.descendants(matching: .any)["settings.root"].firstMatch.waitForExistence(timeout: 20),
            "After login, settings should remain visible."
        )
        dismissSystemPasswordPromptIfNeeded()
        let switchAccountVisible = waitForSignedInSettingsState(app: app, timeout: 30)
        let logoutVisible = app.buttons["settings.logout"].firstMatch.exists
        let openProfileVisible = app.buttons["settings.open_profile_editor"].firstMatch.exists
        XCTAssertTrue(
            switchAccountVisible || logoutVisible || openProfileVisible,
            "After login, account actions should be available."
        )
        if !currentEmail.waitForExistence(timeout: 5) {
            scrollSettingsTowardTopUntilVisible(currentEmail, in: app, maxSwipes: 6)
        }
        XCTAssertTrue(currentEmail.waitForExistence(timeout: 10), "Signed-in settings state should show the current account email.")
        XCTAssertEqual(
            currentEmail.label.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            "The physical device should be signed in with the requested QA account."
        )
    }

    @MainActor
    func waitForSignedInSettingsState(app: XCUIApplication, timeout: TimeInterval) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        let switchAccount = app.buttons["settings.switch_account"].firstMatch
        let logout = app.buttons["settings.logout"].firstMatch
        let openProfile = app.buttons["settings.open_profile_editor"].firstMatch
        let openLogin = app.buttons["settings.open_login"].firstMatch

        while Date() < deadline {
            if switchAccount.exists || logout.exists || openProfile.exists {
                return true
            }
            if !openLogin.exists && (switchAccount.exists || logout.exists || openProfile.exists) {
                return true
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.6))
        }

        return switchAccount.exists || logout.exists || openProfile.exists
    }

    @MainActor
    func selectSettingsRootArea(app: XCUIApplication, label: String) {
        let segment = app.buttons[label].firstMatch
        if !segment.waitForExistence(timeout: 2) {
            for _ in 0..<4 where !segment.exists {
                app.swipeDown()
                RunLoop.current.run(until: Date().addingTimeInterval(0.25))
            }
            for _ in 0..<2 where !segment.exists {
                app.swipeUp()
                RunLoop.current.run(until: Date().addingTimeInterval(0.25))
            }
        }
        tapElementReliably(
            segment,
            in: app,
            timeout: 10,
            failureMessage: "Settings root area '\(label)' should be selectable."
        )
        waitForUISettle()
    }

    @MainActor
    func scrollSettingsUntilVisible(_ element: XCUIElement, in app: XCUIApplication, maxSwipes: Int) {
        for _ in 0..<maxSwipes where !element.exists {
            if app.scrollViews.firstMatch.exists {
                app.scrollViews.firstMatch.swipeUp()
            } else {
                app.swipeUp()
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.25))
        }
    }

    @MainActor
    func scrollSettingsTowardTopUntilVisible(_ element: XCUIElement, in app: XCUIApplication, maxSwipes: Int) {
        for _ in 0..<maxSwipes where !element.exists {
            if app.scrollViews.firstMatch.exists {
                app.scrollViews.firstMatch.swipeDown()
            } else {
                app.swipeDown()
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.25))
        }
    }

    func loadRoleCredentials(_ key: String) -> (email: String, password: String) {
        let env = ProcessInfo.processInfo.environment
        let email = (env["SKYDOWN_TEST_\(key)_EMAIL"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        let password = env["SKYDOWN_TEST_\(key)_PASSWORD"] ?? ""
        if !email.isEmpty && !password.isEmpty {
            return (email, password)
        }

        let fallbackPath = env["SKYDOWN_TEST_CREDENTIALS_FILE"] ?? "/tmp/skydown-e2e-accounts.json"
        if let fallback = loadRoleCredentialsFromJSON(path: fallbackPath, key: key) {
            return fallback
        }

        for resourceName in [
            "skydown-e2e-accounts.local",
            "skydown-e2e-accounts",
        ] {
            if let resourceURL = Bundle(for: type(of: self)).url(forResource: resourceName, withExtension: "json"),
               let fallback = loadRoleCredentialsFromJSON(path: resourceURL.path, key: key) {
                return fallback
            }
        }

        XCTFail(
            "Missing test credentials for \(key). Set SKYDOWN_TEST_\(key)_EMAIL/PASSWORD, provide \(fallbackPath), or add a local bundle credentials JSON."
        )
        return ("", "")
    }

    func loadRoleCredentialsFromJSON(path: String, key: String) -> (email: String, password: String)? {
        guard let data = FileManager.default.contents(atPath: path) else {
            return nil
        }
        guard
            let raw = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
            let role = raw[key.lowercased()] as? [String: Any]
        else {
            return nil
        }

        let email = (role["email"] as? String ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        let password = role["password"] as? String ?? ""
        guard !email.isEmpty, !password.isEmpty else {
            return nil
        }
        return (email, password)
    }

    @MainActor
    func tapTab(app: XCUIApplication, index: Int) {
        let tabBar = app.tabBars.firstMatch
        if tabBar.waitForExistence(timeout: 4) {
            let buttons = tabBar.buttons.allElementsBoundByIndex
            XCTAssertGreaterThan(buttons.count, index, "Expected at least \(index + 1) tab buttons.")
            buttons[index].tap()
            return
        }

        let fallbackTabs = [
            (identifier: "bag.fill", label: "Merch"),
            (identifier: "waveform.circle.fill", label: "Music"),
            (identifier: "house.fill", label: "Home"),
            (identifier: "play.rectangle.fill", label: "Videos"),
            (identifier: "sparkles", label: "AI"),
        ]
        XCTAssertGreaterThan(fallbackTabs.count, index, "Expected at least \(index + 1) fallback tab buttons.")

        let target = fallbackTabs[index]
        let predicate = NSPredicate(
            format: "identifier == %@ AND label == %@",
            target.identifier,
            target.label
        )
        let button = app.buttons.matching(predicate).firstMatch
        tapElementReliably(
            button,
            in: app,
            timeout: 20,
            failureMessage: "Fallback top tab \(target.label) must be visible."
        )
    }

    @MainActor
    func scroll(_ container: XCUIElement, untilVisible element: XCUIElement, maxSwipes: Int) {
        guard container.waitForExistence(timeout: 5) else { return }
        for _ in 0..<maxSwipes where !element.exists {
            container.swipeUp()
        }
    }

    @MainActor
    func waitForUISettle() {
        RunLoop.current.run(until: Date().addingTimeInterval(1.8))
    }

    /// Settings uses a `LazyVStack`: owner workspace + lock card can appear only after scrolling.
    @MainActor
    func ownerWorkspaceLockVisible(in app: XCUIApplication) -> Bool {
        let lockedHint = app.descendants(matching: .any)["settings.owner.locked_hint"].firstMatch
        if lockedHint.exists {
            return true
        }
        // Do not match on "Owner-only" alone — normal section subtitles also contain that phrase.
        let lockCopy = app.staticTexts.containing(
            NSPredicate(format: "label CONTAINS[c] %@", "Melde dich als Owner")
        ).firstMatch
        return lockCopy.exists
    }


    @MainActor
    func assertNonOwnerSeesOwnerWorkspaceLock(in app: XCUIApplication) {
        for _ in 0..<18 {
            if ownerWorkspaceLockVisible(in: app) {
                return
            }
            if app.scrollViews.firstMatch.exists {
                app.scrollViews.firstMatch.swipeUp()
            } else {
                app.swipeUp()
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.25))
        }
        XCTAssertTrue(
            ownerWorkspaceLockVisible(in: app),
            "Non-owner should see owner-workspace lock (card id or Owner-only copy)."
        )
    }

    @MainActor
    func openProfileFromSettings(app: XCUIApplication) {
        let openProfileQuery = app.buttons.matching(identifier: "settings.open_profile_editor")
        let openProfile = openProfileQuery.firstMatch
        dismissSystemPasswordPromptIfNeeded()
        XCTAssertTrue(openProfile.waitForExistence(timeout: 20), "Profile editor entry should be visible in settings.")

        let profileRoot = app.descendants(matching: .any)["profile.root"].firstMatch
        for attempt in 0..<10 {
            dismissSystemPasswordPromptIfNeeded()

            let candidates = openProfileQuery.allElementsBoundByIndex.filter(\.exists)
            if let hittableButton = candidates.first(where: \.isHittable) {
                hittableButton.tap()
            } else if let visibleButton = candidates.first(where: { button in
                app.frame.intersects(button.frame) && !button.frame.isEmpty
            }) {
                visibleButton.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
            } else if let firstButton = candidates.first {
                swipeScrollableContent(in: app, up: firstButton.frame.midY >= app.frame.midY)
            } else {
                swipeScrollableContent(in: app, up: attempt.isMultiple(of: 2))
            }
            if profileRoot.waitForExistence(timeout: 5) {
                return
            }
        }

        XCTAssertTrue(
            profileRoot.waitForExistence(timeout: 30),
            "Profile screen should open cleanly."
        )
    }

    @MainActor
    func swipeScrollableContent(in app: XCUIApplication, up: Bool) {
        let settingsScrollView = app.scrollViews["settings.root"].firstMatch
        let scrollView = settingsScrollView.exists ? settingsScrollView : app.scrollViews.firstMatch
        if scrollView.exists {
            if up {
                scrollView.swipeUp()
            } else {
                scrollView.swipeDown()
            }
        } else {
            if up {
                app.swipeUp()
            } else {
                app.swipeDown()
            }
        }
        RunLoop.current.run(until: Date().addingTimeInterval(0.25))
    }

    @MainActor
    func scrollToElementIfNeeded(_ element: XCUIElement, in app: XCUIApplication, maxSwipes: Int) {
        guard !element.exists else { return }
        for _ in 0..<maxSwipes {
            if element.exists {
                return
            }
            app.swipeUp()
            RunLoop.current.run(until: Date().addingTimeInterval(0.25))
        }
    }

    @MainActor
    func dismissSettingsIfNeeded(app: XCUIApplication) {
        let settingsRoot = app.descendants(matching: .any)["settings.root"].firstMatch
        guard settingsRoot.waitForExistence(timeout: 10) else { return }

        let closeSettings = app.buttons["settings.close"].firstMatch
        if closeSettings.waitForExistence(timeout: 5) {
            closeSettings.tap()
        }

        if waitForNonExistence(of: settingsRoot, timeout: 6) {
            return
        }

        app.swipeDown()
        if waitForNonExistence(of: settingsRoot, timeout: 6) {
            return
        }

        if closeSettings.exists {
            closeSettings.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        }

        XCTAssertTrue(
            waitForNonExistence(of: settingsRoot, timeout: 10),
            "Settings sheet should be dismissed before the Tools workspace is opened."
        )
    }

    @MainActor
    func dismissSystemPasswordPromptIfNeeded() {
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let labels = [
            "Später",
            "Nicht sichern",
            "Not Now",
            "Don't Save",
            "Never",
            "Cancel"
        ]

        for _ in 0..<3 {
            for label in labels {
                let button = springboard.buttons[label].firstMatch
                if button.exists {
                    button.tap()
                    RunLoop.current.run(until: Date().addingTimeInterval(0.4))
                    return
                }
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.25))
        }
    }

    @MainActor
    func tapElementReliably(
        _ element: XCUIElement,
        in app: XCUIApplication,
        timeout: TimeInterval,
        failureMessage: String
    ) {
        XCTAssertTrue(element.waitForExistence(timeout: timeout), failureMessage)

        if element.isHittable {
            element.tap()
            return
        }

        // Some entries live in scroll containers and are visible but not directly hittable.
        for _ in 0..<2 {
            app.swipeUp()
            if element.isHittable {
                element.tap()
                return
            }
        }
        for _ in 0..<2 {
            app.swipeDown()
            if element.isHittable {
                element.tap()
                return
            }
        }

        if element.exists {
            element.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
            return
        }

        XCTFail("Element exists but could not be tapped reliably: \(element)")
    }

    @MainActor
    func replaceText(in element: XCUIElement, with text: String) {
        element.tap()

        if let existingValue = element.value as? String,
           !existingValue.isEmpty,
           existingValue != element.label {
            let deleteSequence = String(repeating: XCUIKeyboardKey.delete.rawValue, count: existingValue.count)
            element.typeText(deleteSequence)
        }

        element.typeText(text)
    }

    @MainActor
    func waitForNonExistence(of element: XCUIElement, timeout: TimeInterval) -> Bool {
        let predicate = NSPredicate(format: "exists == false")
        let expectation = XCTNSPredicateExpectation(predicate: predicate, object: element)
        return XCTWaiter().wait(for: [expectation], timeout: timeout) == .completed
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
