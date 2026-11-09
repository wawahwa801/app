import CoreGraphics
import Foundation
import Vision

/// Recognises on-screen text. Broadcast extensions are killed above about 50 MB, so this feeds
/// Vision a small grayscale image and keeps nothing between frames.
enum FrameReader {
    static let targetHeight = 1000

    static func grayscaleImage(bytes: UnsafeRawPointer, width: Int, height: Int, bytesPerRow: Int) -> CGImage? {
        let step = max(1, height / targetHeight)
        let outWidth = width / step
        let outHeight = height / step
        guard outWidth > 0, outHeight > 0 else { return nil }
        var pixels = [UInt8](repeating: 0, count: outWidth * outHeight)
        let source = bytes.assumingMemoryBound(to: UInt8.self)
        for row in 0..<outHeight {
            let line = source + row * step * bytesPerRow
            for column in 0..<outWidth {
                pixels[row * outWidth + column] = line[column * step]
            }
        }
        guard let provider = CGDataProvider(data: Data(pixels) as CFData) else { return nil }
        return CGImage(
            width: outWidth,
            height: outHeight,
            bitsPerComponent: 8,
            bitsPerPixel: 8,
            bytesPerRow: outWidth,
            space: CGColorSpaceCreateDeviceGray(),
            bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.none.rawValue),
            provider: provider,
            decode: nil,
            shouldInterpolate: false,
            intent: .defaultIntent
        )
    }

    static func recognizeText(in image: CGImage) throws -> [TextBox] {
        let request = VNRecognizeTextRequest()
        request.recognitionLevel = .fast
        request.usesLanguageCorrection = false
        request.recognitionLanguages = ["en-US"]
        try VNImageRequestHandler(cgImage: image, options: [:]).perform([request])
        return (request.results ?? []).compactMap { observation in
            guard let text = observation.topCandidates(1).first?.string else { return nil }
            // Vision's origin is at the bottom left.
            return TextBox(text: text, x: observation.boundingBox.minX, y: 1 - observation.boundingBox.maxY)
        }
    }
}
