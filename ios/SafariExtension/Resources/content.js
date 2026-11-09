(function () {
  const R = globalThis.SpeedbumpRules;
  const ext = globalThis.browser || globalThis.chrome;
  const site = R.siteOf(location.hostname);
  if (!site) return;

  const STATE_KEY = "speedbump-session";
  const HIDDEN_ATTR = "data-speedbump-hidden";
  let settings = { ...R.DEFAULTS };
  let unlockedUntil = 0;
  let wall = null;
  let lastPath = null;
  let lastAllowed = true;
  let scanQueued = false;

  function readSession() {
    try {
      return JSON.parse(sessionStorage.getItem(STATE_KEY));
    } catch {
      return null;
    }
  }

  function writeSession(state) {
    try {
      sessionStorage.setItem(STATE_KEY, JSON.stringify(state));
    } catch {
      // Private browsing can refuse storage. A sent reel then shows the wall, which is safe.
    }
  }

  function isEnabled() {
    return site === "instagram" ? settings.blockInstagram : settings.blockYoutube;
  }

  function isBlockedPath() {
    const path = location.pathname;
    if (site === "youtube") return !R.evaluateYoutube(path).allowed;
    // Evaluate once per path, since the sent-reel state changes as it's read.
    if (path !== lastPath) {
      const result = R.evaluateInstagram(path, readSession());
      writeSession(result.state);
      lastPath = path;
      lastAllowed = result.allowed;
    }
    return !lastAllowed;
  }

  function check() {
    const blocked = isBlockedPath();
    if (blocked && isEnabled() && Date.now() >= unlockedUntil) {
      showWall();
    } else if (wall) {
      removeWall();
    }
    if (wall) document.querySelectorAll("video").forEach((video) => video.pause());
  }

  function removeWall() {
    wall?.host.remove();
    clearInterval(wall?.timer);
    wall = null;
  }

  function goBack() {
    const before = location.href;
    removeWall();
    history.back();
    setTimeout(() => {
      if (location.href === before) location.assign("/");
    }, 800);
  }

  function unlock() {
    unlockedUntil = Date.now() + settings.unlockMinutes * 60000;
    ext.storage.local.set({ [`unlockedUntil_${site}`]: unlockedUntil });
    removeWall();
  }

  function element(tag, props = {}, ...children) {
    const node = Object.assign(document.createElement(tag), props);
    node.append(...children);
    return node;
  }

  function showWall() {
    if (wall) return;
    const host = element("div");
    host.style.cssText = "all:initial;position:fixed;inset:0;z-index:2147483647";
    const shadow = host.attachShadow({ mode: "closed" });
    shadow.append(element("style", { textContent: `
      :host { color-scheme: light dark; }
      .wall { position: fixed; inset: 0; overflow: auto; box-sizing: border-box; padding: 48px 20px;
        background: Canvas; color: CanvasText; font: 17px/1.45 -apple-system, system-ui, sans-serif; }
      .inner { max-width: 520px; margin: 0 auto; display: flex; flex-direction: column; gap: 16px; }
      h1 { font-size: 28px; margin: 0; }
      .count { font-size: 56px; font-weight: 300; }
      button, input { font: inherit; border-radius: 12px; padding: 14px; width: 100%; box-sizing: border-box; }
      button { border: 1px solid CanvasText; background: transparent; color: inherit; }
      button:disabled { opacity: .4; }
      input { border: 1px solid CanvasText; background: transparent; color: inherit; }
      .error { color: #c62828; }
    ` }));
    const inner = element("div", { className: "inner" });
    shadow.append(element("div", { className: "wall" }, inner));
    inner.append(element("h1", { textContent: site === "instagram" ? "Instagram Reels" : "YouTube Shorts" }));
    wall = { host, timer: null };
    if (settings.mode === "aime") buildAime(inner);
    else buildWait(inner);
    inner.append(
      element("button", { textContent: "Go back", onclick: goBack }),
      element("small", { textContent: `Opens for ${settings.unlockMinutes} min once you get through.` }),
    );
    document.documentElement.append(host);
  }

  function buildWait(inner) {
    const count = element("div", { className: "count" });
    const proceed = element("button", { textContent: "Wait…", disabled: true, onclick: unlock });
    inner.append(element("p", { textContent: "Still want to go in? Stay on this page for a minute." }), count, proceed);
    let end = Date.now() + R.WAIT_SECONDS * 1000;
    const tick = () => {
      // Leaving the tab restarts the minute, so the time can't pass in the background.
      if (document.hidden) end = Date.now() + R.WAIT_SECONDS * 1000;
      const left = Math.max(0, Math.ceil((end - Date.now()) / 1000));
      count.textContent = `${left}s`;
      if (left === 0) {
        proceed.disabled = false;
        proceed.textContent = "Continue";
        clearInterval(wall?.timer);
      }
    };
    wall.timer = setInterval(tick, 250);
    tick();
  }

  function buildAime(inner) {
    const problems = globalThis.SPEEDBUMP_PROBLEMS;
    let problem = problems[Math.floor(Math.random() * problems.length)];
    const text = element("p", { textContent: problem.text });
    const input = element("input", { inputMode: "numeric", maxLength: 3, placeholder: "Answer" });
    const message = element("p", { className: "error" });
    const submit = element("button", { textContent: "Submit" });
    const other = element("button", { textContent: "Different problem" });
    const lock = () => {
      let left = R.WRONG_ANSWER_LOCK_SECONDS;
      submit.disabled = other.disabled = true;
      const step = () => {
        submit.textContent = left > 0 ? `Try again in ${left}s` : "Submit";
        if (left <= 0) {
          submit.disabled = other.disabled = false;
          clearInterval(timer);
        }
        left -= 1;
      };
      const timer = setInterval(step, 1000);
      step();
    };
    submit.onclick = () => {
      if (R.isCorrectAnswer(input.value, problem.answer)) return unlock();
      input.value = "";
      message.textContent = "That's not it.";
      lock();
    };
    other.onclick = () => {
      const rest = problems.filter((p) => p.id !== problem.id);
      problem = rest[Math.floor(Math.random() * rest.length)];
      text.textContent = problem.text;
      input.value = "";
      message.textContent = "";
      lock();
    };
    inner.append(
      element("p", { textContent: "Solve this to go in. The answer is an integer from 000 to 999." }),
      text, input, message, submit, other,
    );
  }

  function scanMetrics() {
    scanQueued = false;
    document.querySelectorAll(`[${HIDDEN_ATTR}]`).forEach((node) => {
      const key = R.metricSetting(node.textContent);
      if (!key || !settings[key]) {
        node.style.removeProperty("opacity");
        node.removeAttribute(HIDDEN_ATTR);
      }
    });
    // Never touch chats, where a message could read "5 likes".
    if (site !== "instagram" || location.pathname.startsWith("/direct")) return;
    if (!settings.hideLikes && !settings.hideViews && !settings.hideFollowers) return;
    document.querySelectorAll("a, span, div, li, section").forEach((node) => {
      if (node.hasAttribute(HIDDEN_ATTR) || node.childElementCount > 4) return;
      const key = R.metricSetting(node.textContent);
      if (key && settings[key]) {
        // Opacity keeps the element tappable, so a follower-list link still opens.
        node.style.setProperty("opacity", "0", "important");
        node.setAttribute(HIDDEN_ATTR, "");
      }
    });
  }

  function queueScan() {
    if (scanQueued) return;
    scanQueued = true;
    setTimeout(scanMetrics, 200);
  }

  function start() {
    ext.storage.local.get(null, (stored) => {
      settings = { ...R.DEFAULTS, ...stored };
      unlockedUntil = stored[`unlockedUntil_${site}`] || 0;
      check();
      queueScan();
      new MutationObserver(queueScan).observe(document.documentElement, { childList: true, subtree: true, characterData: true });
      // Both sites navigate without page loads, and they do it from page scripts this
      // script can't hook, so the path is checked on a short interval instead.
      setInterval(check, 400);
    });
    ext.storage.onChanged.addListener((changes) => {
      for (const [key, change] of Object.entries(changes)) settings[key] = change.newValue;
      if (changes[`unlockedUntil_${site}`]) unlockedUntil = changes[`unlockedUntil_${site}`].newValue;
      if (changes.mode) removeWall();
      check();
      queueScan();
    });
  }

  start();
})();
