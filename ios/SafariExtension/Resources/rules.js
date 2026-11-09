// Pure decision logic, shared by the content script, popup and tests.
(function (root) {
  const DEFAULTS = {
    mode: "wait", // "wait" or "aime"
    unlockMinutes: 5,
    blockInstagram: true,
    blockYoutube: true,
    hideLikes: false,
    hideViews: false,
    hideFollowers: false,
  };
  const WAIT_SECONDS = 60;
  const WRONG_ANSWER_LOCK_SECONDS = 10;
  const COUNT = String.raw`\d[\d.,\s]*[KkMmBb]?`;
  const METRICS = {
    hideLikes: new RegExp(String.raw`^${COUNT}\s*likes?$|^liked by .+|^likes?:?\s*${COUNT}$`, "i"),
    hideViews: new RegExp(String.raw`^${COUNT}\s*(views?|plays?)$`, "i"),
    hideFollowers: new RegExp(String.raw`^${COUNT}\s*(followers?|following)$`, "i"),
  };

  function siteOf(hostname) {
    if (/(^|\.)instagram\.com$/.test(hostname)) return "instagram";
    if (/(^|\.)youtube\.com$/.test(hostname)) return "youtube";
    return null;
  }

  // Returns { allowed, state }. A reel opened from a chat stays allowed until the path
  // moves to any other reel, because scrolling on the web changes the reel's id in the URL.
  function evaluateInstagram(path, state) {
    const prior = state || { fromChat: false, sentId: null };
    if (path.startsWith("/direct/") || path === "/direct") {
      return { allowed: true, state: { fromChat: true, sentId: null } };
    }
    const reel = path.match(/^\/reels?\/([^/]+)/);
    if (reel) {
      if (prior.sentId === reel[1]) return { allowed: true, state: prior };
      if (prior.fromChat && !prior.sentId) {
        return { allowed: true, state: { fromChat: false, sentId: reel[1] } };
      }
      return { allowed: false, state: { fromChat: false, sentId: null } };
    }
    if (/^\/reels\/?$/.test(path)) {
      return { allowed: false, state: { fromChat: false, sentId: null } };
    }
    return { allowed: true, state: { fromChat: false, sentId: null } };
  }

  // Only /shorts/ is ever blocked, so /watch and all long-form pages are untouched.
  function evaluateYoutube(path) {
    return { allowed: !/^\/shorts(\/|$)/.test(path), state: null };
  }

  function isCorrectAnswer(input, answer) {
    const value = String(input).trim();
    return /^\d{1,3}$/.test(value) && Number(value) === answer;
  }

  function metricSetting(text) {
    const value = String(text || "").trim();
    if (!value || value.length > 60) return null;
    return Object.keys(METRICS).find((key) => METRICS[key].test(value)) || null;
  }

  root.SpeedbumpRules = {
    DEFAULTS, WAIT_SECONDS, WRONG_ANSWER_LOCK_SECONDS,
    siteOf, evaluateInstagram, evaluateYoutube, isCorrectAnswer, metricSetting,
  };
})(globalThis);
