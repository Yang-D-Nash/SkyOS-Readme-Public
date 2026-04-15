import Foundation
import FirebaseAppCheck
import FirebaseCore

final class SkydownAppCheckProviderFactory: NSObject, AppCheckProviderFactory {
    func createProvider(with app: FirebaseApp) -> AppCheckProvider? {
#if DEBUG || targetEnvironment(simulator)
        return AppCheckDebugProvider(app: app)
#else
        // Release builds on real devices must never rely on debug tokens.
        // DeviceCheck is used as the stable production provider.
        return DeviceCheckProvider(app: app)
#endif
    }
}
