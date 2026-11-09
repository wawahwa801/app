const test = require("node:test");
const assert = require("node:assert");
require("../SafariExtension/Resources/rules.js");
const R = globalThis.SpeedbumpRules;

function walk(paths) {
  let state = null;
  return paths.map((path) => {
    const result = R.evaluateInstagram(path, state);
    state = result.state;
    return result.allowed;
  });
}

test("reel opened from a chat plays", () => {
  assert.deepStrictEqual(walk(["/direct/t/1/", "/reel/ABC/"]), [true, true]);
});
test("same sent reel stays allowed on re-evaluation", () => {
  assert.deepStrictEqual(walk(["/direct/t/1/", "/reel/ABC/", "/reel/ABC/comments/"]), [true, true, true]);
});
test("scrolling to another reel is blocked", () => {
  assert.deepStrictEqual(walk(["/direct/t/1/", "/reel/ABC/", "/reels/DEF/"]), [true, true, false]);
});
test("returning to chat allows the next sent reel", () => {
  assert.deepStrictEqual(walk(["/direct/t/1/", "/reel/A/", "/reels/B/", "/direct/t/1/", "/reel/C/"]), [true, true, false, true, true]);
});
test("reels feed is blocked, even straight from a chat", () => {
  assert.deepStrictEqual(walk(["/direct/inbox/", "/reels/"]), [true, false]);
  assert.deepStrictEqual(walk(["/reels/"]), [false]);
});
test("reel from outside a chat is blocked", () => {
  assert.deepStrictEqual(walk(["/", "/reel/ABC/"]), [true, false]);
  assert.deepStrictEqual(walk(["/direct/t/1/", "/", "/reel/ABC/"]), [true, true, false]);
});
test("usernames that start with reel aren't blocked", () => {
  assert.deepStrictEqual(walk(["/reelsfan/", "/reel.maker/"]), [true, true]);
});
test("youtube blocks only shorts", () => {
  for (const path of ["/shorts", "/shorts/abc", "/shorts/"]) assert.strictEqual(R.evaluateYoutube(path).allowed, false, path);
  for (const path of ["/watch", "/", "/feed/subscriptions", "/@shortsfan", "/playlist", "/shortsfan"]) assert.strictEqual(R.evaluateYoutube(path).allowed, true, path);
});
test("site matching rejects lookalikes", () => {
  assert.strictEqual(R.siteOf("www.instagram.com"), "instagram");
  assert.strictEqual(R.siteOf("m.youtube.com"), "youtube");
  assert.strictEqual(R.siteOf("notyoutube.com"), null);
});
test("answers accept leading zeros and nothing else", () => {
  assert.ok(R.isCorrectAnswer("042", 42));
  assert.ok(R.isCorrectAnswer("42", 42));
  for (const bad of ["", "43", "0042", "-42", "4x", "4.2"]) assert.ok(!R.isCorrectAnswer(bad, 42), bad);
});
test("metrics are recognised and ordinary text isn't", () => {
  assert.strictEqual(R.metricSetting("1,204 likes"), "hideLikes");
  assert.strictEqual(R.metricSetting("Liked by sam and others"), "hideLikes");
  assert.strictEqual(R.metricSetting("2.3M views"), "hideViews");
  assert.strictEqual(R.metricSetting("10.5K followers"), "hideFollowers");
  assert.strictEqual(R.metricSetting("312 following"), "hideFollowers");
  for (const text of ["Following", "I got 5 likes today", "42", ""]) assert.strictEqual(R.metricSetting(text), null, text);
});
