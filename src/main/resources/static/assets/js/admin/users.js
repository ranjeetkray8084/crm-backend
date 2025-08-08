// document.addEventListener("DOMContentLoaded", function () {
//   const viewUsersBtn = document.getElementById("viewUsersBtn");
//   if (viewUsersBtn) {
//     viewUsersBtn.addEventListener("click", () => {
//       document.getElementById("main-content").innerHTML = getUserTableTemplate();
//       loadUsers();
//     });
//   }
// });


function getAddUserForm() {
  return `
   <div class="addform">
    <h2>Add User</h2>
    <form id="userForm" onsubmit="createUser(event)">
      <label>Name:</label>
      <input type="text" name="name" required>
      <label>Email:</label>
      <input type="email" name="email" required>
      <label>Phone:</label>
      <input type="text" name="phone" required>
      <label>Password:</label>
      <input type="password" name="password" required>
      <label>Confirm Password:</label>
      <input type="password" name="confirmPassword" required>
      <button type="submit">Create</button>
    </form>
  </div>
  `;
}



function createUser(event) {
  event.preventDefault();
  const form = event.target;
  const formData = new FormData(form);
  const companyId = parseInt(localStorage.getItem("companyId"), 10);
  const adminId = parseInt(localStorage.getItem("userId"), 10); // 👈 Admin ID from localStorage

  if (!companyId || isNaN(companyId)) {
    customAlert("Company ID is missing or invalid.");
    return;
  }

  if (!adminId || isNaN(adminId)) {
    customAlert("Admin ID is missing or invalid.");
    return;
  }

  const user = {
    name: formData.get('name'),
    email: formData.get('email'),
    phone: formData.get('phone'),
    password: formData.get('password'),
    confirmPassword: formData.get('confirmPassword'),
    role: 'USER', // 👈 Hardcoded USER role
    company: { id: companyId },
    admin: { userId: adminId } // 👈 Set admin ID
  };

  if (user.password !== user.confirmPassword) {
    customAlert("Passwords do not match!");
    return;
  }

  delete user.confirmPassword;

  fetch("/api/users", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(user),
  })
    .then(response => {
      if (response.ok) {
        customAlert("User created successfully!");
        form.reset();
      } else {
        return response.text().then(text => {
          throw new Error(text || "Failed to create user");
        });
      }
    })
    .catch(error => {
      console.error("Error creating user:", error);
      customAlert(error.message || "Error creating user");
    });
}




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
            <th>Actions</th>
          </tr>
        </thead>
        <tbody></tbody>
      </table>
    </div>
  `;
}

function loadUsers() {
  const user = JSON.parse(localStorage.getItem("user"));
  if (!user || !user.companyId) {
    alert("Please login.");
    return;
  }
    const adminId = parseInt(localStorage.getItem("userId"), 10); // 👈 Admin ID from localStorage


  fetch(`/api/users/admin/${adminId}/users`)
    .then(response => response.json())
    .then(users => {
      const tbody = document.querySelector("#userTable tbody");
      tbody.innerHTML = "";

      users.forEach(user => {
        const row = document.createElement("tr");

        const statusText = user.status === "active" || user.status === true ? "Active" : "Inactive";
        const statusColor = statusText === "Active" ? "green" : "red";
        const actionButton = user.status === "active" || user.status === true
          ? `<button onclick="revokeUser(${user.userId})">Deactivate</button>`
          : `<button onclick="activateUser(${user.userId})">Activate</button>`;

        row.innerHTML = `
          <td>${user.name}</td>
          <td>${user.email}</td>
          <td>${user.phone}</td>
          <td>${user.role}</td>
          <td style="color: ${statusColor}; font-weight: bold;">${statusText}</td>
          <td>
            <button onclick='showUpdateUserModal(${JSON.stringify(user)})'>Update</button>
            ${actionButton}
          </td>
        `;

        tbody.appendChild(row);
      });
    })
    .catch(err => {
      console.error("Error loading users:", err);
    });
}

function filterUsers() {
  const input = document.getElementById("searchUsersInput").value.toLowerCase();
  const rows = document.querySelectorAll("#userTable tbody tr");

  rows.forEach(row => {
    const text = row.innerText.toLowerCase();
    row.style.display = text.includes(input) ? "" : "none";
  });
}




function showUpdateUserModal(user) {
  // Prevent multiple modals
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

  // Add event listener for the form submit
  document.getElementById("updateUserForm").addEventListener("submit", function (event) {
    event.preventDefault();
    submitUpdatedUser();
  });

  // Optional: focus the first field
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