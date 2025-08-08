function getAdminTableTemplate() {
  return `
    <h2>View Admins</h2>
    <input type="text" id="searchAdminsInput" placeholder="Search admins..." oninput="filterAdmins()" style="margin-bottom: 10px; padding: 5px; width: 100%; max-width: 300px;">

    <div class="table-responsive">
      <table id="adminTable">
        <thead>
          <tr>
            <th>Name</th>
            <th>Email</th>
            <th>Phone Number</th>
            <th>Role </th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody></tbody>
      </table>
    </div>
  `;
}


function filterAdmins() {
  const searchInput = document.getElementById('searchAdminsInput').value.toLowerCase();
  const table = document.getElementById('adminTable');
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



function loadAdmins() {
  const user = JSON.parse(localStorage.getItem("user"));
  const companyId = user?.companyId;

  if (!companyId || isNaN(companyId)) {
    customAlert("Company ID is missing or invalid.");
    return;
  }

  fetch(`/api/users/admin-role/${companyId}`, {
    method: 'GET',
    credentials: 'include'
  })
    .then(res => {
      if (!res.ok) {
        throw new Error(`HTTP Error ${res.status}`);
      }
      return res.json();
    })
    .then(admins => {
      const filteredAdmins = admins.filter(admin => admin.role === "ADMIN");
      const tbody = document.querySelector("#adminTable tbody");
      tbody.innerHTML = "";

      if (filteredAdmins.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;">No admins found</td></tr>`;
        return;
      }

      filteredAdmins.forEach(admin => {
        const row = document.createElement("tr");

        const isActive = admin.status === "active" || admin.status === true || admin.status === 1;
        const statusText = isActive ? "Active" : "Inactive";
        const statusColor = isActive ? "green" : "red";

        const actionButton = isActive
          ? `<button onclick="revokeAdmin(${admin.userId})" style="background-color: #ff4444; color: white;">Deactivate</button>`
          : `<button onclick="activateAdmin(${admin.userId})" style="background-color: #44cc44; color: white;">Activate</button>`;

        row.innerHTML = `
          <td>${admin.name}</td>
          <td>${admin.email}</td>
          <td>${admin.phone}</td>
          <td>${admin.role}</td>
          <td style="color: ${statusColor}; font-weight: bold;">${statusText}</td>
          <td>
            <button onclick='showUpdateAdminModal(${JSON.stringify(admin)})' style="margin-right: 5px;">Update</button>
            ${actionButton}
          </td>
        `;

        tbody.appendChild(row);
      });
    })
    .catch(err => {
      console.error("Error loading admins:", err);
      customAlert("Failed to load admins.");
    });
}


function showUpdateAdminModal(user) {
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
    submitUpdatedAdmin();
  });

  document.getElementById("updateName").focus();
}

function submitUpdatedAdmin() {
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
      loadAdmins(); // Refresh the user list
    })
    .catch((error) => {
      console.error("Error updating user:", error);
      customAlert("Error updating user");
    });
}
