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
      
      <label>Role:</label>
      <select name="role" id="roleSelect" onchange="handleRoleChange()" required>
        <option value="">Select Role</option>
        <option value="USER">USER</option>
        <option value="ADMIN">ADMIN</option>
      </select>
      
      <div id="adminSelectContainer" style="display: none;">
        <label>Select Admin:</label>
        <select name="adminId" id="adminSelect"></select>
      </div>
      
      <button type="submit">Create</button>
    </form>
  </div>
  `;
}

function handleRoleChange() {
  const role = document.getElementById("roleSelect").value;
  const companyId = parseInt(localStorage.getItem("companyId"), 10);
  const adminContainer = document.getElementById("adminSelectContainer");
  const adminSelect = document.getElementById("adminSelect");

  if (role === "USER") {
    adminContainer.style.display = "block";

    fetch(`/api/users/admin-role/${companyId}`, { credentials: "include" })
      .then(res => res.json())
      .then(admins => {
        adminSelect.innerHTML = "<option value=''>Select Admin</option>";
        admins.forEach(admin => {
          adminSelect.innerHTML += `<option value="${admin.userId}">${admin.name}</option>`;
        });
      })
      .catch(err => {
        console.error("Failed to load admins:", err);
        adminSelect.innerHTML = "<option value=''>Error loading admins</option>";
      });
  } else {
    adminContainer.style.display = "none";
    adminSelect.innerHTML = "";
  }
}

function createUser(event) {
  event.preventDefault();
  const form = event.target;
  const formData = new FormData(form);
  const companyId = parseInt(localStorage.getItem("companyId"), 10);

  if (!companyId || isNaN(companyId)) {
    customAlert("Company ID is missing or invalid.");
    return;
  }

  const role = formData.get("role");
  const adminId = formData.get("adminId");

  const user = {
    name: formData.get("name"),
    email: formData.get("email"),
    phone: formData.get("phone"),
    password: formData.get("password"),
    confirmPassword: formData.get("confirmPassword"),
    role: role,
    company: { id: companyId },
    admin: role === "USER" && adminId ? { userId: parseInt(adminId, 10) } : null
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
        if (typeof loadUsers === "function") loadUsers();
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
