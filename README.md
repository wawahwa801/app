# Speedbump

Speedbump puts friction in front of short-video feeds. You can still get in, but first you wait a minute or solve an AIME-style math problem. It can also hide like, view and follower counts on Instagram.

## What works where

iOS gives apps no way to change or inspect another app's screens directly. Screen Time can only shield a whole app, which would also block Instagram DMs and long YouTube videos. So the iPhone version has three parts:

- **Safari extension:** a real block on instagram.com and youtube.com.
- **Wall on opening Instagram (iOS 26 or later):** a Shortcuts automation shows a Speedbump screen whenever Instagram opens. **Messages** opens your DMs in Safari, where Reels are walled. **Full Instagram** goes through the wall, then opens the real app for the time you chose. It walls the whole app, not just the Reels tab, and deleting the automation removes it.
- **Screen broadcast (experimental):** reads your screen on the phone to notice Reels and Shorts in the real apps. With a free Apple ID it can only keep alerting you, because closing or shielding an app needs Apple's Family Controls capability, which requires a paid developer account.

| | Android | iPhone, Safari | iPhone, real apps |
|---|---|---|---|
| Instagram Reels | Block | Block | Wall on opening the app, then alerts |
| YouTube Shorts | Block | Block | Alerts only |
| WeChat Channels | Block | Not possible | Not detected |
| Wait or AIME wall | Yes | Yes | Yes |
| Hide Instagram counts | Yes | Yes | Not possible |
| Reels sent in a DM still play | Yes | Yes | Through Messages, or with a weaker swipe check in the app |

YouTube and WeChat have no wall on opening, because it would also interrupt long videos and chats. Use YouTube's Shorts feed limit or youtube.com in Safari, and try WeChat's Manage Discover setting to turn Channels off.

The broadcast's detection rules have never been checked against real screens. They're in `ios/Shared/ScreenClassifier.swift`. It can only notice a swipe to a new reel when the "Follow" button shows the author's name beside it.

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

While it's running, the iPhone screen broadcast can see everything on your screen, including messages and anything you type. Speedbump reads the text from one frame every 1.5 seconds on the phone, then discards it. It saves no frames or text, and it sends nothing anywhere. Stop the broadcast from Control Center or the red status indicator whenever you want.

## Android setup

Requires an Android phone running Android 9 or later. Building the app requires JDK 17 and the Android SDK (Android Studio includes both).

1. Build the app:

   ```sh
   cd android
   ./gradlew :app:assembleDebug
   ```

2. Move `android/app/build/outputs/apk/debug/app-debug.apk` to your phone by USB, Google Drive or email. Don't install the `fixture` app, which is a test app that pretends to be Instagram.
3. Open the file on your phone, and allow installs from that source when asked.
4. Open Speedbump, tap **Open Accessibility settings**, find Speedbump in the list, and turn it on. The menu's location varies by phone brand.
5. If Android says the setting is restricted, go to **Settings > Apps > Speedbump**, tap ⋮, choose **Allow restricted settings**, and try step 4 again.
6. Choose wait or AIME, and turn on any counts you want hidden.

To test it, open Reels and check that you hit the wall. Then open a reel from a DM. It should play, and swiping to the next one should bring up the wall.

The in-app rules rely on the view ids and activity names these apps use today, and those change between app versions. If a block stops firing or fires in the wrong place, update `core/Detector.kt`.

Google Play restricts Accessibility Service use, so check its current policy before publishing there.

## iPhone setup

Requires a Mac with Xcode, an iPhone, a USB cable and an Apple ID. A free Apple ID works, but the app stops opening after 7 days until you press **Run** in Xcode again. Free accounts also limit how many apps and app IDs you can use, and each extension may count toward those limits. The wall in front of Instagram needs iOS 26 or later.

### Install the app

1. Generate and open the project:

   ```sh
   brew install xcodegen
   cd ios
   xcodegen
   open Speedbump.xcodeproj
   ```

2. In Xcode, add your Apple ID under **Settings > Accounts**.
3. Plug in your iPhone, tap **Trust** on the phone, and select it at the top of the Xcode window.
4. Click the blue **Speedbump** project in the left sidebar. Under **Signing & Capabilities**, choose your name as the Team for the `Speedbump`, `SpeedbumpExtension` and `SpeedbumpBroadcast` targets.
5. Press **Run**. If iOS asks for Developer Mode, turn it on under **Settings > Privacy & Security** and restart the phone.
6. If the app won't open, go to **Settings > General > VPN & Device Management** and trust your Apple ID.

If Xcode says the app's ID is taken, change `BUNDLE_PREFIX` in `ios/Signing.xcconfig` to something unique to you, then run `xcodegen` again in the `ios` folder. A free account may also reject App Groups, which has not been tested.

### Add the wall in front of Instagram

Apps can't create automations themselves, so this step is manual.

1. Open **Shortcuts**, tap **Automation**, then **+**.
2. Choose **App**, select Instagram, and leave **Is Opened** on.
3. Choose **Run Immediately**, and turn off **Notify When Run**.
4. Tap **New Blank Automation**, and add **Check Instagram Speedbump**.

The Messages button opens Safari through an undocumented `x-safari-https://` link, which could stop working in a future iOS update.

### Get Reels and Shorts alerts (optional)

1. In Speedbump, tap **Allow notifications**, then **Start watching my screen**.
2. In **Settings > Notifications**, turn on **Screen Sharing** so alerts can appear during the broadcast.

The broadcast may need restarting after you lock your phone.

## Safari on iPhone setup

The extension comes with the iPhone app, so install that first. It's built for iPhone, so it doesn't run in Safari on a Mac.

1. Go to **Settings > Apps > Safari > Extensions** (on older iOS, **Settings > Safari > Extensions**).
2. Turn on **Speedbump**, and allow it on instagram.com and youtube.com.
3. In Safari, go to instagram.com and log in.
4. Tap the extensions icon in the address bar, then **Speedbump**, and choose your wall type and which counts to hide.

To test it, check that instagram.com/reels and any YouTube Short show the wall, and that long YouTube videos don't. Your DMs should open normally, and any counts you hid should be gone.

## Tests

```sh
cd android && ./gradlew :app:testDebugUnitTest   # detection, DM rules, metrics, answers

cd ios/tests
npm i --no-save jsdom@22
node --test .                                    # URL rules and the content script

cd ios && xcodegen
xcodebuild test -project Speedbump.xcodeproj -scheme Speedbump \
  -destination 'platform=iOS Simulator,name=iPhone 18 Pro'   # classifier, sent reels, OCR on drawn screens
```

`android/fixture` is a test-only app that borrows Instagram's package name, so the real service can be exercised on an emulator. Install it only on an emulator without the real Instagram, and never publish it.

## Math problems

The problems are original and written in the AIME's style. None is copied from a real contest, because the MAA holds the copyright on those papers. To add one, add the problem and a brute-force solver to `tools/problems_source.py`, then run:

```sh
python3 tools/build_problems.py
```

This computes every answer from its solver and writes the bundle each platform ships.
