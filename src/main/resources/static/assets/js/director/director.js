document.addEventListener('DOMContentLoaded', function () {
  const localUser = JSON.parse(localStorage.getItem("user"));

  if (!localUser || !localUser.userId) {
    customAlert("Please log in to continue.");
    window.location.href = "/indext.html";
    return;
  }

  //console.log("Fetching user data for user ID:", localUser.userId);

  // Fetch latest user from backend
  fetch(`/api/users/${localUser.userId}`,{
    credentials: 'include',
  })
    .then(response => {
      if (!response.ok) throw new Error("Failed to fetch user data.");
      return response.json();
    })
    .then(user => {
      //console.log("Fetched user data:", user);

      // Update UI
      const userName = document.getElementById("userName");
      const userRole = document.getElementById("userRole");
      const avatarPreview = document.getElementById("avatarPreview");

      userName.textContent = user.name || "Default Name";
      userRole.textContent = user.role || "Default Role";

      // Fetch the avatar
      fetchAvatar(user.userId);

      // Save updated user to localStorage
      localStorage.setItem("user", JSON.stringify(user));
    })
    .catch(error => {
      console.error("Error fetching user:", error);
      customAlert("Failed to load user data. Please try again.");
    });

  // ✅ Menu item highlight
  const menuItems = document.querySelectorAll('.nav-links button');
  menuItems.forEach(item => {
    item.addEventListener('click', function () {
      menuItems.forEach(i => i.classList.remove('active'));
      this.classList.add('active');
    });
  });

  // ✅ Search functionality
  const searchInput = document.querySelector('.search-bar input');
  searchInput.addEventListener('keyup', function (e) {
    if (e.key === 'Enter') {
      //console.log('Searching for:', this.value);
    }
  });
});

// ✅ Fetch Avatar by User ID
function fetchAvatar(userId) {
  fetch(`/api/users/${userId}/avatar`,{
    credentials: 'include',
  })
    .then(response => {
      if (!response.ok) {
        throw new Error("Failed to fetch avatar.");
      }
      return response.blob(); // Convert the response to a blob (binary data)
    })
    .then(imageBlob => {
      // Create an object URL for the image blob and set it as the source of the avatar preview
      const imageUrl = URL.createObjectURL(imageBlob);
      document.getElementById("avatarPreview").src = imageUrl;
    })
    .catch(error => {
      console.error("Error fetching avatar:", error);
      document.getElementById("avatarPreview").src = "assets/default-avatar.png"; // Fallback image
    });
}


let previousNotesHash = "";

function showSection(section, id = null) {
  const content = document.getElementById("dashboard-stats");

  if (!content) {
    console.error("Element with ID 'dashboard-stats' not found!");
    return;
  }

  switch (section) {
    case "ViewDashboard":
      const dashboardContainer = document.getElementById("dashboard-stats");
      dashboardContainer.innerHTML = `
        <div id="glass-loader" class="glass-loader-dashbord">
          <div class="spinner"></div>
          <div class="loading-text">Loading Dashbord...</div>
        </div>
      `;
      getDashboard().then(data => {
        dashboardContainer.innerHTML = data;
      });
      break;

    case "ViewUsers":
      content.innerHTML = getUserTableTemplate();
      loadUsers();
      break;

       case "ViewAdmins":
      content.innerHTML = getAdminTableTemplate();
      loadAdmins(); 
      break;


    case "ViewLead":
      content.innerHTML = getLeadTableTemplate();
      const companyId = parseInt(localStorage.getItem("companyId"), 10);
      loadLeads(companyId);
      leadloadCreatedByOptions(); 
      break;
    case "ViewNotification":
        content.innerHTML = `
          <h2>Notifications</h2>
          <div id="glass-loader" class="glass-loader" style="display: none;">
            <div class="spinner"></div>
            <div class="loading-text">Loading Notifications...</div>
          </div>
          <ul id="notificationList" style="display: none;"></ul>
        `;
        fetchNotifications();
        break;
    case "ViewProperty":
      content.innerHTML = getPropertyTableTemplate();
      loadProperty();
      loadCreatedByOptions(); 
      break;

    case "ViewNotes":
      content.innerHTML = getNoteTableTemplate();
      previousNotesHash = "";
      loadNotes();
      break;
    case "ViewTask":
      content.innerHTML = getTaskTableTemplate();
      loadTasks();
      break;

    case "ViewAccount":
      content.innerHTML = getAccountView();
      loadAccountInfo();
      setupAvatarUploadListener();
      break;

    case "User":
      content.innerHTML = getAddUserForm();
      break;

    case "Lead":
      content.innerHTML = getAddLeadForm();
      setupRequirementCheckboxes();
      break;

    case "leads":
      content.innerHTML = getLeadTableTemplate();
      loadLeads(id);
      break;

    case "Properties":
      content.innerHTML = getAddPropertyForm();
      break;

    case "Notes":
      content.innerHTML = getAddNoteForm();
      setupFormEvents();
      setMinDateTime();
      break;
    case "Task":
      content.innerHTML = getAddTaskForm();
      loadTasks();
      break;

    case "EditLead":
      if (id) editLead(id);
      else content.innerHTML = "<p>Error: No lead ID provided.</p>";
      break;

    case "logout":
      content.innerHTML = getLogOut();
      logout();
      break;

    default:
      content.innerHTML = "<p>Feature under construction...</p>";
  }

  // ✅ Handle .active class on sidebar buttons
  const allButtons = document.querySelectorAll(".nav-links button");
  allButtons.forEach(btn => btn.classList.remove("active"));

  if (id && id.classList) {
    id.classList.add("active");
  }
}

function getLogOut() {
  return `<div class="logout-message"><h2>Logging Out...</h2><p>You will be redirected shortly.</p></div>`;
}

function logout() {
  fetch("/api/auth/logout", {
    method: "POST",
    credentials: "include"
  })
    .then(response => {
      if (!response.ok) {
        throw new Error("Logout failed");
      }
      return response.json();
    })
    .then(data => {
      console.log(data.message || "Logged out successfully");
    })
    .catch(error => {
      console.error("Logout error:", error.message);
    })
    .finally(() => {
      localStorage.clear();
      setTimeout(() => {
        window.location.href = "../index.html";
      }, 1000); // Shorter delay for better UX
    });
}


// ------------------------ Account Section ------------------------
// HTML Template for Account View
function getAccountView() {
  return `
    <div class="account-card">
      <h2>Account</h2>
      <div class="profile-pic-container">
        <img id="UavatarPreview" src="assets/default-avatar.png" alt="User Avatar" class="profile-pic" />
        <button type="button" class="edit-icon" onclick="document.getElementById('avatarInput').click();">
          <i class="fa fa-pencil"></i>
        </button>
        <input type="file" id="avatarInput" style="display: none;" accept="image/*" />
      </div>
      <form id="accountForm" onsubmit="saveAccount(event)">
        <label>Name</label>
        <input type="text" id="UserName" value="Loading..." readonly />

        <label>Phone</label>
        <input type="text" id="userPhone" value="Loading..." readonly />

        <label>Email</label>
        <input type="email" id="userEmail" value="Loading..." readonly />

        <div id="roleContainer">
          <label>Role</label>
          <input type="text" id="UserRole" value="Loading..." readonly />
        </div>

        <div class="btn-group">
          <button type="button" class="edit-btn" onclick="enableEdit()">Edit Profile</button>
          <button type="submit" class="save-btn" style="display:none;">Save</button>
        </div>
      </form>
    </div>
  `;
}

// Load Account Page
async function loadAccountPage() {
  document.getElementById("content").innerHTML = getAccountView();
  setupAvatarUploadListener();
  await loadAccountInfo();
}

// Load user data
async function loadAccountInfo() {
  const localUser = JSON.parse(localStorage.getItem("user") || "{}");
  const userId = localUser.userId;
  if (!userId) {
    customAlert("User not found in local storage.");
    return;
  }

  try {
  const res = await fetch(`/api/users/${userId}`, {
  method: "GET",
  credentials: "include", // 👈 send cookies with the request
});

    if (!res.ok) throw new Error("Failed to fetch user data.");

    const user = await res.json();

    document.getElementById("UserName").value = user.name;
    document.getElementById("userEmail").value = user.email;
    document.getElementById("userPhone").value = user.phone;
    document.getElementById("UserRole").value = user.role;

    UfetchAvatar(userId);
    localStorage.setItem("user", JSON.stringify(user));
  } catch (error) {
    console.error("Failed to fetch user:", error);
    customAlert("Error loading user info.");
  }
}

// Enable form editing
function enableEdit() {
  setReadOnlyMode(false);
  document.querySelector(".edit-btn").style.display = "none";
  document.querySelector(".save-btn").style.display = "inline-block";

  // Hide Role section when editing
  const roleContainer = document.getElementById("roleContainer");
  if (roleContainer) {
    roleContainer.style.display = "none";
  }
}

// Toggle input fields' read-only mode
function setReadOnlyMode(isReadOnly) {
  ["UserName", "userEmail", "userPhone"].forEach(id => {
    const input = document.getElementById(id);
    if (input) input.readOnly = isReadOnly;
  });

  // If switching back to readonly mode, show the role again
  const roleContainer = document.getElementById("roleContainer");
  if (roleContainer && isReadOnly) {
    roleContainer.style.display = "block";
  }
}

// Save profile changes
async function saveAccount(event) {
  event.preventDefault();

  const localUser = JSON.parse(localStorage.getItem("user") || "{}");
  const userId = localUser.userId;
  if (!userId) return;

  const name = document.getElementById("UserName")?.value;
  const email = document.getElementById("userEmail")?.value;
  const phone = document.getElementById("userPhone")?.value;

  if (!name || !email || !phone) {
    customAlert("Please fill in all fields.");
    return;
  }

  const data = {
    name,
    email,
    phone,
    role: localUser.role,
  };

try {
  const res = await fetch(`/api/users/update-profile/${userId}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include", // 🔒 ensures cookies like JWT are sent (for Spring Security)
    body: JSON.stringify(data),
  });

    if (res.ok) {
      customAlert("Account updated successfully!");
      setReadOnlyMode(true);
      document.querySelector(".edit-btn").style.display = "inline-block";
      document.querySelector(".save-btn").style.display = "none";
      loadAccountInfo();
    } else {
      const errorData = await res.json();
      customAlert(errorData.message || "Failed to update account.");
    }
  } catch (error) {
    console.error("Save error:", error);
    customAlert("An error occurred while saving.");
  }
}

// Fetch user avatar
function UfetchAvatar(userId) {
fetch(`/api/users/${userId}/avatar`, {
  method: "GET",
  credentials: "include" // 🔒 Includes cookies (e.g. for JWT auth)
})    .then(response => {
      if (!response.ok) throw new Error("Failed to fetch avatar.");
      return response.blob();
    })
    .then(imageBlob => {
      const imageUrl = URL.createObjectURL(imageBlob);
      document.getElementById("UavatarPreview").src = imageUrl;
    })
    .catch(error => {
      console.error("Avatar fetch error:", error);
      document.getElementById("UavatarPreview").src = "assets/default-avatar.png";
    });
}

// Avatar upload handler
function setupAvatarUploadListener() {
  const avatarInput = document.getElementById("avatarInput");
  const avatarPreview = document.getElementById("UavatarPreview");

  avatarInput.addEventListener("change", async function () {
    const file = this.files[0];
    const user = JSON.parse(localStorage.getItem("user"));

    if (!user || !user.userId) {
      customAlert("Please log in to upload your avatar.");
      return;
    }

    if (file) {
      const reader = new FileReader();
      reader.onload = function (e) {
        avatarPreview.src = e.target.result;
      };
      reader.readAsDataURL(file);

      const formData = new FormData();
      formData.append("avatar", file);
      formData.append("avatarName", file.name);

      try {
          avatarPreview.src = "assets/uploading.gif";
        const uploadRes = await fetch(`/api/users/${user.userId}/upload-avatar`, {
          method: "POST",
          credentials: "include", // 🔐 Send cookies like JWT
          body: formData
        });

        if (!uploadRes.ok) {
          const errorText = await uploadRes.text();
          throw new Error(errorText || "Avatar upload failed");
        }

        const message = await uploadRes.text();
        customAlert(message || "Avatar uploaded successfully!");

        // 🔄 Fetch updated user info (includes updated avatar if needed)
        const userRes = await fetch(`/api/users/${user.userId}`, {
          method: "GET",
          credentials: "include"
        });

        if (!userRes.ok) {
          throw new Error("Failed to fetch updated user info.");
        }

        const updatedUser = await userRes.json();

        // 🔃 Refresh avatar & update localStorage
        UfetchAvatar(updatedUser.userId);
        localStorage.setItem("user", JSON.stringify(updatedUser));

      } catch (error) {
        console.error("Avatar upload error:", error);
        customAlert("Error uploading avatar: " + error.message);
        avatarPreview.src = "assets/default-avatar.png"; // fallback if error
      }

    } else {
      avatarPreview.src = "assets/default-avatar.png";
    }
  });
}

// ------------------ Helper: Custom customAlert ------------------


// ------------------ Initial Load ------------------
document.addEventListener("DOMContentLoaded", () => {
  loadAccountPage(); // Load Account section on startup
});


function customAlert(message) {
      const customAlertBox = document.getElementById("custom-customAlert");
      const customAlertMessage = document.getElementById("custom-customAlert-message");
      customAlertMessage.textContent = message;
      customAlertBox.classList.remove("hidden");
    }
