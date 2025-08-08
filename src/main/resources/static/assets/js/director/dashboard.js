async function getDashboard() {
  const companyId = parseInt(localStorage.getItem("companyId"), 10);
  const localUser = JSON.parse(localStorage.getItem("user") || "{}");
  const userId = localUser.userId;

  const loading = document.getElementById("glass-loader");
  const tbody = document.querySelector("#leadTable tbody");

  if (loading) loading.style.display = "flex";
  if (tbody) tbody.innerHTML = "";

  function getKolkataDateTime() {
    const now = new Date();
    const offset = 330;
    const utc = now.getTime() + now.getTimezoneOffset() * 60000;
    return new Date(utc + offset * 60000);
  }

  function getKolkataDateString() {
    const kolkataTime = getKolkataDateTime();
    const year = kolkataTime.getFullYear();
    const month = (kolkataTime.getMonth() + 1).toString().padStart(2, "0");
    const day = kolkataTime.getDate().toString().padStart(2, "0");
    return `${year}-${month}-${day}`;
  }

  function convertToKolkataTime(dateTimeStr) {
    if (!dateTimeStr) return null;
    const date = new Date(dateTimeStr);
    const utc = date.getTime() + date.getTimezoneOffset() * 60000;
    return new Date(utc + 330 * 60000);
  }

  try {
    const [leadsCountRes, closedCountRes, propertiesResponse, usersResponse] = await Promise.all([
      fetch(`/api/companies/${companyId}/leads/count`, { method: "GET", headers: { "Content-Type": "application/json" }, credentials: "include" }),
      fetch(`/api/companies/${companyId}/leads/count/closed`, { method: "GET", headers: { "Content-Type": "application/json" }, credentials: "include" }),
      fetch(`/api/companies/${companyId}/properties/count`, { method: "GET", headers: { "Content-Type": "application/json" }, credentials: "include" }),
      fetch(`/api/users/company/${companyId}`, { method: "GET", headers: { "Content-Type": "application/json" }, credentials: "include" }),
    ]);

    if (!leadsCountRes.ok || !closedCountRes.ok || !propertiesResponse.ok || !usersResponse.ok)
      throw new Error("Failed to fetch dashboard metrics");

    const [totalLeads, dealsClosed, totalProperties, users] = await Promise.all([
      leadsCountRes.json(),
      closedCountRes.json(),
      propertiesResponse.json(),
      usersResponse.json(),
    ]);
    const totalUsers = users.filter(user => user.role === "USER").length;
    const totalAdmins = users.filter(user => user.role === "ADMIN").length;


    const [userNotesResponse, publicNotesResponse] = await Promise.all([
      fetch(`/api/companies/${companyId}/notes/visible-to/${userId}`, {
        method: "GET", headers: { "Content-Type": "application/json" }, credentials: "include"
      }),
      fetch(`/api/companies/${companyId}/notes/public-and-admin?adminId=${userId}`, {
        method: "GET", headers: { "Content-Type": "application/json" }, credentials: "include"
      })
    ]);

    if (!userNotesResponse.ok || !publicNotesResponse.ok)
      throw new Error("Failed to fetch notes");

    let userNotes = await userNotesResponse.json();
    let publicNotes = await publicNotesResponse.json();

    userNotes = userNotes.filter(note => note.status !== "COMPLETED");
    publicNotes = publicNotes.filter(note => note.status !== "COMPLETED");

    const notesMap = new Map();
    [...userNotes, ...publicNotes].forEach(note => {
      notesMap.set(note.id, note); // assumes note.id is unique
    });

    let allNotes = Array.from(notesMap.values());

    const todayStr = getKolkataDateString();
    const nowKolkata = getKolkataDateTime();

    allNotes = allNotes.filter(note => {
      if (!note.dateTime) return false;
      const kolkataDateTime = convertToKolkataTime(note.dateTime);
      const noteDateStr = kolkataDateTime.toISOString().split("T")[0];
      return noteDateStr === todayStr && kolkataDateTime >= nowKolkata;
    });

    const notesWithUsernames = await Promise.all(
      allNotes.map(async note => {
        try {
          if (!note.userId) {
            note.username = "Unknown";
            return note;
          }
          const res = await fetch(`/api/users/${note.userId}/username`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
            credentials: "include"
          });
          note.username = await res.text();
        } catch {
          note.username = "Unknown";
        }
        return note;
      })
    );

    let notesTableRows = "";
    if (notesWithUsernames.length > 0) {
      notesTableRows = notesWithUsernames.map(note => {
        const scheduledTime = convertToKolkataTime(note.dateTime).toLocaleTimeString("en-IN", {
          timeZone: "Asia/Kolkata",
          hour: "2-digit",
          minute: "2-digit",
          hour12: false
        });
        return `
          <tr>
            <td data-label="Content">${note.content}</td>
            <td data-label="Scheduled Date & Time">${scheduledTime}</td>
            <td data-label="Created By">${note.username}</td>
          </tr>
        `;
      }).join("");
    }

    const notesTableDisplay = notesWithUsernames.length > 0 ? "table" : "none";

    return `
      <style>
        .table-responsive {
          overflow-x: auto;
          -webkit-overflow-scrolling: touch;
          width: 100%;
        }

        #notesTable {
          width: 100%;
          border-collapse: collapse;
          min-width: 600px;
        }

        #notesTable th, #notesTable td {
          padding: 8px 12px;
          text-align: left;
          white-space: nowrap;
        }

        @media (max-width: 768px) {
          #notesTable, #notesTable thead, #notesTable tbody, #notesTable th, #notesTable td, #notesTable tr {
            display: block;
            width: 100%;
          }

          #notesTable thead {
            display: none;
          }

          #notesTable tr {
            margin-bottom: 15px;
            border: 1px solid #ccc;
            padding: 10px;
          }

          #notesTable td {
            text-align: right;
            padding-left: 50%;
            position: relative;
          }

          #notesTable td::before {
            content: attr(data-label);
            position: absolute;
            left: 10px;
            width: 45%;
            white-space: nowrap;
            text-align: left;
            font-weight: bold;
          }
        }
      </style>

      <div class="stat">
        <div class="stat-card"><h3>Total Leads</h3><div class="number">${totalLeads}</div></div>
        <div class="stat-card"><h3>Property Listed</h3><div class="number">${totalProperties}</div></div>
        <div class="stat-card"><h3>Deals Closed</h3><div class="number">${dealsClosed}</div></div>
        <div class="stat-card"><h3>Total Users</h3><div class="number">${totalUsers}</div></div>
        <div class="stat-card"><h3>Total Admins</h3><div class="number">${totalAdmins}</div></div>
      </div>


      <div class="notes-section">
        <h3>Your Event For Today</h3>
        ${notesWithUsernames.length === 0 ? `<div>No notes for today.</div>` : ""}
        <div class="table-responsive" style="display: ${notesTableDisplay}; margin-top: 10px;">
          <table id="notesTable" border="1" cellspacing="0" cellpadding="8">
            <thead>
              <tr>
                <th>Content</th>
                <th>Scheduled Date & Time</th>
                <th>Created By</th>
              </tr>
            </thead>
            <tbody>
              ${notesTableRows}
            </tbody>
          </table>
        </div>
      </div>
    `;
  } catch (error) {
    console.error("Error fetching dashboard data:", error);
    return `
      <div class="stat">
        <div class="stat-card"><h3>Total Leads</h3><div class="number">Error</div></div>
        <div class="stat-card"><h3>Property Listed</h3><div class="number">Error</div></div>
        <div class="stat-card"><h3>Deals Closed</h3><div class="number">Error</div></div>
        <div class="stat-card"><h3>Total Users</h3><div class="number">Error</div></div>
      </div>
      <div class="notes-section"><h3>Today's Notes</h3><div>Error loading notes</div></div>
    `;
  }
}
