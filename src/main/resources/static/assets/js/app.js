document.getElementById("loginForm").addEventListener("submit", async function (e) {
  e.preventDefault();

  const email = document.getElementById("username").value.trim();
  const password = document.getElementById("password").value.trim();

  if (!email || !password) {
    alert("Please fill in all fields.");
    return;
  }

  const loginBtn = this.querySelector("button[type='submit']");
  loginBtn.disabled = true;

  try {
    // ✅ Optional: Fetch app version
    let version = "1.0";
    try {
      const versionRes = await fetch("../version.json", { cache: "no-store" });
      if (versionRes.ok) {
        const versionData = await versionRes.json();
        version = versionData.version;
        console.log("App Version:", version);
      } else {
        console.warn("⚠️ version.json fetch failed with status", versionRes.status);
      }
    } catch (err) {
      console.warn("⚠️ Failed to fetch version.json:", err);
    }

    // ✅ Send login request
    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ email, password })
    });

    const result = await response.json();

    if (response.ok) {
      // ✅ Save login details in localStorage
      localStorage.setItem("user", JSON.stringify(result));
      localStorage.setItem("userId", result.userId || "");
      localStorage.setItem("email", result.email || "");
      localStorage.setItem("role", result.role || "");
      localStorage.setItem("companyId", result.companyId || "");
      localStorage.setItem("appVersion", version);

      // ✅ Redirect by role
      const role = (result.role || "").toUpperCase();

      if (role === "ADMIN") {
        localStorage.setItem("autoLoadDashboard", "true");
        window.location.href = "./pages/admin.html";
      } else if (role === "USER") {
        localStorage.setItem("autoLoadDashboard", "true");
        window.location.href = "./pages/users.html";
      } else if (role === "DEVELOPER") {
        localStorage.setItem("autoLoadDashboard", "true");
        window.location.href = "./pages/developer.html";
      } else if (role === "DIRECTOR") {
        localStorage.setItem("autoLoadDashboard", "true");
        window.location.href = "./pages/director.html";  // ✅ NEW redirect page
      } else {
        alert("Login successful, but no valid role found.");
      }
    } else {
      alert(result.message || "Login failed. Please try again.");
    }

  } catch (err) {
    console.error("Login error:", err);
    alert("Something went wrong. Please try again later.");
  } finally {
    loginBtn.disabled = false;
  }
});
