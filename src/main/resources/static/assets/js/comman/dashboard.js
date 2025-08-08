async function getDashboard() {
  const companyId = parseInt(localStorage.getItem("companyId"), 10);
  const localUser = JSON.parse(localStorage.getItem("user") || "{}");
  const userId = localUser.userId;

  const loading = document.getElementById("glass-loader");
  const tbody = document.querySelector("#leadTable tbody");

  if (loading) loading.style.display = "flex";
  if (tbody) tbody.innerHTML = "";

  const getKolkataDateTime = () => {
    const now = new Date();
    const offset = 330;
    const utc = now.getTime() + now.getTimezoneOffset() * 60000;
    return new Date(utc + offset * 60000);
  };

  const getKolkataDateString = () => {
    const kolkata = getKolkataDateTime();
    return kolkata.toISOString().split("T")[0];
  };

  const convertToKolkataTime = dateStr => {
    if (!dateStr) return null;
    const date = new Date(dateStr);
    return new Date(date.getTime() + (330 - date.getTimezoneOffset()) * 60000);
  };

  try {
    const [
      visibleLeadsCountRes,
      closedCountRes,
      propertiesResponse,
      userCountResponse
    ] = await Promise.all([
      fetch(`/api/companies/${companyId}/leads/count-visible-to-admin/${userId}`, { credentials: "include" }),
      fetch(`/api/companies/${companyId}/leads/count/closed-droped?companyId=${companyId}&adminId=${userId}`, { credentials: "include" }),
      fetch(`/api/companies/${companyId}/properties/count`, { credentials: "include" }),
      fetch(`/api/users/count-by-admin/${userId}?companyId=${companyId}`, { credentials: "include" })
    ]);

    if (![visibleLeadsCountRes, closedCountRes, propertiesResponse, userCountResponse].every(res => res.ok))
      throw new Error("Failed to fetch dashboard metrics");

    const [totalLeads, dealsClosed, totalProperties, totalUsers] = await Promise.all([
      visibleLeadsCountRes.json(),
      closedCountRes.json(),
      propertiesResponse.json(),
      userCountResponse.json()
    ]);

    const [userNotesRes, publicNotesRes] = await Promise.all([
      fetch(`/api/companies/${companyId}/notes/visible-to/${userId}`, { credentials: "include" }),
      fetch(`/api/companies/${companyId}/notes/public-and-admin?adminId=${userId}`, { credentials: "include" })
    ]);

    if (!userNotesRes.ok || !publicNotesRes.ok) throw new Error("Failed to fetch notes");

    const [userNotes, publicNotes] = await Promise.all([userNotesRes.json(), publicNotesRes.json()]);

    const allNotes = Array.from(new Map(
      [...userNotes, ...publicNotes]
        .filter(n => n.status !== "COMPLETED" && n.dateTime)
        .map(n => [n.id, n])
    ).values());

    const todayStr = getKolkataDateString();
    const now = getKolkataDateTime();

    const todayNotes = allNotes.filter(note => {
      const dt = convertToKolkataTime(note.dateTime);
      return dt && dt.toISOString().split("T")[0] === todayStr && dt >= now;
    });

    const notesWithNames = await Promise.all(todayNotes.map(async note => {
      try {
        const res = await fetch(`/api/users/${note.userId}/username`, { credentials: "include" });
        note.username = res.ok ? await res.text() : "Unknown";
      } catch {
        note.username = "Unknown";
      }
      return note;
    }));

    const notesTableRows = notesWithNames.map(note => {
      const timeStr = convertToKolkataTime(note.dateTime).toLocaleTimeString("en-IN", {
        timeZone: "Asia/Kolkata", hour: "2-digit", minute: "2-digit", hour12: false
      });
      return `
        <tr>
          <td data-label="Content">${note.content}</td>
          <td data-label="Scheduled Date & Time">${timeStr}</td>
          <td data-label="Created By">${note.username}</td>
        </tr>`;
    }).join("");

    const notesTableDisplay = notesWithNames.length > 0 ? "table" : "none";

    return `
      <style>
        .table-responsive { overflow-x: auto; width: 100%; }
        #notesTable { width: 100%; border-collapse: collapse; min-width: 600px; }
        #notesTable th, #notesTable td { padding: 8px 12px; white-space: nowrap; }

        @media (max-width: 768px) {
          #notesTable, #notesTable thead, #notesTable tbody, #notesTable th, #notesTable td, #notesTable tr {
            display: block; width: 100%;
          }
          #notesTable thead { display: none; }
          #notesTable tr { margin-bottom: 15px; border: 1px solid #ccc; padding: 10px; }
          #notesTable td {
            text-align: right; padding-left: 50%; position: relative;
          }
          #notesTable td::before {
            content: attr(data-label); position: absolute; left: 10px;
            width: 45%; white-space: nowrap; text-align: left; font-weight: bold;
          }
        }
      </style>

      <div class="stat">
        <div class="stat-card"><h3>Total Leads</h3><div class="number">${totalLeads}</div></div>
        <div class="stat-card"><h3>Property Listed</h3><div class="number">${totalProperties}</div></div>
        <div class="stat-card"><h3>Deals Closed</h3><div class="number">${dealsClosed}</div></div>
        <div class="stat-card"><h3>Total Users</h3><div class="number">${totalUsers}</div></div>
      </div>

      <div class="notes-section">
        <h3>Your Event For Today</h3>
        ${notesWithNames.length === 0 ? `<div>No notes for today.</div>` : ""}
        <div class="table-responsive" style="display: ${notesTableDisplay}; margin-top: 10px;">
          <table id="notesTable" border="1" cellspacing="0" cellpadding="8">
            <thead>
              <tr>
                <th>Content</th>
                <th>Scheduled Date & Time</th>
                <th>Created By</th>
              </tr>
            </thead>
            <tbody>${notesTableRows}</tbody>
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
  } finally {
    if (loading) loading.style.display = "none";
  }
}
