// followup.js

function getAddFollowUpForm() {
  return `
      <h2>Add Follow Up</h2>
      <form id="followUpForm" onsubmit="event.preventDefault(); saveFollowUp()">
        <input type="number" id="leadId" placeholder="Lead ID" required />
        <input type="number" id="userId" placeholder="User ID" required />
        <textarea id="note" placeholder="Follow-up Note" required></textarea>
  
        <label for="followupDate">Follow-up Date:</label>
        <input type="datetime-local" id="followupDate" required />
  
        <label for="nextFollowup">Next Follow-up:</label>
        <input type="datetime-local" id="nextFollowup" required />
  
        <button type="submit">Save Follow Up</button>
        <div id="followUpLoading" style="display:none;">Saving...</div>
      </form>
    `;
}

function getFollowUpTableTemplate() {
  return `
      <h2>Follow Ups</h2>
      <input type="text" id="followUpSearch" class="search-box" placeholder="Search follow-ups..." onkeyup="filterTable('followUpTable', 'followUpSearch')" />
      <div id="followUpTableLoading" style="display: none;">Loading follow-ups...</div>
      <table id="followUpTable">
        <thead>
          <tr>
            <th>ID</th>
            <th>Lead ID</th>
            <th>User ID</th>
            <th>Note</th>
            <th>Follow-up Date</th>
            <th>Next Follow-up</th>
            <th>Created At</th>
          </tr>
        </thead>
        <tbody></tbody>
      </table>
    `;
}

async function saveFollowUp() {
  const leadId = document.getElementById("leadId").value;
  const userId = document.getElementById("userId").value;
  const note = document.getElementById("note").value;
  const followupDate = document.getElementById("followupDate").value;
  const nextFollowup = document.getElementById("nextFollowup").value;

  const followUp = {
    lead: { leadId: parseInt(leadId) },
    user: { userId: parseInt(userId) },
    note,
    followupDate,
    nextFollowup
  };

  const loading = document.getElementById("followUpLoading");
  loading.style.display = "block";

  try {
    const response = await fetch("/api/followups", {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(followUp)
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || "Failed to save follow-up");
    }

    custemcustomAlert("Follow-up saved successfully!");
    document.getElementById("followUpForm").reset();
    await loadFollowUps();
  } catch (error) {
    custemcustomAlert(`Error: ${error.message}`);
    console.error(error);
  } finally {
    loading.style.display = "none";
  }
}

async function loadFollowUps() {
  const loading = document.getElementById("followUpTableLoading");
  loading.style.display = "block";

  try {
    const response = await fetch("/api/followups", {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
    });
    if (!response.ok) throw new Error("Failed to load follow-ups");

    const followUps = await response.json();
    const tbody = document.querySelector("#followUpTable tbody");
    tbody.innerHTML = followUps.map(fu => `
        <tr>
          <td>${fu.followupId}</td>
          <td>${fu.lead?.leadId || ''}</td>
          <td>${fu.user?.userId || ''}</td>
          <td>${fu.note}</td>
          <td>${fu.followupDate?.replace("T", " ")}</td>
          <td>${fu.nextFollowup?.replace("T", " ")}</td>
          <td>${fu.createdAt?.replace("T", " ")}</td>
        </tr>
      `).join('');
  } catch (error) {
    custemcustomAlert(`Error: ${error.message}`);
    console.error(error);
  } finally {
    loading.style.display = "none";
  }
}
