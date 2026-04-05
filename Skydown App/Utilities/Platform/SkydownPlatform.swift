import Foundation

enum SkydownPlatform {
    static var isDesktop: Bool {
        #if targetEnvironment(macCatalyst)
        true
        #else
        false
        #endif
    }

    static var supportsInAppMailComposer: Bool {
        #if targetEnvironment(macCatalyst)
        false
        #else
        true
        #endif
    }
}
