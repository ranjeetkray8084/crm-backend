/* --------------------------------------------------------------
   Add‑Note form HTML
-------------------------------------------------------------- */
function getAddNoteForm() {
  return `
  <style>
    input[type="date"],
    input[type="time"] {
      background-color: #333;
      color: #fff;
      border: 1px solid #555;
      padding: 4px 6px;
      border-radius: 4px;
    }
    input[type="date"]::-webkit-calendar-picker-indicator,
    input[type="time"]::-webkit-calendar-picker-indicator {
      filter: invert(1);
      cursor: pointer;
    }
  </style>

  <div class="addform">
    <div style="max-height: 500px; overflow-y: auto; border: 1px solid #ccc; padding: 15px;">
      <form id="addNoteForm">
        <!-- Type -->
        <div>
          <label for="noteType">Type:</label><br>
          <select id="noteType" name="noteType" required style="background: #333; color: #fff;">
            <option value="NOTE">Note</option>
            <option value="EVENT">Event</option>
          </select>
        </div>

        <!-- Event date & time (hidden for NOTE) -->
        <div id="dateTimeSection" style="display: none;">
          <div id="dateWrapper">
            <label for="eventDate">Date:</label><br>
            <input type="date" id="eventDate" name="eventDate">
          </div>
          <div id="timeWrapper" style="margin-top: 6px;">
            <label for="eventTime">Time:</label><br>
            <input type="time" id="eventTime" name="eventTime">
          </div>
        </div>

        <!-- Visibility -->
        <div>
          <label for="visibility">Visibility:</label><br>
          <select id="visibility" name="visibility" required>
            <option value="ONLY_ME">Only Me</option>
           <option value="ME_AND_ADMIN">Me and Admin</option>
           <option value="ME_AND_DIRECTOR">Me and Director</option>

          </select>
        </div>
        <!-- Content -->
        <div style="margin-top: 8px;">
          <label for="content">Content:</label><br>
          <textarea id="content" name="content" rows="4" cols="50" required></textarea>
        </div>

        <div style="margin-top: 15px;">
          <button type="button" id="saveNoteBtn">Add Note</button>
        </div>
      </form>
    </div>
  </div>
  `;
}

/* --------------------------------------------------------------
   Attach all dynamic behaviour AFTER the form is injected
-------------------------------------------------------------- */
function setupFormEvents() {
  const noteType = document.getElementById('noteType');
  const dateTimeSection = document.getElementById('dateTimeSection');
  const eventDateInput = document.getElementById('eventDate');
  const eventTimeInput = document.getElementById('eventTime');

  function toggleDateTimeVisibility() {
    const isEvent = noteType.value === 'EVENT';
    dateTimeSection.style.display = isEvent ? 'block' : 'none';
    eventDateInput.required = isEvent;
    eventTimeInput.required = isEvent;
    if (!isEvent) {
      eventDateInput.value = '';
      eventTimeInput.value = '';
    }
  }

  noteType.addEventListener('change', toggleDateTimeVisibility);
  toggleDateTimeVisibility();

  const visibilitySelect = document.getElementById('visibility');


  setMinDateTime();

  // Replace old button to clear previous event listeners
  const oldBtn = document.getElementById('saveNoteBtn');
  const newBtn = oldBtn.cloneNode(true);
  oldBtn.parentNode.replaceChild(newBtn, oldBtn);

  // Add event listener for saving
  newBtn.addEventListener('click', async (e) => {
    e.preventDefault();
    await saveNote();
  });
}


/* --------------------------------------------------------------
   Set minimum date (today) and minimum time (now) helpers
-------------------------------------------------------------- */
function setMinDateTime() {
  const dateInput = document.getElementById('eventDate');
  const timeInput = document.getElementById('eventTime');
  if (!dateInput || !timeInput) return;

  const now = new Date();
  const yyyy = now.getFullYear();
  const mm = String(now.getMonth() + 1).padStart(2, '0');
  const dd = String(now.getDate()).padStart(2, '0');
  const hh = String(now.getHours()).padStart(2, '0');
  const min = String(now.getMinutes()).padStart(2, '0');

  dateInput.min = `${yyyy}-${mm}-${dd}`;
  timeInput.min = `${hh}:${min}`;
}




/* --------------------------------------------------------------
   Save note / event
-------------------------------------------------------------- */
async function saveNote() {
  const btn = document.getElementById('saveNoteBtn');
  btn.disabled = true; // Prevent double click

  const companyId = parseInt(localStorage.getItem('companyId'), 10);
  if (!companyId) {
    customAlert('Company ID not found.');
    btn.disabled = false;
    return;
  }

  const localUser = JSON.parse(localStorage.getItem('user') || '{}');
  const userId = localUser.userId;
  if (!userId) {
    customAlert('User ID not found.');
    btn.disabled = false;
    return;
  }

  const type = document.getElementById('noteType').value;
  const content = document.getElementById('content').value.trim();
  const visibility = document.getElementById('visibility').value;

  if (!content) {
    customAlert('Please enter content.');
    btn.disabled = false;
    return;
  }

  let dateTimeIso = null;
  if (type === 'EVENT') {
    const eventDate = document.getElementById('eventDate').value;
    const eventTime = document.getElementById('eventTime').value;

    if (!eventDate || !eventTime) {
      customAlert('Please select both date and time for the event.');
      btn.disabled = false;
      return;
    }

    dateTimeIso = `${eventDate}T${eventTime}:00`;
  }



  const payload = {
    userId,
    type,
    content,
    dateTime: dateTimeIso,
    visibility
  };

  try {
    const res = await fetch(
      `/api/companies/${companyId}/notes`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(payload)
      }
    );

    if (!res.ok) {
      const err = await res.text();
      throw new Error(err);
    }

    customAlert('Saved successfully!');
    loadNotes();
    document.getElementById('addNoteForm').reset();
    setupFormEvents(); // Reattach if needed
    await loadNotes();

  } catch (e) {
    console.error('Save failed:', e);
    customAlert(`Failed to save: ${e.message}`);
  } finally {
    btn.disabled = false; // Re-enable the button
  }
}





// --------- Function to return Notes Table Template ----------

function getNoteTableTemplate() {
  return `
    <div>
      <h2>All Notes</h2>
      <input type="text" id="searchNotesInput" placeholder="Search notes..." oninput="filterNotes()" 
             style="margin-bottom: 10px; padding: 5px; ">
      <div class="table-responsive" style="position: relative; height: 65vh;">
      <!-- ✅ Loader -->
      <div id="glass-loader" class="glass-loader" style="display: none;">
        <div class="spinner"></div>
        <div class="loading-text">Loading Notes...</div>
      </div>
        <table id="notesTable" border="1" cellspacing="0" cellpadding="8" style="width: 100%; border-collapse: collapse;">
          <thead>
            <tr class="sticky">
              <th>Content</th>
              <th>Status</th>
              <th>Priority</th>
              <th>Scheduled Date & Time</th>
              <th>Type</th>
              <th>Remark</th>
              <th>Created For</th>
              <th>Created At</th>
              <th>Created By</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody id="notesTableBody"></tbody>
        </table>
      </div>
    </div>
  `;
}


function filterNotes() {
  const searchInput = document.getElementById('searchNotesInput').value.toLowerCase();
  const table = document.getElementById('notesTable');
  const rows = table.getElementsByTagName('tr');

  for (let i = 1; i < rows.length; i++) { // skip header row
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

// -------------------------------------------------------------
//  light in‑memory cache so we only rebuild when data changes
// -------------------------------------------------------------


let previousNotesHash = "";

async function loadNotes() {
  const localUser = JSON.parse(localStorage.getItem("user") || "{}");
  const userId = localUser.userId;
  const companyId = parseInt(localStorage.getItem("companyId"), 10);

  if (!userId || !companyId) {
    customAlert("User or Company ID not found. Please log in again.");
    return;
  }

  const loading = document.getElementById("glass-loader");
  const tbody = document.getElementById("notesTableBody");

  if (loading) loading.style.display = "flex";
  if (!tbody) {
    console.error("notesTableBody not found!");
    return;
  }

  const endpoints = {
    userNotes: `/api/companies/${companyId}/notes/user/${userId}`,
    visibleNotes: `/api/companies/${companyId}/notes/visible-to/${userId}`,
    publicNotes: `/api/companies/${companyId}/notes/public`,
    getUsername: id => `/api/users/${id}/username`
  };

  try {
    const [userRes, visibleRes, publicRes] = await Promise.all([
      fetch(endpoints.userNotes, { headers: { 'Content-Type': 'application/json' }, credentials: 'include' }),
      fetch(endpoints.visibleNotes, { headers: { 'Content-Type': 'application/json' }, credentials: 'include' }),
      fetch(endpoints.publicNotes, { headers: { 'Content-Type': 'application/json' }, credentials: 'include' })
    ]);

    if (!userRes.ok || !visibleRes.ok || !publicRes.ok) throw new Error("Failed to fetch one or more note types.");

    const userNotes = await userRes.json();
    const visibleNotes = await visibleRes.json();
    const publicNotes = await publicRes.json();

    const noteMap = new Map();
    [...userNotes, ...visibleNotes, ...publicNotes].forEach(note => noteMap.set(note.id, note));
    const allNotesRaw = [...noteMap.values()];

    const openNotes = allNotesRaw.filter(n => n.status !== "COMPLETED").sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    const closedNotes = allNotesRaw.filter(n => n.status === "COMPLETED").sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    const allNotes = [...openNotes, ...closedNotes];

    const newHash = JSON.stringify(allNotes.map(n => [n.id, n.status, n.updatedAt || n.createdAt, n.content, n.dateTime]));
    if (newHash === previousNotesHash) {
      if (loading) loading.style.display = "none";
      return;
    }
    previousNotesHash = newHash;

    const uniqueUserIds = [...new Set(allNotes.map(n => n.userId).filter(Boolean))];
    const idToName = {};
    await Promise.all(uniqueUserIds.map(async id => {
      try {
        const res = await fetch(endpoints.getUsername(id), {
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include'
        });
        idToName[id] = res.ok ? await res.text() : "Unknown";
      } catch {
        idToName[id] = "Unknown";
      }
    }));

    const safe = text => {
      const temp = document.createElement("div");
      temp.textContent = text ?? "";
      return temp.innerHTML || "N/A";
    };

    const fmtTime = d => d ? new Date(d).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "-";

    const getWordChunks = (text, chunkSize = 5) => {
      const words = text.split(/\s+/);
      const chunks = [];
      for (let i = 0; i < words.length; i += chunkSize) {
        chunks.push(words.slice(i, i + chunkSize).join(" "));
      }
      return chunks;
    };

    const rows = allNotes.map(note => {
      const createdBy = idToName[note.userId] || "Unknown";
      const schedDT = note.dateTime ? new Date(note.dateTime) : null;
      const createdDT = note.createdAt ? new Date(note.createdAt) : null;

      const rowBg = {
        NEW: "#fff8e1",
        PROCESSING: "#e0f7fa",
        COMPLETED: "#f8d7da"
      }[note.status] || "transparent";

      const selBg = {
        NEW: "#f0ad4e",
        PROCESSING: "#5bc0de",
        COMPLETED: "#d9534f"
      }[note.status] || "#ccc";

      const statusSelect = `
        <select onchange="updateNoteStatus(${note.id}, this.value)" style="
          background:${selBg}; color:white; border:none; padding:2px 6px; border-radius:4px;">
          ${["NEW", "PROCESSING", "COMPLETED"].map(s => `<option value="${s}" ${note.status === s ? "selected" : ""}>${s}</option>`).join("")}
        </select>`;

      const priorityBg = {
        PRIORITY_A: "#f44336",
        PRIORITY_B: "#ff9800",
        PRIORITY_C: "#4caf50"
      }[note.priority] || "#777";

      const prioritySelect = `
        <select onchange="updateNotePriority(${note.id}, this.value)" style="
          background:${priorityBg}; color:white; border:none; padding:2px 6px; border-radius:4px;">
          ${["PRIORITY_A", "PRIORITY_B", "PRIORITY_C"]
            .map(p => `<option value="${p}" ${note.priority === p ? "selected" : ""}>${p.replace("PRIORITY_", "Priority ")}</option>`).join("")}
        </select>`;

      const schedStr = schedDT ? schedDT.toLocaleDateString() + " " + fmtTime(schedDT) : "N/A";
      const createdStr = createdDT ? createdDT.toLocaleDateString() + " " + fmtTime(createdDT) : "-";
      const typeStr = schedDT ? "Event" : "Note";

      const fullText = note.content ?? "";
      const chunks = getWordChunks(fullText);
      const divId = `note-content-${note.id}`;

      const contentHtml = `
        <div id="${divId}">
          <div class="note-content" data-index="0" data-chunks='${JSON.stringify(chunks)}'>
            ${safe(chunks[0] ?? "")}
          </div>
          ${chunks.length > 1
            ? `<button class="toggle-btn" onclick="toggleNoteContent('${divId}')">Show more</button>`
            : ""}
        </div>`;

      return `
        <tr style="background:${rowBg}">
          <td>${contentHtml}</td>
          <td>${statusSelect}</td>
          <td>${prioritySelect}</td>
          <td>${schedStr}</td>
          <td>${typeStr}</td>
          <td>
            <button class="action-btn" onclick="openAddNoteRemarkModal(${note.id})">Add</button>
            <button class="action-btn" onclick="viewNoteRemarks(${note.id})">View</button>
          </td>
          <td>${safe(note.visibility)}</td>
          <td>${createdStr}</td>
          <td>${safe(createdBy)}</td>
          <td><button class="action-btn" onclick="openEditModal(${note.id})">Edit</button></td>
        </tr>`;
    });

    tbody.innerHTML = rows.join("");
  } catch (error) {
    console.error("Error loading notes:", error);
    tbody.innerHTML = `<tr><td colspan="9">Error loading notes.</td></tr>`;
  } finally {
    if (loading) loading.style.display = "none";
  }
}

// Toggle next 50-word chunk
function toggleNoteContent(divId) {
  const container = document.getElementById(divId);
  const contentDiv = container.querySelector(".note-content");
  const toggleBtn = container.querySelector(".toggle-btn");

  const chunks = JSON.parse(contentDiv.getAttribute("data-chunks"));
  const isExpanded = contentDiv.getAttribute("data-expanded") === "true";

  if (!isExpanded) {
    // ✅ Show all chunks
    contentDiv.innerHTML = chunks.map(chunk => `<p>${chunk}</p>`).join("");
    contentDiv.setAttribute("data-expanded", "true");
    toggleBtn.textContent = "Show less";
  } else {
    // 🔁 Collapse to only first chunk
    contentDiv.innerHTML = `<p>${chunks[0]}</p>`;
    contentDiv.setAttribute("data-expanded", "false");
    toggleBtn.textContent = "Show more";
  }
}



async function updateNotePriority(noteId, newPriority) {
  const companyId = parseInt(localStorage.getItem("companyId"), 10);
  if (!companyId) {
    customAlert("Company ID not found. Please login again.");
    return;
  }

  try {
    const res = await fetch(`/api/companies/${companyId}/notes/${noteId}/priority?priority=${newPriority}`, {
      method: "PATCH",
      credentials: "include"
    });

    if (!res.ok) throw new Error("Failed to update priority");

    // ✅ Select element ko dhoondo
    const selectElem = document.querySelector(`select[onchange*="updateNotePriority(${noteId}"`);
    if (selectElem) {
      const newBg = {
        PRIORITY_A: "#f44336", // red
        PRIORITY_B: "#ff9800", // orange
        PRIORITY_C: "#4caf50"  // green
      }[newPriority] || "#777";

      // ✅ background color update karo
      selectElem.style.background = newBg;
    }


    customAlert("Priority updated successfully!");

    await loadNotes();

  } catch (err) {
    console.error("Error updating priority:", err);
    customAlert("Error updating priority.");
  }
}


// 1. Modal HTML template
function getEditModalTemplate() {
  return `
    <div id="editModal" style="display:none;position:fixed;top:10%;left:50%;transform:translateX(-50%);
         width:450px;background:#222;color:#eee;border:1px solid #444;border-radius:8px;padding:20px;z-index:1050;">
      <h3>Edit Note</h3>
      <input type="hidden" id="editNoteId" />
      
      <div class="field" style="margin-bottom:10px;">
        <label>Scheduled Date</label><br/>
        <input type="date" id="editScheduledDate" style="width:48%;background:#333;color:#fff;border:1px solid #555; margin:10px;" />
        <input type="time" id="editScheduledTime" style="width:48%;background:#333;color:#fff;border:1px solid #555; margin:10px;" />
      </div>
      
      <div class="field" style="margin-bottom:20px;">
        <label>Content</label><br/>
        <input type="text" id="editContent" style="width:100%;background:#333;color:#fff;border:1px solid #555;" />
      </div>
      
      <div style="text-align:right;">
        <button onclick="submitNoteUpdate()" style="margin-right:8px;">Update</button>
        <button onclick="closeEditModal()">Cancel</button>
      </div>
    </div>
  `;
}

// Ensures the modal is appended correctly to DOM
function renderEditModal() {
  if (!document.getElementById('editModal')) {
    const modalWrapper = document.createElement('div');
    modalWrapper.innerHTML = getEditModalTemplate();

    // ✅ Append the actual modal, not the wrapper
    document.body.appendChild(modalWrapper.firstElementChild);
  }
}

async function openEditModal(noteId) {
  renderEditModal();

  const companyId = parseInt(localStorage.getItem("companyId"), 10) || 1;
  const noteUrl = `/api/companies/${companyId}/notes/${noteId}`;

  try {
    const response = await fetch(noteUrl, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
    });
    if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);

    const note = await response.json();

    document.getElementById('editNoteId').value = note.id;

    if (note.dateTime) {
      const dateObj = new Date(note.dateTime);
      const dateStr = dateObj.toISOString().split('T')[0];       // yyyy-MM-dd
      const timeStr = dateObj.toTimeString().slice(0, 5);         // HH:mm

      document.getElementById('editScheduledDate').value = dateStr;
      document.getElementById('editScheduledTime').value = timeStr;
    } else {
      document.getElementById('editScheduledDate').value = '';
      document.getElementById('editScheduledTime').value = '';
    }

    document.getElementById('editContent').value = note.content || '';
    document.getElementById('editModal').style.display = 'block';

  } catch (error) {
    console.error("Error loading note by ID:", error);
    customAlert("Failed to load note for editing.");
  }
}

async function submitNoteUpdate() {
  const noteIdEl = document.getElementById('editNoteId');
  const dateEl = document.getElementById('editScheduledDate');
  const timeEl = document.getElementById('editScheduledTime');
  const contentEl = document.getElementById('editContent');

  if (!noteIdEl || !dateEl || !timeEl || !contentEl) {
    customAlert('Modal elements not found in DOM! Make sure modal is rendered.');
    return;
  }

  const noteId = noteIdEl.value;
  const scheduledDate = dateEl.value;
  const scheduledTime = timeEl.value;
  const content = contentEl.value.trim();
  const companyId = parseInt(localStorage.getItem("companyId"), 10) || 1;

  let dateTime = null;
  if (scheduledDate && scheduledTime) {
    dateTime = `${scheduledDate}T${scheduledTime}:00`;
  } else if (scheduledDate) {
    dateTime = `${scheduledDate}T00:00:00`;
  }

  const updatePayload = {
    dateTime: dateTime,
    content: content
  };

  try {
    const response = await fetch(`/api/companies/${companyId}/notes/${noteId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(updatePayload)
    });

    if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
    customAlert("Note updated successfully.");
    closeEditModal();
    loadNotes();  // ✅ Refresh notes table only
  } catch (error) {
    console.error("Error updating note:", error);
    customAlert("Failed to update note.");
  }
}

function closeEditModal() {
  const modal = document.getElementById('editModal');
  if (modal) modal.style.display = 'none';
}






// 3. Open modal and load note by ID

// 4. Close modal






async function updateNoteStatus(noteId, newStatus) {
  const companyId = parseInt(localStorage.getItem("companyId"), 10);
  if (!companyId) {
    customAlert("Company ID not found. Please select a company.");
    return;
  }

  try {
    const response = await fetch(
      `/api/companies/${companyId}/notes/${noteId}/status?status=${newStatus}`,
      {
        method: 'PATCH',
        headers: { "Content-Type": "application/json" },
        credentials: "include"
      }
    );
    if (!response.ok) {
      throw new Error('Failed to update status');
    }

    // ✅ Update dropdown background color instantly
    const selectElem = document.querySelector(`select[onchange*="updateNoteStatus(${noteId}"`);
    if (selectElem) {
      const newBg = {
        NEW: "#f0ad4e",         // yellow
        PROCESSING: "#5bc0de",  // blue
        COMPLETED: "#d9534f"    // red
      }[newStatus] || "#ccc";

      selectElem.style.background = newBg;
    }

    // ✅ Optional success feedback
    customAlert("Status updated successfully!");

    await loadNotes();

  } catch (error) {
    customAlert(`Error updating status: ${error.message}`);
  }
}

function openAddNoteRemarkModal(noteId) {
  currentNoteId = noteId;
  document.getElementById("noteRemarkInput").value = "";
  document.getElementById("noteModalBody").innerHTML = "";
  document.getElementById("noteRemarkInputContainer").style.display = "block";
  document.getElementById("noteRemarkModal").style.display = "block";
}

function safeDisplay(value) {
  return value ? String(value).replace(/</g, "&lt;").replace(/>/g, "&gt;") : "N/A";
}

async function viewNoteRemarks(noteId) {
  const companyId = parseInt(localStorage.getItem("companyId"), 10);
  if (!companyId) {
    customAlert("Company ID not found in local storage!");
    return;
  }

  currentNoteId = noteId;
  document.getElementById("noteRemarkInputContainer").style.display = "none";
  document.getElementById("noteModalBody").innerHTML = "<p>Loading…</p>";

  try {
    const remarksRes = await fetch(`/api/companies/${companyId}/notes/${noteId}/remarks`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
    });
    if (!remarksRes.ok) throw new Error("Failed to fetch remarks");
    let remarks = await remarksRes.json();

    // Sort by latest
    remarks.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

    let html = '';
    if (remarks.length === 0) {
      html = "<p>No remarks found.</p>";
    } else {
      html = `<table style="width:100%;border-collapse:collapse;">
        <thead>
          <tr style="background-color:#f0f0f0;">
            <th style="padding:10px;border-bottom:1px solid #ccc;">Remark</th>
            <th style="padding:10px;border-bottom:1px solid #ccc;">Created By</th>
            <th style="padding:10px;border-bottom:1px solid #ccc;">Date & Time</th>
          </tr>
        </thead>
        <tbody>`;

      remarks.forEach(remark => {
        const createdAt = remark.createdAt
          ? new Date(remark.createdAt).toLocaleString('en-IN', {
            day: '2-digit',
            month: 'short',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            hour12: true
          })
          : '-';

        const createdByName = remark.createdBy?.name || "Unknown";

        html += `
          <tr>
            <td style="padding:10px;border-bottom:1px solid #ccc;">${safeDisplay(remark.remark)}</td>
            <td style="padding:10px;border-bottom:1px solid #ccc;">${safeDisplay(createdByName)}</td>
            <td style="padding:10px;border-bottom:1px solid #ccc;">${createdAt}</td>
          </tr>`;
      });

      html += '</tbody></table>';
    }

    document.getElementById("noteModalBody").innerHTML = html;
    document.getElementById("noteRemarkModal").style.display = "block";
  } catch (error) {
    document.getElementById("noteModalBody").innerHTML = `<p>Error loading remarks: ${error.message}</p>`;
  }
}


function submitNoteRemark() {
  const remark = document.getElementById("noteRemarkInput").value.trim();
  const companyId = parseInt(localStorage.getItem("companyId"), 10);
  const user = JSON.parse(localStorage.getItem("user") || "{}");
  const userId = user.userId;
  if (!companyId) return customAlert("Company ID not found in local storage!");
  if (!userId) return customAlert("User ID not found. Please login again.");
  if (!remark) return customAlert("Remark cannot be empty!");



  fetch(`/api/companies/${companyId}/notes/${currentNoteId}/remarks`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ remark, userId })
  })
    .then(res => {
      if (!res.ok) throw new Error("Failed to add note remark");
      customAlert("Note remark added successfully!");
      closeNoteRemarkModal();  // You should define this function to close the modal if applicable
    })
    .catch(err => customAlert(err.message));
}


function closeNoteRemarkModal() {
  document.getElementById("noteRemarkModal").style.display = "none";
  currentNoteId = null;
}



async function updateNoteStatus(noteId, newStatus) {
  const companyId = parseInt(localStorage.getItem("companyId"), 10);


  try {
    const response = await fetch(
      `/api/companies/${companyId}/notes/${noteId}/status?status=${newStatus}`,
      {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
      }
    );
    if (!response.ok) {
      throw new Error('Failed to update status');
    }
    // Optionally reload notes after update
    await loadNotes();
  } catch (error) {
    customAlert(`Error updating status: ${error.message}`);
  }
}




function closeNoteRemarkModal() {
  document.getElementById("noteRemarkModal").style.display = "none";
  currentNoteId = null;
}


