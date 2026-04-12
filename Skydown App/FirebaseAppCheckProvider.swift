import Foundation
import FirebaseAppCheck
import FirebaseCore

final class SkydownAppCheckProviderFactory: NSObject, AppCheckProviderFactory {
    func createProvider(with app: FirebaseApp) -> AppCheckProvider? {
#if targetEnvironment(simulator)
        return AppCheckDebugProvider(app: app)
#else
        if ProcessInfo.processInfo.arguments.contains("-app-check-debug")
            || ProcessInfo.processInfo.environment["SKYDOWN_APP_CHECK_DEBUG"] == "1" {
            return AppCheckDebugProvider(app: app)
        }

        // Default to DeviceCheck for production stability on real devices.
        // App Attest can be re-enabled later behind an explicit rollout flag.
        return DeviceCheckProvider(app: app)
#endif
    }
}
