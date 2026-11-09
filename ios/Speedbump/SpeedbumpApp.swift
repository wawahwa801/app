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
                .fullScreenCover(item: $router.destination) { destination in
                    switch destination {
                    case .gate: GateView()
                    case .wall(let site): WallView(site: site)
                    }
                }
                .onOpenURL { url in
                    // speedbump://gate or speedbump://wall?site=instagram
                    if url.host == "gate" {
                        router.destination = .gate
                    } else {
                        let items = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems
                        router.destination = items?.first { $0.name == "site" }?.value
                            .flatMap(Site.init(rawValue:))
                            .map(Destination.wall)
                    }
                }
        }
    }
}

enum Destination: Identifiable {
    case gate
    case wall(Site)

    var id: String {
        switch self {
        case .gate: return "gate"
        case .wall(let site): return site.rawValue
        }
    }
}

final class WallRouter: ObservableObject {
    static let shared = WallRouter()
    @Published var destination: Destination?
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
            WallRouter.shared.destination = name.flatMap(Site.init(rawValue:)).map(Destination.wall)
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
                    Text("Without a paid Apple developer account, iOS won't let Speedbump close Reels or Shorts once you're inside an app. It can put a wall in front of Instagram, and alert you if you reach Reels or Shorts.")
                }
                Section("Put a wall in front of Instagram") {
                    Text("1. Open Shortcuts, tap Automation, then +")
                    Text("2. Choose App, select Instagram, and leave Is Opened on")
                    Text("3. Choose Run Immediately, and turn off Notify When Run")
                    Text("4. Tap New Blank Automation, and add Check Instagram Speedbump")
                    Text("Needs iOS 26 or later. Turn on Speedbump for Safari too, so Messages can open there with Reels walled.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                Section("Alert me in Reels and Shorts") {
                    if !notificationsAllowed {
                        Button("Allow notifications") { requestNotifications() }
                    }
                    BroadcastButton()
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

/// Apple only lets a broadcast start from its own system picker, which offers no public way to
/// set a label or to be triggered. So the visible button presses the picker's internal one.
struct BroadcastButton: View {
    @State private var picker = BroadcastPicker()
    @State private var failed = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button("Start watching my screen") {
                failed = !picker.start()
            }
            .background(PickerHost(picker: picker).frame(width: 1, height: 1).opacity(0.01))
            if failed {
                Text("Couldn't open the broadcast picker. In Control Center, press and hold Screen Recording, then choose Speedbump.")
                    .font(.footnote)
                    .foregroundStyle(.red)
            }
        }
    }
}

final class BroadcastPicker {
    let view: RPSystemBroadcastPickerView = {
        let picker = RPSystemBroadcastPickerView(frame: CGRect(x: 0, y: 0, width: 44, height: 44))
        picker.preferredExtension = Bundle.main.object(forInfoDictionaryKey: "SpeedbumpBroadcastExtension") as? String
        picker.showsMicrophoneButton = false
        return picker
    }()

    func start() -> Bool {
        guard let button = Self.findButton(in: view) else { return false }
        button.sendActions(for: .touchUpInside)
        return true
    }

    private static func findButton(in view: UIView) -> UIButton? {
        if let button = view as? UIButton { return button }
        for subview in view.subviews {
            if let button = findButton(in: subview) { return button }
        }
        return nil
    }
}

private struct PickerHost: UIViewRepresentable {
    let picker: BroadcastPicker
    func makeUIView(context: Context) -> RPSystemBroadcastPickerView { picker.view }
    func updateUIView(_ uiView: RPSystemBroadcastPickerView, context: Context) {}
}
