import Foundation
import FirebaseAppCheck
import FirebaseCore

final class SkydownAppCheckProviderFactory: NSObject, AppCheckProviderFactory {
    func createProvider(with app: FirebaseApp) -> AppCheckProvider? {
#if targetEnvironment(simulator)
        return AppCheckDebugProvider(app: app)
#else
        // Release-safe default until App Attest capability rollout is fully verified.
        return DeviceCheckProvider(app: app)
#endif
    }
}
