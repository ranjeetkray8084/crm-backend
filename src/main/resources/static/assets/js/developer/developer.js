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


// ✅ Avatar Upload Functionality
const avatarInput = document.getElementById("avatarInput");
const avatarPreview = document.getElementById("avatarPreview");

avatarPreview.addEventListener("click", () => {
  const user = JSON.parse(localStorage.getItem("user"));
  if (!user || !user.userId) {
    custemcustomAlert("Please log in to upload your avatar.");
    return;
  }
  avatarInput.click();
});

avatarInput.addEventListener("change", function () {
  const file = this.files[0];
  const user = JSON.parse(localStorage.getItem("user"));

  if (!user || !user.userId) {
    custemcustomAlert("Please log in to upload your avatar.");
    return;
  }

  if (file) {
    const reader = new FileReader();
    reader.onload = function (e) {
      avatarPreview.src = e.target.result; // Update preview with selected image
    };
    reader.readAsDataURL(file);

    const formData = new FormData();
    formData.append("avatar", file);
    formData.append("avatarName", file.name);

    // Upload the avatar
    fetch(`/api/users/${user.userId}/upload-avatar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: formData
    })
      .then(response => {
        if (!response.ok) throw new Error("Upload failed");
        return response.text();
      })
      .then(message => {
        custemcustomAlert(message || "Avatar uploaded successfully!");

        // 🔁 Fetch updated user with new avatar after upload
        return fetch(`/api/users/${user.userId}`, {
          method: 'GET',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
        });
      })
      .then(response => response.json())
      .then(updatedUser => {
        // Update avatar preview with the new avatar
        fetchAvatar(updatedUser.userId); // Fetch updated avatar

        // Save updated user to localStorage
        localStorage.setItem("user", JSON.stringify(updatedUser));
      })
      .catch(error => {
        custemcustomAlert("Error uploading avatar: " + error.message);
      });
  } else {
    avatarPreview.src = "assets/default-avatar.png"; // Set to default image if no file selected
  }
});



function showSection(section) {
  const content = document.getElementById("dashboard-stats");

  switch (section) {
    case "ViewDashboard":
      content.innerHTML = getDashboard();
      fetchAndRenderUsers(); // Update user/admin counts
      break;

    case "addCompany":
      content.innerHTML = getAddCompany();
      break;

    case "ViewCompany":
      content.innerHTML = getCompany();
      loadCompany();
      break;

    case "AddAdmin":
      content.innerHTML = getAddUserForm();
      loadCompanies(); // Load company options for form
      break;

    case "ViewAdmins":
      content.innerHTML = getAdmins();
      loadCompanies();
      loadAdmin(); 
      break;

    case "ViewDirector":
      content.innerHTML = getDirectors(); 
      loadCompanies(); 
      loadDirectors(); 
      break;  

    case "ViewUsers":
      content.innerHTML = getUsers();
      loadUsers(); // Load users based on role (USER)
      loadCompanies(); // Populate company filter dropdown
      break;

    case "ViewAccount":
      content.innerHTML = getAccountView();
      loadAccountInfo();
      setupAvatarUploadListener();
      break;

    case "logout":
      content.innerHTML = getLogOut();
      logout();
      break;

    default:
      content.innerHTML = `<p>Feature under construction...</p>`;
  }
}


function filterTable(tableId, inputId) {
  const input = document.getElementById(inputId).value.toLowerCase();
  const rows = document.querySelectorAll(`#${tableId} tbody tr`);
  rows.forEach(row => {
    const text = row.textContent.toLowerCase();
    row.style.display = text.includes(input) ? "" : "none";
  });
}



function getLogOut() {
  return `<div class="logout-message"><h2>Logging Out...</h2><p>You will be redirected shortly.</p></div>`;
}

function confirmLogout() {
  const confirmed = confirm("Are you sure you want to logout?");
  if (confirmed) {
    logout();
  }
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
      }, 1000);
    });
}





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
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
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
    const res = await fetch(`/api/users/${userId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
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
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
  })
    .then(response => {
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
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: formData
        });

        if (!uploadRes.ok) throw new Error("Avatar upload failed");

        const message = await uploadRes.text();
        customAlert(message || "Avatar uploaded successfully!");

        const userRes = await fetch(`/api/users/${user.userId}`, {
          method: 'GET',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
        });
        const updatedUser = await userRes.json();

        UfetchAvatar(updatedUser.userId);
        localStorage.setItem("user", JSON.stringify(updatedUser));

      } catch (error) {
        console.error("Avatar upload error:", error);
        customAlert("Error uploading avatar: " + error.message);
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
