document.addEventListener("DOMContentLoaded", () => {
  fetchUnreadCount();
  setInterval(fetchUnreadCount, 5000);

  const notificationBtn = document.getElementById("notificationButton");
  if (notificationBtn) {
    notificationBtn.addEventListener("click", async () => {
      const userId = getUserId();
      const companyId = parseInt(localStorage.getItem("companyId"), 10);

      if (!userId || !companyId) {
        customAlert("User or Company ID not found. Please log in again.");
        return;
      }

      try {
        const markReadRes = await fetch(
          `/api/notifications/mark-all-as-read/user/${userId}/company/${companyId}`,
          {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
          }
        );
        if (!markReadRes.ok) throw new Error("Failed to mark all as read");

        updateNotificationCount(0);
      } catch (error) {
        // fail silently
      }
    });
  }
});

async function fetchNotifications() {
  const userId = getUserId();
  const companyId = parseInt(localStorage.getItem("companyId"), 10);

  if (!userId || !companyId) {
    customAlert("User or Company ID not found. Please log in again.");
    return;
  }

  const loader = document.getElementById("glass-loader");
  const list = document.getElementById("notificationList");

  if (loader) loader.style.display = "flex";
  if (list) list.style.display = "none";

  try {
    const res = await fetch(`/api/notifications/user/${userId}/company/${companyId}`, {
      method: "GET",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
    });
    if (!res.ok) throw new Error("Failed to fetch notifications");

    const notifications = await res.json();
    if (!list) return;

    list.innerHTML = "";

    if (notifications.length === 0) {
      list.innerHTML = "<li>No notifications found.</li>";
      return;
    }

    notifications.reverse().forEach((notification) => {
      const item = document.createElement("li");
      item.classList.add("notification-item");
      item.innerHTML = `
        <div>
          <strong>${!notification.isRead ? "[New] " : ""}</strong>${sanitizeHTML(notification.message)}
          <br><small>${formatDate(notification.createdAt)}</small>
        </div>
      `;
      list.prepend(item);
    });
  } catch (error) {
    if (list) list.innerHTML = "<li>Failed to load notifications.</li>";
  } finally {
    if (loader) loader.style.display = "none";
    if (list) list.style.display = "block";
  }
}

async function fetchUnreadCount() {
  const userId = getUserId();
  const companyId = parseInt(localStorage.getItem("companyId"), 10);

  if (!userId || !companyId) {
    customAlert("User or Company ID not found. Please log in again.");
    return;
  }

  try {
    const res = await fetch(
      `/api/notifications/user/${userId}/company/${companyId}/unread-count`,
      {
        method: "GET",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
      }
    );
    if (!res.ok) throw new Error("Failed to fetch unread count");

    const count = await res.json();
    updateNotificationCount(count);
  } catch (error) {
    // fail silently
  }
}

function updateNotificationCount(count) {
  const badge = document.getElementById("notificationCount");
  if (!badge) return;

  if (count > 0) {
    badge.textContent = count;
    badge.style.display = "inline-block";
  } else {
    badge.textContent = "";
    badge.style.display = "none";
  }
}

function getUserId() {
  try {
    const user = JSON.parse(localStorage.getItem("user") || "{}");
    return user?.userId || null;
  } catch {
    return null;
  }
}

function formatDate(dateStr) {
  if (!dateStr) return "Unknown";
  const date = new Date(dateStr);
  if (isNaN(date.getTime())) return "Invalid date";

  const options = {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  };
  return date.toLocaleString(undefined, options);
}

function sanitizeHTML(str) {
  const temp = document.createElement("div");
  temp.textContent = str;
  return temp.innerHTML;
}
