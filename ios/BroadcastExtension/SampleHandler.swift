import CoreMedia
import CoreVideo
import ReplayKit
import UserNotifications

/// Reads one frame every 1.5 seconds. Frames are never saved or sent anywhere.
final class SampleHandler: RPBroadcastSampleHandler {
    private static let sampleInterval: TimeInterval = 1.5

    private let store = SharedStore()
    private lazy var policy = SessionPolicy(state: store.policyState)
    private var lastSampleAt: TimeInterval = 0

    override func processSampleBuffer(_ sampleBuffer: CMSampleBuffer, with sampleBufferType: RPSampleBufferType) {
        guard sampleBufferType == .video else { return }
        let now = Date().timeIntervalSince1970
        guard now - lastSampleAt >= Self.sampleInterval else { return }
        lastSampleAt = now
        guard let frame = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        autoreleasepool { process(frame, now: now) }
    }

    private func process(_ frame: CVPixelBuffer, now: TimeInterval) {
        CVPixelBufferLockBaseAddress(frame, .readOnly)
        defer { CVPixelBufferUnlockBaseAddress(frame, .readOnly) }
        // Broadcast frames are YUV, and plane 0 is already a full-resolution grayscale image.
        guard CVPixelBufferIsPlanar(frame),
              let luma = CVPixelBufferGetBaseAddressOfPlane(frame, 0),
              let image = FrameReader.grayscaleImage(
                  bytes: luma,
                  width: CVPixelBufferGetWidthOfPlane(frame, 0),
                  height: CVPixelBufferGetHeightOfPlane(frame, 0),
                  bytesPerRow: CVPixelBufferGetBytesPerRowOfPlane(frame, 0)
              ),
              let boxes = try? FrameReader.recognizeText(in: image)
        else { return }

        store.lastFrameAt = Date()
        let store = self.store
        let site = policy.observe(ScreenClassifier.classify(boxes), now: now) { store.isAllowed($0) }
        store.policyState = policy.state
        if let site { nudge(site) }
    }

    private func nudge(_ site: Site) {
        let content = UNMutableNotificationContent()
        content.title = "You're in \(site.sectionName)"
        content.body = "Tap to go through your Speedbump."
        content.sound = .default
        content.userInfo = ["site": site.rawValue]
        // A fixed id replaces the last nudge, so they don't pile up.
        let request = UNNotificationRequest(identifier: "speedbump-\(site.rawValue)", content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request)
    }
}
