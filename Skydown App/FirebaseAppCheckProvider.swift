import Foundation
import FirebaseAppCheck
import FirebaseCore

final class SkydownAppCheckProviderFactory: NSObject, AppCheckProviderFactory {
    func createProvider(with app: FirebaseApp) -> AppCheckProvider? {
#if DEBUG
        return AppCheckDebugProvider(app: app)
#elseif targetEnvironment(simulator)
        return AppCheckDebugProvider(app: app)
#else
        if #available(iOS 14.0, *) {
            return AppAttestProvider(app: app)
        }

        return DeviceCheckProvider(app: app)
#endif
    }
}
