import ReplayKit
import SwiftUI
import UserNotifications

@main
struct SpeedbumpApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var delegate
    @StateObject private var router = WallRouter.shared

    var body: some Scene {
        WindowGroup {
            SetupView()
                .fullScreenCover(item: $router.site) { site in
                    WallView(site: site)
                }
                .onOpenURL { url in
                    // speedbump://wall?site=instagram
                    let items = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems
                    router.site = items?.first { $0.name == "site" }?.value.flatMap(Site.init(rawValue:))
                }
        }
    }
}

extension Site: Identifiable {
    var id: String { rawValue }
}

final class WallRouter: ObservableObject {
    static let shared = WallRouter()
    @Published var site: Site?
}

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let name = response.notification.request.content.userInfo["site"] as? String
        DispatchQueue.main.async {
            WallRouter.shared.site = name.flatMap(Site.init(rawValue:))
        }
        completionHandler()
    }
}

struct SetupView: View {
    private let store = SharedStore()
    @State private var mode = SharedStore().mode
    @State private var unlockMinutes = SharedStore().unlockMinutes
    @State private var notificationsAllowed = false
    @State private var instagram = SharedStore().isEnabled(.instagram)
    @State private var youtube = SharedStore().isEnabled(.youtube)

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text("Without a paid Apple developer account, iOS won't let Speedbump close Reels or Shorts. It notices them and keeps alerting you until you go through the wall or leave.")
                }
                Section("Watch for Reels and Shorts") {
                    if !notificationsAllowed {
                        Button("Allow notifications") { requestNotifications() }
                    }
                    BroadcastButton()
                        .frame(height: 44)
                    Text("Your screen is read on this phone and never saved or sent. You'll need to start this again after locking your phone. In Settings, under Notifications, turn on Screen Sharing so alerts can appear.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    if let last = store.lastFrameAt {
                        Text("Last checked your screen \(last.formatted(.relative(presentation: .named)))")
                            .font(.footnote)
                    }
                }
                Section("Sections") {
                    Toggle(Site.instagram.sectionName, isOn: $instagram)
                        .onChange(of: instagram) { store.setEnabled(.instagram, $0) }
                    Toggle(Site.youtube.sectionName, isOn: $youtube)
                        .onChange(of: youtube) { store.setEnabled(.youtube, $0) }
                }
                Section("To get past the wall") {
                    Picker("Friction", selection: $mode) {
                        Text("Wait one minute").tag(FrictionMode.wait)
                        Text("Solve an AIME-style problem").tag(FrictionMode.aime)
                    }
                    .onChange(of: mode) { store.mode = $0 }
                    Picker("Stay open for", selection: $unlockMinutes) {
                        ForEach([1, 5, 15], id: \.self) { Text("\($0) min").tag($0) }
                    }
                    .onChange(of: unlockMinutes) { store.unlockMinutes = $0 }
                }
                Section("In Safari") {
                    Text("For a real block and hidden counts, use instagram.com and youtube.com in Safari. Turn on Speedbump under Settings, Apps, Safari, Extensions.")
                }
                Section("What iPhone can't do") {
                    Text("Speedbump can't hide counts inside the Instagram app, and it can't detect WeChat Channels. In WeChat, try Me, Settings, General, Manage Discover to turn Channels off.")
                }
            }
            .navigationTitle("Speedbump")
            .task { await refreshNotifications() }
        }
    }

    private func requestNotifications() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { _, _ in
            Task { await refreshNotifications() }
        }
    }

    @MainActor
    private func refreshNotifications() async {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        notificationsAllowed = settings.authorizationStatus == .authorized
    }
}

/// Apple only lets a broadcast start from its own system button.
struct BroadcastButton: UIViewRepresentable {
    func makeUIView(context: Context) -> RPSystemBroadcastPickerView {
        let picker = RPSystemBroadcastPickerView()
        picker.preferredExtension = Bundle.main.object(forInfoDictionaryKey: "SpeedbumpBroadcastExtension") as? String
        picker.showsMicrophoneButton = false
        return picker
    }

    func updateUIView(_ uiView: RPSystemBroadcastPickerView, context: Context) {}
}
