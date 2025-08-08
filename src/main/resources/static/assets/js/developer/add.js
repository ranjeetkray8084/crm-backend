// Custom customAlert Functions
function customcustomAlert(message) {
  const customAlertBox = document.getElementById("custom-customAlert");
  const customAlertMessage = document.getElementById("custom-customAlert-message");
  customAlertMessage.innerText = message;
  customAlertBox.classList.remove("hidden");
}

function closeCustomcustomAlert() {
  document.getElementById("custom-customAlert").classList.add("hidden");
}

// Dashboard Section
function getDashboard() {
  return `
    <h2>Dashboard Overview</h2>
    <p>Welcome to the Developer dashboard.</p>
    
    <div id="all-users" style="border:1px solid #ccc; padding:10px; margin-bottom:20px;">
      <h3>All Users</h3>
      <p id="user-count">Loading users count...</p>
    </div>
    
    <div id="all-admin" style="border:1px solid #ccc; padding:10px;">
      <h3>All Admin</h3>
      <p id="admin-count">Loading admins count...</p>
    </div>
  `;
}

function fetchAndRenderUsers() {
  fetch('/api/users', {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
  })
    .then(response => response.json())
    .then(data => {
      const users = data.filter(user => user.role === 'USER');
      const admins = data.filter(user => user.role === 'ADMIN');

      document.getElementById('user-count').textContent = users.length;
      document.getElementById('admin-count').textContent = admins.length;
    })
    .catch(error => {
      console.error('Error fetching users:', error);
      document.getElementById('user-count').textContent = 'Failed to load users count';
      document.getElementById('admin-count').textContent = 'Failed to load admins count';
    });
}

// Add User Form
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
        <select name="role" required>
          <option value="">Select Role</option>
          <option value="DIRECTOR">DIRECTOR</option>
        </select>

        <label>Company:</label>
        <select name="companyId" id="companySelect" required>
          <option value="">Select Company</option>
        </select>

        <button type="submit">Create</button>
      </form>
    </div>
  `;
}

function loadCompanies() {
  fetch("/api/companies/all", {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
  })
    .then(response => response.json())
    .then(companies => {
      const select = document.getElementById("companySelect");
      select.innerHTML = '<option value="">Select Company</option>';
      companies.forEach(company => {
        const option = `<option value="${company.id}">${company.name}</option>`;
        select.insertAdjacentHTML("beforeend", option);
      });
    })
    .catch(error => {
      console.error("Error loading companies:", error);
      customcustomAlert("Failed to load companies");
    });
}

// Users Section
function getUsers() {
  return `
    <h2>Users</h2>
    <div style="display: flex; justify-content: space-between;">
      <input type="text" id="userSearch" onkeyup="filterTable('userTable', 'userSearch')" placeholder="Search users...">
      <div>
        <label for="companyFilter">Filter by Company:</label>
        <select id="companyFilter" onchange="loadUsers()">
          <option value="">All Companies</option>
        </select>
      </div>
    </div>
    <table id="userTable" border="1" style="margin-top:10px;">
      <thead>
        <tr>
          <th>User ID</th>
          <th>Name</th>
          <th>Email</th>
          <th>Company</th>
        </tr>
      </thead>
      <tbody></tbody>
    </table>
  `;
}

function loadUsers() {
  const selectedCompanyId = document.getElementById("companyFilter")?.value;

  fetch("/api/users/user-role", {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
  })
    .then(response => {
      if (!response.ok) {
        throw new Error("Failed to fetch users: " + response.status);
      }
      return response.json();
    })
    .then(users => {
      // Filter by selected company only (role filtering is unnecessary)
      let userList = users;
      if (selectedCompanyId) {
        userList = users.filter(user => user.company && user.company.id == selectedCompanyId);
      }

      const tbody = document.querySelector("#userTable tbody");
      if (!tbody) {
        console.error("Table body not found with selector '#userTable tbody'");
        return;
      }

      tbody.innerHTML = "";

      userList.forEach(user => {
        const companyName = user.company?.name || "No Company";
        const row = `
          <tr>
            <td>${user.userId}</td>
            <td>${user.name}</td>
            <td>${user.email}</td>
            <td>${companyName}</td>
          </tr>
        `;
        tbody.innerHTML += row;
      });
    })
    .catch(err => {
      console.error("Error loading users:", err);
      customcustomAlert("Failed to load users. Please try again.");
    });
}


function loadCompanie() {
  fetch("/api/companies", {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
  })
    .then(res => res.json())
    .then(companies => {
      const companyFilter = document.getElementById("companyFilter");
      if (!companyFilter) return;
      companies.forEach(company => {
        const option = document.createElement("option");
        option.value = company.id;
        option.textContent = company.name;
        companyFilter.appendChild(option);
      });
    })
    .catch(err => console.error("Error loading companies:", err));
}

// Admin Section
function getAdmins() {
  return `
    <h2>Admins</h2>
    <input type="text" id="adminSearch" onkeyup="filterTable('adminTable', 'adminSearch')" placeholder="Search admins...">
    <table id="adminTable" border="1">
      <thead>
        <tr>
          <th>Admin ID</th>
          <th>Name</th>
          <th>Email</th>
          <th>Company</th>
        </tr>
      </thead>
      <tbody></tbody>
    </table>
  `;
}

function loadAdmin() {
  fetch("/api/users/admin-role", {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
  }) // assuming this returns only ADMIN users now
    .then(response => {
      if (!response.ok) {
        throw new Error("Failed to fetch admins: " + response.status);
      }
      return response.json();
    })
    .then(admins => {
      const tbody = document.querySelector("#adminTable tbody");
      if (!tbody) {
        console.error("Table body not found with selector '#adminTable tbody'");
        return;
      }

      tbody.innerHTML = "";

      admins.forEach(admin => {
        const companyName = admin.company?.name || "No Company";
        const row = `
          <tr>
            <td>${admin.userId}</td>
            <td>${admin.name}</td>
            <td>${admin.email}</td>
            <td>${companyName}</td>
          </tr>
        `;
        tbody.innerHTML += row;
      });
    })
    .catch(err => {
      console.error("Error loading admins:", err);
      customcustomAlert("Failed to load admins. Please try again.");
    });
}

function getDirectors() {
  return `
    <h2>Directors</h2>
    <input type="text" id="directorSearch" onkeyup="filterTable('directorTable', 'directorSearch')" placeholder="Search directors...">
    <table id="directorTable" border="1">
      <thead>
        <tr>
          <th>Director ID</th>
          <th>Name</th>
          <th>Email</th>
          <th>Company</th>
        </tr>
      </thead>
      <tbody></tbody>
    </table>
  `;
}


//write  all function code a function to load directors

function loadDirectors() {
  fetch("/api/users/director-role", {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
  })
    .then(response => {
      if (!response.ok) {
        throw new Error("Failed to fetch directors: " + response.status);
      }
      return response.json();
    })
    .then(directors => {
      const tbody = document.querySelector("#directorTable tbody");
      if (!tbody) {
        console.error("Table body not found with selector '#directorTable tbody'");
        return;
      }

      tbody.innerHTML = "";

      directors.forEach(director => {
        const companyName = director.company?.name || "No Company";
        const row = `
          <tr>
            <td>${director.userId}</td>
            <td>${director.name}</td>
            <td>${director.email}</td>
            <td>${companyName}</td>
          </tr>
        `;
        tbody.innerHTML += row;
      });
    })
    .catch(err => {
      console.error("Error loading directors:", err);
      customAlert("Failed to load directors. Please try again.");
    });
}



// Create User Function
function createUser(event) {
  event.preventDefault();
  const form = event.target;
  const formData = new FormData(form);
  const companyId = formData.get('companyId');

  if (!companyId || isNaN(companyId)) {
    customcustomAlert("❌ Please select a valid company before creating a user.");
    return;
  }

  const password = formData.get('password');
  const confirmPassword = formData.get('confirmPassword');

  if (password !== confirmPassword) {
    customcustomAlert("❌ Passwords do not match!");
    return;
  }

  const user = {
    name: formData.get('name'),
    email: formData.get('email'),
    phone: formData.get('phone'),
    password: password,
    role: formData.get('role'),
    company: { id: parseInt(companyId, 10) }
  };

  fetch("/api/users", {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(user),
  })
    .then(response => {
      if (response.ok) {
        customcustomAlert("✅ User created successfully!");
        form.reset();
      } else {
        return response.text().then(text => {
          throw new Error(text || "❌ Failed to create user");
        });
      }
    })
    .catch(error => {
      console.error("Error creating user:", error);
      customcustomAlert(error.message || "❌ Error creating user");
    });
}

// Logout
function getLogOut() {
  return `
    <h2>Logging Out...</h2>
    <p>You will be redirected shortly.</p>
  `;
}

function confirmLogout() {
  const confirmed = confirm("Are you sure you want to logout?");
  if (confirmed) {
    logout();
  }
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



