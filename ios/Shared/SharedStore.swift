import Foundation

enum FrictionMode: String { case wait, aime }

/// Settings and status shared by the app and the broadcast extension.
struct SharedStore {
    private let defaults: UserDefaults

    init() {
        let group = Bundle.main.object(forInfoDictionaryKey: "SpeedbumpAppGroup") as? String
        defaults = group.flatMap { UserDefaults(suiteName: $0) } ?? .standard
    }

    init(defaults: UserDefaults) {
        self.defaults = defaults
    }

    var mode: FrictionMode {
        get { FrictionMode(rawValue: defaults.string(forKey: "mode") ?? "") ?? .wait }
        nonmutating set { defaults.set(newValue.rawValue, forKey: "mode") }
    }

    var unlockMinutes: Int {
        get { defaults.object(forKey: "unlockMinutes") as? Int ?? 5 }
        nonmutating set { defaults.set(newValue, forKey: "unlockMinutes") }
    }

    func isEnabled(_ site: Site) -> Bool {
        defaults.object(forKey: "enabled_\(site.rawValue)") as? Bool ?? true
    }

    func setEnabled(_ site: Site, _ enabled: Bool) {
        defaults.set(enabled, forKey: "enabled_\(site.rawValue)")
    }

    func isAllowed(_ site: Site, now: Date = Date()) -> Bool {
        !isEnabled(site) || now.timeIntervalSince1970 < defaults.double(forKey: "unlockedUntil_\(site.rawValue)")
    }

    func unlock(_ site: Site, now: Date = Date()) {
        let until = now.timeIntervalSince1970 + Double(unlockMinutes * 60)
        defaults.set(until, forKey: "unlockedUntil_\(site.rawValue)")
    }

    var policyState: PolicyState {
        get {
            defaults.data(forKey: "policy").flatMap { try? JSONDecoder().decode(PolicyState.self, from: $0) } ?? PolicyState()
        }
        nonmutating set {
            defaults.set(try? JSONEncoder().encode(newValue), forKey: "policy")
        }
    }

    /// When the extension last processed a frame, so the app can show whether it's running.
    var lastFrameAt: Date? {
        get { defaults.object(forKey: "lastFrameAt") as? Date }
        nonmutating set { defaults.set(newValue, forKey: "lastFrameAt") }
    }
}
