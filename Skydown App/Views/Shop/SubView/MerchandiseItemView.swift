//
//  MerchandiseItemView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 24.07.25.
//
import SwiftUI


struct MerchandiseItemView: View {
    let item: MerchandiseItem

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 15) {
                    ForEach(item.imageURLs, id: \.self) { urlString in
                        AsyncImage(url: URL(string: urlString)) { image in
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(width: 250, height: 250)
                                .clipped()
                                .cornerRadius(10)
                        } placeholder: {
                            ProgressView()
                                .frame(width: 250, height: 250)
                                .background(Color(.systemGray6))
                                .cornerRadius(10)
                        }
                    }
                }
                .padding(.horizontal, 10)
            }
            
            Text(item.name)
                .font(.headline)
            
            Text("€\(item.price, specifier: "%.2f")")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .padding(.vertical, 10)
    }
}

#Preview {
    let mockItem = MerchandiseItem(
        id: "1",
        name: "Exklusives T-Shirt",
        price: 24.99,
        description: "Ein super cooles T-Shirt für jeden Anlass.",
        imageURLs: [
            "https://i.imgur.com/8QG3tQJ.png",
            "https://i.imgur.com/G20y5iQ.png",
            "https://i.imgur.com/R38w6kS.png"
        ],
        available: true
    )
    
    MerchandiseItemView(item: mockItem)
        .padding()
}
