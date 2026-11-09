import UIKit
import Vision
import XCTest

/// Runs real Vision OCR over drawn stand-ins for each screen. They only imitate the layouts, so
/// passing shows the pipeline works, not that real Instagram or YouTube screens will match.
final class FrameReaderTests: XCTestCase {
    private func frame(_ labels: [(String, CGFloat, CGFloat)]) -> [UInt8] {
        let width = 1170, height = 2532
        var pixels = [UInt8](repeating: 255, count: width * height)
        let context = CGContext(
            data: &pixels, width: width, height: height, bitsPerComponent: 8, bytesPerRow: width,
            space: CGColorSpaceCreateDeviceGray(), bitmapInfo: CGImageAlphaInfo.none.rawValue
        )!
        context.translateBy(x: 0, y: CGFloat(height))
        context.scaleBy(x: 1, y: -1)
        UIGraphicsPushContext(context)
        for (text, x, y) in labels {
            (text as NSString).draw(
                at: CGPoint(x: x * CGFloat(width), y: y * CGFloat(height)),
                withAttributes: [.font: UIFont.systemFont(ofSize: 54, weight: .semibold), .foregroundColor: UIColor.black]
            )
        }
        UIGraphicsPopContext()
        return pixels
    }

    private func classify(_ pixels: [UInt8]) throws -> Observation {
        let image = pixels.withUnsafeBytes { raw in
            FrameReader.grayscaleImage(bytes: raw.baseAddress!, width: 1170, height: 2532, bytesPerRow: 1170)
        }
        return ScreenClassifier.classify(try FrameReader.recognizeText(in: XCTUnwrap(image)))
    }

    func testDownscaleMatchesTarget() throws {
        let pixels = frame([])
        let image = pixels.withUnsafeBytes {
            FrameReader.grayscaleImage(bytes: $0.baseAddress!, width: 1170, height: 2532, bytesPerRow: 1170)
        }
        XCTAssertLessThanOrEqual(try XCTUnwrap(image).height, 1300)
    }

    func testReadsScreens() throws {
        XCTAssertEqual(try classify(frame([("Reels", 0.05, 0.06), ("Add comment", 0.1, 0.9)])).kind, .shortForm(.instagram))
        XCTAssertEqual(try classify(frame([("Message", 0.2, 0.92), ("hello there", 0.2, 0.4)])).kind, .directMessages)
        XCTAssertEqual(try classify(frame([("Dislike", 0.82, 0.45), ("Share", 0.82, 0.6), ("Remix", 0.82, 0.7)])).kind, .shortForm(.youtube))
        XCTAssertEqual(try classify(frame([("Dislike", 0.1, 0.4), ("Share", 0.4, 0.4), ("Remix", 0.72, 0.4)])).kind, .other)
    }

    /// The real limit is about 50 MB for the whole extension, on real hardware.
    func testRepeatedOcrDoesNotGrowMemory() throws {
        let pixels = frame([("Reels", 0.05, 0.06), ("Add comment", 0.1, 0.9)])
        _ = try classify(pixels)
        let baseline = footprint()
        for _ in 0..<20 {
            try autoreleasepool { _ = try classify(pixels) }
        }
        let growth = Double(footprint() - baseline) / 1_048_576
        print("OCR memory: baseline \(baseline / 1_048_576) MB, growth over 20 frames \(growth) MB")
        XCTAssertLessThan(growth, 15)
    }

    private func footprint() -> Int {
        var info = task_vm_info_data_t()
        var count = mach_msg_type_number_t(MemoryLayout<task_vm_info_data_t>.size / MemoryLayout<natural_t>.size)
        withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: Int(count)) {
                _ = task_info(mach_task_self_, task_flavor_t(TASK_VM_INFO), $0, &count)
            }
        }
        return Int(info.phys_footprint)
    }
}
