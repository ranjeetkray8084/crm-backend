// ✅ assets/js/comman/checkAppVersion.js

async function checkAppVersion() {
  try {
    const res = await fetch("../version.json", { cache: "no-store" }); // ../ because HTML is inside /pages
    const { version } = await res.json();
    const currentVersion = localStorage.getItem("appVersion");

    if (currentVersion && currentVersion !== version) {
      console.log("🆕 New version found:", version);
      localStorage.clear();
      sessionStorage.clear();

      // Clear all cookies
      document.cookie.split(";").forEach(function (c) {
        document.cookie = c
          .replace(/^ +/, "")
          .replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/");
      });

      localStorage.setItem("appVersion", version);
      location.reload(true);
    } else {
      localStorage.setItem("appVersion", version);
      console.log("✅ App version is up-to-date:", version);
    }
  } catch (e) {
    console.error("❌ Version check failed:", e);
  }
}
