

function getUserTableTemplate() {
  return `
    <h2>View Users</h2>
    <input type="text" id="searchUsersInput" placeholder="Search users..." oninput="filterUsers()" style="margin-bottom: 10px; padding: 5px; width: 100%; max-width: 300px;">

    <div class="table-responsive">
      <table id="userTable">
        <thead>
          <tr>
            <th>Name</th>
            <th>Email</th>
            <th>Phone</th>
            <th>Role</th>
            <th>Status</th>
            <th>Admin</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody id="userTableBody">
          <!-- Rows will be added here by loadUsers() -->
        </tbody>
      </table>
    </div>
  `;
}






function loadUsers() {
  const localUser = JSON.parse(localStorage.getItem("user"));
  if (!localUser || !localUser.companyId) {
    customAlert("Please login first.");
    return;
  }

  fetch(`/api/users/user-role/${localUser.companyId}`, {
    method: 'GET',
    credentials: 'include'
  })
    .then(res => res.json())
    .then(users => {
      const filteredUsers = users.filter(user => user.role === "USER");
      const tbody = document.querySelector("#userTable tbody");

      if (!tbody) {
        console.error("User table body not found in DOM.");
        return;
      }

      tbody.innerHTML = "";

      filteredUsers.forEach(user => {
        const row = document.createElement("tr");

        const isActive = user.status === true || user.status === "active";
        const statusText = isActive ? "Active" : "Inactive";
        const statusColor = isActive ? "green" : "red";

        const actionButton = isActive
          ? `<button onclick="revokeUser(${user.userId})">Deactivate</button>`
          : `<button onclick="activateUser(${user.userId})">Activate</button>`;

        const assignButton = user.admin && user.admin.userId
          ? `<button onclick="unassignAdmin(${user.userId})">Unassign</button>`
          : `<button onclick="assignAdmin(${user.userId})">Assign</button>`;

        const updateButton = `<button onclick='showUpdateUserModal(${JSON.stringify(user)})'>Update</button>`;

        row.innerHTML = `
          <td>${safeDisplay(user.name)}</td>
          <td>${safeDisplay(user.email)}</td>
          <td>${safeDisplay(user.phone)}</td>
          <td>${safeDisplay(user.role)}</td>
          <td style="color: ${statusColor}; font-weight: bold;">${statusText}</td>
          <td>${safeDisplay(user.adminName || "No Admin")}</td>
          <td>
            ${updateButton}
            ${actionButton}
            ${assignButton}
          </td>
        `;

        tbody.appendChild(row);
      });

      // 🔁 Optional reloads
      if (typeof loadAdmin === "function") loadAdmin();
      if (typeof reload === "function") reload();
    })
    .catch(err => {
      console.error("Error loading users:", err);
      customAlert("Failed to load users.");
    });
}


function safeDisplay(value) {
  return value ? value : "";
}




function filterUsers() {
  const searchInput = document.getElementById('searchUsersInput').value.toLowerCase();
  const table = document.getElementById('userTable');
  const rows = table.getElementsByTagName('tr');

  for (let i = 1; i < rows.length; i++) {
    const cells = rows[i].getElementsByTagName('td');
    let rowMatches = false;

    for (let j = 0; j < cells.length; j++) {
      const cellText = cells[j].textContent || cells[j].innerText;
      if (cellText.toLowerCase().includes(searchInput)) {
        rowMatches = true;
        break;
      }
    }

    rows[i].style.display = rowMatches ? '' : 'none';
  }
}

function assignAdmin(userId) {
  const companyId = parseInt(localStorage.getItem("companyId"), 10);
  if (!companyId) {
    customAlert("Company ID not found");
    return;
  }

  fetch(`/api/users/admin-role/${companyId}`, {
    method: 'GET',
    credentials: 'include'
  })
    .then(res => res.json())
    .then(admins => {
      const options = admins.map(admin =>
        `<option value="${admin.userId}">${admin.name} (${admin.email})</option>`).join("");

      const adminSelectHtml = `
        <label>Select Admin:</label>
        <select id="selectedAdminId">
          <option value="">-- Select Admin --</option>
          ${options}
        </select>
      `;

      const container = document.createElement("div");
      container.style = "background:white;padding:20px;border-radius:8px;color:black;max-width:300px;position:relative;";
      container.innerHTML = `
        <button onclick="closeAssignModal()" style="position:absolute;top:10px;right:10px;background:none;border:none;color:black;font-size:18px;cursor:pointer;">&times;</button>
        <h3>Select Admin</h3>
        ${adminSelectHtml}
        <button onclick="confirmAssignAdmin(${userId})" style="margin-top:10px;">Assign</button>
      `;

      const modal = document.createElement("div");
      modal.id = "assignModal";
      modal.style = "position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.6);display:flex;align-items:center;justify-content:center;z-index:9999;";
      modal.appendChild(container);
      document.body.appendChild(modal);
    })
    .catch(err => {
      console.error("Failed to fetch admins:", err);
      customAlert("Failed to load admins.");
    });
}

function closeAssignModal() {
  const modal = document.getElementById("assignModal");
  if (modal) modal.remove();
}

function unassignAdmin(userId) {
  fetch(`/api/users/${userId}/unassign-admin`, {
    method: 'PUT',
    credentials: 'include'
  })
    .then(res => {
      if (res.ok) {
        customAlert("Admin unassigned successfully!");
        loadUsers();
      } else {
        return res.text().then(t => { throw new Error(t); });
      }
    })
    .catch(err => customAlert(err.message));
}

function confirmAssignAdmin(userId) {
  const selectedAdminId = parseInt(document.getElementById("selectedAdminId").value, 10);

  if (!selectedAdminId || isNaN(selectedAdminId)) {
    customAlert("Please select a valid admin.");
    return;
  }

  fetch(`/api/users/${userId}/assign-admin`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ adminId: selectedAdminId })
  })
    .then(res => {
      if (res.ok) {
        customAlert("User assigned successfully!");
        loadUsers();
        closeAssignModal();
      } else {
        return res.text().then(text => {
          throw new Error(text || "Failed to assign admin.");
        });
      }
    })
    .catch(err => {
      console.error(err);
      customAlert(err.message || "Error assigning admin.");
    });
}

function showUpdateUserModal(user) {
  const existingModal = document.querySelector('.modal2');
  if (existingModal) existingModal.remove();

  currentUserId = user.userId;

  const modalHtml = `
    <div class="modal2" role="dialog" aria-modal="true">
      <div class="modal-content1">
        <span class="close" onclick="document.querySelector('.modal2').remove()" role="button" aria-label="Close">&times;</span>
        <h2>Update User</h2>
        <form id="updateUserForm">
          <input type="hidden" id="updateUserId" value="${user.userId}" />
          <input type="text" id="updateName" value="${user.name}" placeholder="Name" required />
          <input type="email" id="updateEmail" value="${user.email}" placeholder="Email" required />
          <input type="text" id="updatePhone" value="${user.phone}" placeholder="Phone" required />
          <input type="password" id="updatePassword" placeholder="New Password (optional)" />
          <label for="updateRole">Role:</label>
          <select id="updateRole" name="role" required>
            <option value="USER" ${user.role === "USER" ? "selected" : ""}>USER</option>
            <option value="ADMIN" ${user.role === "ADMIN" ? "selected" : ""}>ADMIN</option>
          </select>
          <button type="submit">Update</button>
        </form>
      </div>
    </div>
  `;

  const wrapper = document.createElement("div");
  wrapper.innerHTML = modalHtml;
  document.body.appendChild(wrapper);

  document.getElementById("updateUserForm").addEventListener("submit", function (event) {
    event.preventDefault();
    submitUpdatedUser();
  });

  document.getElementById("updateName").focus();
}

function submitUpdatedUser() {
  const userId = document.getElementById("updateUserId").value;
  const name = document.getElementById("updateName").value;
  const email = document.getElementById("updateEmail").value;
  const phone = document.getElementById("updatePhone").value;
  const password = document.getElementById("updatePassword").value;
  const role = document.getElementById("updateRole").value;

  const updatedUser = {
    name,
    email,
    phone,
    role,
  };

  if (password.trim() !== "") {
    updatedUser.password = password;
  }

  fetch(`/api/users/update-profile/${userId}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify(updatedUser),
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error("Failed to update user");
      }
      return response.json();
    })
    .then((data) => {
      customAlert("User updated successfully!");
      document.querySelector(".modal").remove(); // Close modal
      loadUsers(); // Refresh the user list
    })
    .catch((error) => {
      console.error("Error updating user:", error);
      customAlert("Error updating user");
    });
}
function safeDisplay(text) {
  return text?.replace(/</g, "&lt;").replace(/>/g, "&gt;") || "";
}
