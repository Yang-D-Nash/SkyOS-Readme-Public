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

        if #available(iOS 14.0, *) {
            return AppAttestProvider(app: app)
        }

        return DeviceCheckProvider(app: app)
#endif
    }
}
