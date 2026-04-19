import Foundation
import FirebaseAppCheck
import FirebaseCore

final class SkydownAppCheckProviderFactory: NSObject, AppCheckProviderFactory {
    func createProvider(with app: FirebaseApp) -> AppCheckProvider? {
#if targetEnvironment(simulator)
        return AppCheckDebugProvider(app: app)
#else
        // Real devices must use a production provider even in Debug builds,
        // otherwise installed test builds break as soon as App Check is enforced.
        return DeviceCheckProvider(app: app)
#endif
    }
}
