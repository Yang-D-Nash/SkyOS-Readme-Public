//
//  MailView.swift
//  Skydown App
//
//  Created by Yang D. Nash on 27.08.25.
//

import MessageUI
import SwiftUI

struct MailView: UIViewControllerRepresentable {
    @Environment(\.dismiss) private var dismiss
    
    let subject: String
    let body: String
    let recipients: [String]
    
    class Coordinator: NSObject, MFMailComposeViewControllerDelegate {
        var parent: MailView
        
        init(_ parent: MailView) {
            self.parent = parent
        }
        
        func mailComposeController(_ controller: MFMailComposeViewController,
                                   didFinishWith result: MFMailComposeResult,
                                   error: Error?) {
            controller.dismiss(animated: true) {
                self.parent.dismiss()
            }
        }
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    func makeUIViewController(context: Context) -> MFMailComposeViewController {
        let viewController = MFMailComposeViewController()
        viewController.setSubject(subject)
        viewController.setMessageBody(body, isHTML: false)
        viewController.setToRecipients(recipients)
        viewController.mailComposeDelegate = context.coordinator
        return viewController
    }
    
    func updateUIViewController(_ uiViewController: MFMailComposeViewController, context: Context) {}
}
