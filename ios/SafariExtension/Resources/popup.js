const ext = globalThis.browser || globalThis.chrome;
const CHECKBOXES = ["blockInstagram", "blockYoutube", "hideLikes", "hideViews", "hideFollowers"];

ext.storage.local.get(null, (stored) => {
  const settings = { ...SpeedbumpRules.DEFAULTS, ...stored };
  for (const id of CHECKBOXES) {
    const box = document.getElementById(id);
    box.checked = settings[id];
    box.onchange = () => ext.storage.local.set({ [id]: box.checked });
  }
  document.querySelectorAll('input[name="mode"]').forEach((radio) => {
    radio.checked = radio.value === settings.mode;
    radio.onchange = () => ext.storage.local.set({ mode: radio.value });
  });
  const minutes = document.getElementById("unlockMinutes");
  minutes.value = String(settings.unlockMinutes);
  minutes.onchange = () => ext.storage.local.set({ unlockMinutes: Number(minutes.value) });
});
