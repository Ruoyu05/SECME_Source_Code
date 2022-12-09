//
//  SECMEApp.swift
//  SECME
//
//  Created by cmStudent on 2022/11/18.
//

import SwiftUI


@main
struct SECMEApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(WebSocketClient())
        }
    }
}
