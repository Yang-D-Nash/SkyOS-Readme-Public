//
//  Skydown_AppTests.swift
//  Skydown AppTests
//
//  Created by Yang D. Nash on 23.07.25.
//

import Testing
@testable import Skydown_App

struct Skydown_AppTests {

    @Test func sanitizedUsernamePrefersPreferredValueAndClampsToFirestoreLimit() async throws {
        let longName = "  This Display Name Is Definitely Longer Than Thirty Two Chars  "

        let sanitized = FirebaseAuthService.sanitizedUsername(
            longName,
            authUserDisplayName: "Ignored Display Name",
            fallbackEmail: "fallback@example.com"
        )

        #expect(sanitized == "This Display Name Is Definitely")
        #expect(sanitized.count <= 32)
    }

    @Test func sanitizedUsernameFallsBackToEmailPrefixWhenNeeded() async throws {
        let sanitized = FirebaseAuthService.sanitizedUsername(
            nil,
            authUserDisplayName: "   ",
            fallbackEmail: "nash.lioncorna@gmail.com"
        )

        #expect(sanitized == "nash.lioncorna")
    }

    @Test func ownerEmailAlwaysResolvesToOwnerRole() async throws {
        let role = UserRole.resolve(
            from: nil,
            isAdmin: false,
            email: "NASH.LIONCORNA@GMAIL.COM"
        )

        #expect(role == .owner)
        #expect(role.hasStaffAccess)
        #expect(role.hasAdminWorkspaceAccess)
    }

    @Test func screenHeaderSettingsCountOnlyConfiguredSurfaces() async throws {
        let settings = ScreenHeaderSettings(
            homeImageURL: "https://example.com/home.jpg",
            homeEyebrow: "",
            homeTitle: "",
            homeSubtitle: "",
            homeDetail: "",
            musicHubImageURL: "",
            musicHubEyebrow: "Music",
            musicHubTitle: "Hub",
            musicHubSubtitle: "",
            musicHubDetail: "",
            shopImageURL: "",
            shopEyebrow: "",
            shopTitle: "",
            shopSubtitle: "",
            shopDetail: "",
            videoHubImageURL: "",
            videoHubEyebrow: "",
            videoHubTitle: "",
            videoHubSubtitle: "",
            videoHubDetail: ""
        )

        #expect(settings.configuredCount == 2)
        #expect(settings.resolvedHomeImageURL == "https://example.com/home.jpg")
        #expect(settings.resolvedMusicHubEyebrow == "Music")
    }

}
