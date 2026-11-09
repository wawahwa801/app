import SwiftUI

@main
struct SpeedbumpApp: App {
    var body: some Scene {
        WindowGroup {
            SetupView()
        }
    }
}

struct SetupView: View {
    var body: some View {
        NavigationStack {
            List {
                Section("Turn it on") {
                    Label("Open the Settings app, then Apps, then Safari, then Extensions", systemImage: "1.circle")
                    Label("Turn on Speedbump", systemImage: "2.circle")
                    Label("Allow it on instagram.com and youtube.com", systemImage: "3.circle")
                    Label("In Safari, tap the extensions icon, then Speedbump, to choose your settings", systemImage: "4.circle")
                }
                Section("Use the websites") {
                    Text("Speedbump works in Safari on instagram.com and youtube.com.")
                }
                Section("What iPhone can't do") {
                    Text("iOS doesn't let any app change what's inside the Instagram, YouTube or WeChat apps. Speedbump can't block sections in those apps, and it can't reach WeChat at all.")
                }
            }
            .navigationTitle("Speedbump")
        }
    }
}
