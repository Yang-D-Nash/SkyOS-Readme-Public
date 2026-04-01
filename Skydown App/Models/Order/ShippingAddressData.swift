import Foundation

struct ShippingAddressData: Codable, Equatable {
    var address1: String
    var address2: String = ""
    var city: String
    var zip: String
    var countryCode: String
    var countryName: String
}
