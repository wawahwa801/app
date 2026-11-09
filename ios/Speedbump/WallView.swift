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
    @Environment(\.dismiss) private var dismiss
    private let store = SharedStore()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(site.sectionName).font(.largeTitle)
                switch store.mode {
                case .wait: WaitWall(onDone: unlock)
                case .aime: AimeWall(onDone: unlock)
                }
                Button("Go back") { dismiss() }
                    .buttonStyle(.bordered)
                    .frame(maxWidth: .infinity)
                Text("Opens for \(store.unlockMinutes) min once you get through.")
                    .font(.footnote)
            }
            .padding(24)
        }
        .interactiveDismissDisabled()
    }

    private func unlock() {
        store.unlock(site)
        dismiss()
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
        Button(remaining == 0 ? "Continue" : "Wait…", action: onDone)
            .buttonStyle(.borderedProminent)
            .disabled(remaining > 0)
            .frame(maxWidth: .infinity)
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
        Button(locked > 0 ? "Try again in \(locked)s" : "Submit") {
            guard let problem else { return }
            if Friction.isCorrect(input, answer: problem.answer) {
                onDone()
            } else {
                input = ""
                message = "That's not it."
                locked = Friction.wrongAnswerLockSeconds
            }
        }
        .buttonStyle(.borderedProminent)
        .disabled(locked > 0 || input.isEmpty)
        .frame(maxWidth: .infinity)
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
