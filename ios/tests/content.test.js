// Runs the real content script in a simulated page. Needs jsdom:  npm i --no-save jsdom@22
const test = require("node:test");
const assert = require("node:assert");
const fs = require("node:fs");
const path = require("node:path");
const { JSDOM } = require("jsdom");

const dir = path.join(__dirname, "../SafariExtension/Resources");

function page(url, html, stored = {}) {
  const dom = new JSDOM(`<!doctype html><html><body>${html}</body></html>`, { url, runScripts: "outside-only", pretendToBeVisual: true });
  const shadows = [];
  const paused = [];
  dom.window.HTMLMediaElement.prototype.pause = function () {
    paused.push(this);
  };
  const attach = dom.window.Element.prototype.attachShadow;
  dom.window.Element.prototype.attachShadow = function (init) {
    const shadow = attach.call(this, { ...init, mode: "open" });
    shadows.push(shadow);
    return shadow;
  };
  dom.window.chrome = {
    storage: {
      local: { get: (_, done) => done(stored), set: (values) => Object.assign(stored, values) },
      onChanged: { addListener() {} },
    },
  };
  for (const file of ["problems.js", "rules.js", "content.js"]) dom.window.eval(fs.readFileSync(path.join(dir, file), "utf8"));
  return { dom, paused, wallText: () => shadows.map((s) => s.textContent).join(" ") };
}
const wait = (ms) => new Promise((done) => setTimeout(done, ms));
const hasWall = (p) => p.dom.window.document.documentElement.lastElementChild.tagName === "DIV";

test("reels feed gets the wait wall, and videos pause", async () => {
  const p = page("https://www.instagram.com/reels/", "<video></video>");
  await wait(50);
  assert.ok(hasWall(p));
  assert.match(p.wallText(), /Instagram Reels/);
  assert.match(p.wallText(), /Stay on this page/);
  assert.ok(p.paused.length > 0);
  p.dom.window.close();
});

test("aime mode shows a problem", async () => {
  const p = page("https://www.instagram.com/reels/", "", { mode: "aime" });
  await wait(50);
  assert.match(p.wallText(), /integer from 000 to 999/);
  p.dom.window.close();
});

test("long-form youtube has no wall, and shorts does", async () => {
  const watch = page("https://www.youtube.com/watch?v=abc", "");
  await wait(50);
  assert.ok(!hasWall(watch));
  watch.dom.window.close();
  const shorts = page("https://m.youtube.com/shorts/abc", "");
  await wait(50);
  assert.ok(hasWall(shorts));
  shorts.dom.window.close();
});

test("an unlocked section has no wall", async () => {
  const p = page("https://www.instagram.com/reels/", "", { unlockedUntil_instagram: Date.now() + 60000 });
  await wait(50);
  assert.ok(!hasWall(p));
  p.dom.window.close();
});

test("hides chosen metrics only, and never in chats", async () => {
  const html = '<a id="f"><span>1,024</span> followers</a><span id="l">12 likes</span><p id="c">great post</p>';
  const p = page("https://www.instagram.com/someone/", html, { hideFollowers: true });
  await wait(350);
  const doc = p.dom.window.document;
  assert.strictEqual(doc.getElementById("f").style.opacity, "0");
  assert.strictEqual(doc.getElementById("l").style.opacity, "");
  assert.strictEqual(doc.getElementById("c").style.opacity, "");
  p.dom.window.close();

  const chat = page("https://www.instagram.com/direct/t/1/", '<span id="m">5 likes</span>', { hideLikes: true });
  await wait(350);
  assert.strictEqual(chat.dom.window.document.getElementById("m").style.opacity, "");
  chat.dom.window.close();
});
