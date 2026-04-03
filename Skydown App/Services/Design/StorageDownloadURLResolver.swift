import Foundation
import FirebaseStorage

extension StorageReference {
    func awaitStableDownloadURL(
        attempts: Int = 4,
        initialDelayNanoseconds: UInt64 = 250_000_000
    ) async throws -> URL {
        var nextDelay = initialDelayNanoseconds

        for attempt in 0..<attempts {
            do {
                return try await fetchDownloadURL()
            } catch {
                guard shouldRetryDownloadURL(error), attempt < attempts - 1 else {
                    throw mapDownloadURLError(error)
                }

                try await Task.sleep(nanoseconds: nextDelay)
                nextDelay *= 2
            }
        }

        throw NSError(
            domain: "StorageDownloadURLResolver",
            code: 500,
            userInfo: [NSLocalizedDescriptionKey: "Download-URL konnte nicht geladen werden."]
        )
    }

    private func fetchDownloadURL() async throws -> URL {
        try await withCheckedThrowingContinuation { continuation in
            downloadURL { url, error in
                if let error {
                    continuation.resume(throwing: error)
                } else if let url {
                    continuation.resume(returning: url)
                } else {
                    continuation.resume(
                        throwing: NSError(
                            domain: "StorageDownloadURLResolver",
                            code: 500,
                            userInfo: [NSLocalizedDescriptionKey: "Download-URL fehlt."]
                        )
                    )
                }
            }
        }
    }
}

private func shouldRetryDownloadURL(_ error: Error) -> Bool {
    let nsError = error as NSError
    guard nsError.domain == StorageErrorDomain else { return false }
    return StorageErrorCode(rawValue: nsError.code) == .objectNotFound
}

private func mapDownloadURLError(_ error: Error) -> Error {
    let nsError = error as NSError
    guard nsError.domain == StorageErrorDomain,
          let code = StorageErrorCode(rawValue: nsError.code) else {
        return error
    }

    switch code {
    case .objectNotFound:
        return NSError(
            domain: "StorageDownloadURLResolver",
            code: 404,
            userInfo: [NSLocalizedDescriptionKey: "Bild wurde hochgeladen, aber die Download-URL ist noch nicht verfuegbar. Bitte gleich nochmal versuchen."]
        )
    case .unauthenticated, .unauthorized:
        return NSError(
            domain: "StorageDownloadURLResolver",
            code: 403,
            userInfo: [NSLocalizedDescriptionKey: "Upload wurde gespeichert, aber Firebase blockiert den Zugriff auf die Bild-URL."]
        )
    default:
        return error
    }
}
