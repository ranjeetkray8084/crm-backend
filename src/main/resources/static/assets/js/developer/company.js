function getAddCompany() {
  return `
    <h2>Add New Company</h2>
    <form id="companyForm" onsubmit="createCompany(event)">
      <label>Name: <input type="text" id="companyName" required /></label><br/>
      <label>Email: <input type="email" id="companyEmail" required /></label><br/>
      <label>Phone: <input type="text" id="companyPhone" required /></label><br/>
      <label>Max Users: <input type="number" id="maxUsers" required min="1" /></label><br/>
      <label>Max Admins: <input type="number" id="maxAdmins" required min="1" /></label><br/>
      <button type="submit">Create Company</button>
    </form>
  `;
}

async function createCompany(event) {
  event.preventDefault();

  const developerId = getCurrentUserId(); // 👈 Implement this as per your login/session

  const companyData = {
    name: document.getElementById('companyName').value,
    email: document.getElementById('companyEmail').value,
    phone: document.getElementById('companyPhone').value,
    maxUser: parseInt(document.getElementById('maxUsers').value),
    maxAdmin: parseInt(document.getElementById('maxAdmins').value),
    developer: { userId: developerId } // 👈 backend will map this properly
  };

  try {
    const response = await fetch('/api/companies/add', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(companyData),
    });

    if (response.ok) {
      customAlert('✅ Company created successfully!');
      document.getElementById('companyForm').reset();
      loadCompany();
    } else {
      let errorMsg = 'Unknown error';
      try {
        const error = await response.json();
        errorMsg = error.message || JSON.stringify(error);
      } catch {
        errorMsg = response.statusText;
      }
      customAlert('❌ Error creating company: ' + errorMsg);
    }
  } catch (error) {
    customAlert('❌ Network error: ' + error.message);
  }
}

function getCompany() {
  return `
    <h2>Company List</h2>
    <table id="companyTable" border="1" cellspacing="0" cellpadding="5">
      <thead>
        <tr>
          <th>ID</th>
          <th>Name</th>
          <th>Email</th>
          <th>Phone</th>
          <th>Status</th>
          <th>Max Users</th>
          <th>Max Admins</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody id="companyTableBody">
        <!-- Companies will be loaded here -->
      </tbody>
    </table>
  `;
}

function loadCompany() {
  fetch("/api/companies/all", {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
  })
    .then(response => {
      if (!response.ok) {
        throw new Error('Network response was not OK');
      }
      return response.json();
    })
    .then(companies => {
      const tbody = document.getElementById("companyTableBody");
      tbody.innerHTML = "";

      companies.forEach(company => {
        const statusButton = company.status === "active"
          ? `<button onclick="revokeCompany(${company.id})">Revoke</button>`
          : `<button onclick="unrevokeCompany(${company.id})">Unrevoke</button>`;

        const row = `
          <tr>
            <td>${company.id}</td>
            <td>${company.name}</td>
            <td>${company.email}</td>
            <td>${company.phone}</td>
            <td>${company.status}</td>
            <td>${company.maxUsers ?? '-'}</td>
            <td>${company.maxAdmins ?? '-'}</td>
            <td>
              <button onclick="editCompany(${company.id})">Edit</button>
              ${statusButton}
            </td>
          </tr>
        `;
        tbody.insertAdjacentHTML("beforeend", row);
      });
    })
    .catch(error => {
      console.error("Error loading companies:", error);
      customAlert("❌ Failed to load company list.");
    });
}

function revokeCompany(id) {
  fetch(`/api/companies/revoke/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
  })
    .then(response => response.text())
    .then(message => {
      customAlert(message);
      loadCompany();
    })
    .catch(error => {
      console.error('Error revoking company:', error);
      customAlert("❌ Error revoking company.");
    });
}

function unrevokeCompany(id) {
  fetch(`/api/companies/unrevoke/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
  })
    .then(response => response.text())
    .then(message => {
      customAlert(message);
      loadCompany();
    })
    .catch(error => {
      console.error('Error unrevoking company:', error);
      customAlert("❌ Error unrevoking company.");
    });
}

// 🔒 Dummy utility to get current developer ID (replace with actual implementation)
function getCurrentUserId() {
  // Example: parse from localStorage/session/cookie
  return parseInt(localStorage.getItem("userId")) || 1;
}
