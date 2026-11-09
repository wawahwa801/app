import Combine
import SwiftUI

struct AimeProblem: Decodable, Identifiable {
    let id: Int
    let text: String
    let answer: Int

    static let all: [AimeProblem] = {
        guard let url = Bundle.main.url(forResource: "aime-problems", withExtension: "json"),
              let data = try? Data(contentsOf: url)
        else { return [] }
        return (try? JSONDecoder().decode([AimeProblem].self, from: data)) ?? []
    }()
}

struct WallView: View {
    let site: Site
    var title: String?
    var onBack: (() -> Void)?
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    private let store = SharedStore()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(title ?? site.sectionName).font(.largeTitle)
                switch store.mode {
                case .wait: WaitWall(onDone: unlock)
                case .aime: AimeWall(onDone: unlock)
                }
                Button {
                    onBack?() ?? dismiss()
                } label: {
                    Text("Go back").frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                Text("Opens for \(store.unlockMinutes) min once you get through.")
                    .font(.footnote)
            }
            .padding(24)
        }
        .interactiveDismissDisabled()
    }

    private func unlock() {
        store.unlock(site)
        // The Shortcuts automation fires again here, finds the section unlocked, and does nothing.
        openURL(site.appURL)
        dismiss()
    }
}

/// Shown when Instagram opens with no unlock active.
struct GateView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @State private var showingWall = false
    @State private var safariFailed = false

    // An undocumented Safari link. It keeps instagram.com from reopening the Instagram app.
    private static let messagesURL = URL(string: "x-safari-https://www.instagram.com/direct/inbox/")!

    var body: some View {
        if showingWall {
            WallView(site: .instagram, title: "Instagram", onBack: { showingWall = false })
        } else {
            VStack(alignment: .leading, spacing: 16) {
                Text("Instagram").font(.largeTitle)
                Text("What are you here for?")
                Button {
                    openURL(Self.messagesURL) { opened in
                        if opened { dismiss() } else { safariFailed = true }
                    }
                } label: {
                    Text("Messages").frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                Button {
                    showingWall = true
                } label: {
                    Text("Full Instagram").frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                if safariFailed {
                    Text("Couldn't open Safari. Open instagram.com/direct/inbox there yourself.")
                        .foregroundStyle(.red)
                }
                Text("Messages opens in Safari, where Speedbump walls Reels.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                Button("Not now") { dismiss() }
                    .frame(maxWidth: .infinity)
                Spacer()
            }
            .padding(24)
        }
    }
}

private struct WaitWall: View {
    let onDone: () -> Void
    @Environment(\.scenePhase) private var scenePhase
    @State private var end = Date().addingTimeInterval(Double(Friction.waitSeconds))
    @State private var remaining = Friction.waitSeconds
    private let ticker = Timer.publish(every: 0.25, on: .main, in: .common).autoconnect()

    var body: some View {
        Text("Still want to go in? Stay on this screen for a minute.")
        Text("\(remaining)s").font(.system(size: 64, weight: .light))
        Button(action: onDone) {
            Text(remaining == 0 ? "Continue" : "Wait…").frame(maxWidth: .infinity)
        }
            .buttonStyle(.borderedProminent)
            .disabled(remaining > 0)
            .onReceive(ticker) { _ in
                remaining = max(0, Int(end.timeIntervalSinceNow.rounded(.up)))
            }
            .onChange(of: scenePhase) { phase in
                // Leaving restarts the minute, so the time can't pass in the background.
                if phase != .active, remaining > 0 {
                    end = Date().addingTimeInterval(Double(Friction.waitSeconds))
                }
            }
    }
}

private struct AimeWall: View {
    let onDone: () -> Void
    @State private var problem = AimeProblem.all.randomElement()
    @State private var input = ""
    @State private var message: String?
    @State private var locked = 0
    private let ticker = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        Text("Solve this to go in. The answer is an integer from 000 to 999.")
        Text(problem?.text ?? "No problems found.")
        TextField("Answer", text: $input)
            .keyboardType(.numberPad)
            .textFieldStyle(.roundedBorder)
            .onChange(of: input) { value in
                input = String(value.filter(\.isNumber).prefix(3))
            }
        if let message {
            Text(message).foregroundStyle(.red)
        }
        Button {
            guard let problem else { return }
            if Friction.isCorrect(input, answer: problem.answer) {
                onDone()
            } else {
                input = ""
                message = "That's not it."
                locked = Friction.wrongAnswerLockSeconds
            }
        } label: {
            Text(locked > 0 ? "Try again in \(locked)s" : "Submit").frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .disabled(locked > 0 || input.isEmpty)
        Button("Different problem") {
            problem = AimeProblem.all.filter { $0.id != problem?.id }.randomElement()
            input = ""
            message = nil
            locked = Friction.wrongAnswerLockSeconds
        }
        .disabled(locked > 0)
        .onReceive(ticker) { _ in
            if locked > 0 { locked -= 1 }
        }
    }
}
