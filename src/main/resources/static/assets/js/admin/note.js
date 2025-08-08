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

        <!-- Event date & time -->
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
            <option value="ALL_USERS">All Users</option>
            <option value="ME_AND_DIRECTOR">Me and Director</option>
            <option value="SPECIFIC_USERS">Specific Users</option>
          </select>
        </div>

        <!-- Specific users -->
        <div id="specificUsersSection" style="display: none; margin-top: 10px;">
          <label>Select Specific Users:</label>
          <div id="userCheckboxes"></div>
        </div>

        <!-- Priority -->
        <div style="margin-top: 8px;">
          <label for="priority">Priority:</label><br>
          <select id="priority" name="priority" required style="background: #333; color: #fff;">
            <option value="PRIORITY_A">Priority A</option>
            <option value="PRIORITY_B">Priority B</option>
            <option value="PRIORITY_C">Priority C</option>
          </select>
        </div>

        <!-- Content -->
        <div style="margin-top: 8px;">
          <label for="content">Content:</label><br>
          <textarea id="content" name="content" rows="4" cols="50" required></textarea>
        </div>

        <!-- Save Button -->
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
  const specificUsersSection = document.getElementById('specificUsersSection');
  const userCheckboxes = document.getElementById('userCheckboxes');

  visibilitySelect.addEventListener('change', async () => {
    const show = visibilitySelect.value === 'SPECIFIC_USERS';
    specificUsersSection.style.display = show ? 'block' : 'none';
    if (show) {
      await loadUsersForNotes();
    } else {
      userCheckboxes.innerHTML = '';
    }
  });

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
   Load users when “Specific Users” selected
-------------------------------------------------------------- */
async function loadUsersForNotes() {
  const container = document.getElementById('userCheckboxes');
  if (!container) return;

  const companyId = parseInt(localStorage.getItem('companyId'), 10);

  try {
    const res = await fetch(`/api/users/company/${companyId}`, {
      method: "GET",
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    if (!res.ok) throw new Error(res.statusText);

    const users = (await res.json()).filter(u => u.role === 'USER');

    container.innerHTML = users.length
      ? users.map(u => `
          <div style="margin-bottom:6px; display: flex; align-items: center; margin-right:70%;">
            <label for="user-${u.userId}" style=" margin-right:8px; color:black;">${u.name || 'Unnamed User'}</label>
            <input type="checkbox"
                   id="user-${u.userId}"
                   name="specificUsers"
                   value="${u.userId}"
                   style="margin-left:auto; width:18px; height:18px; cursor:pointer;">
          </div>`).join('')
      : '<p style="color:#ccc;">No users found.</p>';

  } catch (e) {
    console.error('Failed to load users:', e);
    container.innerHTML = '<p style="color:red;">Error loading users.</p>';
  }
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
  const priority = document.getElementById("priority").value;



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

  let visibleUserIds = [];
  if (visibility === 'SPECIFIC_USERS') {
    visibleUserIds = Array.from(
      document.querySelectorAll('input[name="specificUsers"]:checked')
    ).map(cb => parseInt(cb.value, 10));

    if (visibleUserIds.length === 0) {
      customAlert('Select at least one specific user.');
      btn.disabled = false;
      return;
    }
  }

  const payload = {
    userId,
    type,
    content,
    dateTime: dateTimeIso,
    visibility,
    visibleUserIds,
    priority
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

let allNotes = [];

function getNoteTableTemplate() {
  return `
    <div>
      <h2>All Notes</h2>
      
          <input type="text" id="searchNotesInput" placeholder="Search notes..." oninput="filterNotes()" style="margin-bottom: 10px; padding: 5px; width: 100%; max-width: 300px;">

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





// --------- Function to load notes ----------






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
    userNotesUrl: `/api/companies/${companyId}/notes/visible-to/${userId}`,
    publicNotesUrl: `/api/companies/${companyId}/notes/public-and-admin?adminId=${userId}`,
    getUsernameUrl: (id) => `/api/users/${id}/username`
  };

  try {
    const [userRes, publicRes] = await Promise.all([
      fetch(endpoints.userNotesUrl, { headers: { 'Content-Type': 'application/json' }, credentials: 'include' }),
      fetch(endpoints.publicNotesUrl, { headers: { 'Content-Type': 'application/json' }, credentials: 'include' })
    ]);

    if (!userRes.ok || !publicRes.ok) throw new Error("Failed to fetch notes.");

    const userNotes = await userRes.json();
    const publicNotes = await publicRes.json();

    const noteMap = new Map();
    [...userNotes, ...publicNotes].forEach(note => noteMap.set(note.id, note));
    const allNotesRaw = [...noteMap.values()];

    const openNotes = allNotesRaw.filter(n => n.status !== "COMPLETED").sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    const closedNotes = allNotesRaw.filter(n => n.status === "COMPLETED").sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    const allNotes = [...openNotes, ...closedNotes];

    window.allNotes = allNotes;

    const newHash = JSON.stringify(allNotes.map(n => [
      n.id, n.status, n.updatedAt || n.createdAt, n.content, n.dateTime
    ]));

    if (newHash === previousNotesHash) {
      if (loading) loading.style.display = "none";
      return;
    }
    previousNotesHash = newHash;

    const uniqueUserIds = [
      ...new Set(
        allNotes.flatMap(n =>
          [n.userId, ...(n.visibleUserIds || [])]
        ).filter(Boolean)
      )
    ];

    const idToName = {};
    await Promise.all(uniqueUserIds.map(async id => {
      try {
        const res = await fetch(endpoints.getUsernameUrl(id), {
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

    const fmtTime = d =>
      d ? new Date(d).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "-";

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
          ${["NEW", "PROCESSING", "COMPLETED"]
            .map(s => `<option value="${s}" ${note.status === s ? "selected" : ""}>${s}</option>`)
            .join("")}
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
            .map(p => `<option value="${p}" ${note.priority === p ? "selected" : ""}>${p.replace("PRIORITY_", "Priority ")}</option>`)
            .join("")}
        </select>`;

      const schedStr = schedDT
        ? schedDT.toLocaleDateString() + " " + fmtTime(schedDT)
        : "N/A";

      const createdStr = createdDT
        ? createdDT.toLocaleDateString() + " " + fmtTime(createdDT)
        : "-";

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

      const visibilityContent = note.visibility === "SPECIFIC_USERS"
        ? (note.visibleUserIds || [])
            .map(id => safe(idToName[id] || "Unknown"))
            .join(", ")
        : safe(note.visibility);

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
          <td>${visibilityContent}</td>
          <td>${createdStr}</td>
          <td>${safe(createdBy)}</td>
          <td>
            <button class="action-btn" onclick="openEditModal(${note.id})">Edit</button>
            <button class="action-btn" onclick="deleteNote(${note.id})">Delete</button>
          </td>
        </tr>`;
    });

    tbody.innerHTML = rows.join("");
  } catch (error) {
    console.error("Error loading notes:", error);
    tbody.innerHTML = `<tr><td colspan="10">Error loading notes.</td></tr>`;
  } finally {
    if (loading) loading.style.display = "none";
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
      method: "GET",
      headers: { "Content-Type": "application/json" },
      credentials: "include"
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
      headers: {
        'Content-Type': 'application/json'
      },
      credentials: "include",
      body: JSON.stringify(updatePayload)
    });

    if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
    customAlert("Note updated successfully.");
    closeEditModal();
    await loadNotes();  // ✅ Refresh notes table only
  } catch (error) {
    console.error("Error updating note:", error);
    customAlert("Failed to update note.");
  }
}


async function reloadNotesTable() {
  const tableBody = document.querySelector("#notesTable tbody"); // Update with actual table ID
  if (!tableBody) return;

  tableBody.innerHTML = ""; // Clear existing rows

  const companyId = parseInt(localStorage.getItem("companyId"), 10) || 1;

  try {
    const response = await fetch(`/api/companies/${companyId}/notes`, {
      method: "GET",
      headers: { "Content-Type": "application/json" },
      credentials: "include"
    });
    const notes = await response.json();

    notes.forEach(note => {
      const row = document.createElement("tr");

      // Apply your row styling based on status
      const rowBg = {
        NEW: "#fff8e1",
        PROCESSING: "#e0f7fa",
        COMPLETED: "#f8d7da"
      }[note.status] || "transparent";

      row.style.backgroundColor = rowBg;

      row.innerHTML = `
        <td>${note.id}</td>
        <td>${note.content}</td>
        <td>${note.status}</td>
        <td>${note.dateTime || "N/A"}</td>
        <td><button onclick="handleEditNote(${note.id})">Edit</button></td>
      `;

      tableBody.appendChild(row);
    });
  } catch (err) {
    console.error("Failed to reload notes table:", err);
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


function deleteNote(noteId) {
  const companyId = parseInt(localStorage.getItem("companyId"), 10);
  const confirmed = confirm("Are you sure you want to delete this Note?");
  if (!confirmed) return;

  fetch(`/api/companies/${companyId}/notes/${noteId}`, {
    method: 'DELETE',
    headers: { "Content-Type": "application/json" },
    credentials: "include"
  })
    .then(response => {
      if (response.ok) {
        customAlert("note deleted successfully.");
        loadNotes();
        // Refresh your list or remove the deleted property from the DOM
      } else {
        return response.text().then(text => { throw new Error(text) });
      }
    })
    .catch(error => {
      console.error("Error deleting note:", error);
      customAlert("Failed to delete note.");
    });
}
