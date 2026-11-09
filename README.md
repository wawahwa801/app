# Speedbump

Speedbump puts friction in front of short-video feeds. You can still get in, but first you wait a minute or solve an AIME-style math problem. It can also hide like, view and follower counts on Instagram.

## What works where

iOS doesn't allow any third-party app to change or detect sections inside another app. Screen Time can only shield a whole app, which would also block Instagram DMs and long YouTube videos. So the iPhone version works in Safari instead.

| | Android | iPhone |
|---|---|---|
| Instagram Reels | In the app | instagram.com in Safari |
| YouTube Shorts | In the app | youtube.com in Safari |
| WeChat Channels | In the app | Not possible |
| Wait or AIME wall | Yes | Yes |
| Hide Instagram counts | In the app | instagram.com in Safari |
| Reels sent in a DM still play | Yes | Yes, in Safari |

## How it behaves

- **Blocked sections:** opening Reels, Shorts or Channels shows a wall. Get through it and that app's section stays open for 1, 5 or 15 minutes.
- **Wait wall:** leaving the wall restarts the minute.
- **AIME wall:** answers are integers from 000 to 999. A wrong answer, or asking for a different problem, locks the buttons for 10 seconds.
- **Instagram DMs:** chats are never blocked, and counts are never hidden in them. A reel opened from a chat plays, and swiping to another reel shows the wall.
- **Hidden counts:** on Android, the mask is a window that ignores touches, so the buttons under it keep working. A mask can lag slightly behind fast scrolling.
- **YouTube:** only the Shorts player and the Shorts tab trigger a wall. Long videos are never blocked, including ones with "Shorts" in the title or a Shorts shelf beneath them.
- **WeChat:** every Channels screen is blocked, including Channels videos sent in a chat. The exception for sent items applies only to Instagram.

## Privacy

The Android app has no internet permission and reads only Instagram, YouTube and WeChat. It saves your settings and a small amount of navigation state, which holds a hash of a reel's author and never the name. The Safari extension runs only on instagram.com and youtube.com and makes no network requests of its own.

## Android setup

Requires JDK 17 and the Android SDK (Android Studio includes both).

```sh
cd android
./gradlew :app:installDebug
```

Then open Speedbump and turn it on in Accessibility settings. If Android says the setting is restricted, open Speedbump's App info, tap ⋮, choose **Allow restricted settings**, and try again.

The in-app rules rely on the view ids and activity names these apps use today, and those change between app versions. If a block stops firing or fires in the wrong place, update `core/Detector.kt`.

Google Play restricts Accessibility Service use, so check its current policy before publishing there.

## iPhone setup

Requires a Mac with Xcode.

```sh
brew install xcodegen
cd ios
xcodegen
open Speedbump.xcodeproj
```

Choose your team under Signing for both targets and run on your iPhone. Then follow the steps shown in the app to enable the extension in Safari.

## Tests

```sh
cd android && ./gradlew :app:testDebugUnitTest   # detection, DM rules, metrics, answers

cd ios/tests
npm i --no-save jsdom@22
node --test .                                    # URL rules and the content script
```

`android/fixture` is a test-only app that borrows Instagram's package name, so the real service can be exercised on an emulator. Install it only on an emulator without the real Instagram, and never publish it.

## Math problems

The problems are original and written in the AIME's style. None is copied from a real contest, because the MAA holds the copyright on those papers. To add one, add the problem and a brute-force solver to `tools/problems_source.py`, then run:

```sh
python3 tools/build_problems.py
```

This computes every answer from its solver and writes the bundle each platform ships.
