function getAddTaskForm() {
  return `
    <h2>Add Task</h2>
    <form id="taskForm" onsubmit="event.preventDefault(); saveTask()">
      <label for="excelFile">Select Excel File:</label>
      <input type="file" id="excelFile" accept=".xls,.xlsx" required />
      
      <label for="title">Title:</label>
      <input type="text" id="title" placeholder="Enter title" required />
      
      <button type="submit">Upload Task</button>
      
      <div id="taskLoading" style="display:none;">Uploading...</div>
    </form>
  `;
}


async function saveTask() {
  const title = document.getElementById('title').value.trim();
  const fileInput = document.getElementById('excelFile');
  const localUser = JSON.parse(localStorage.getItem("user") || "{}");
  const uploadedBy = localUser.userId || localUser.id;
  const companyId = parseInt(localStorage.getItem("companyId"), 10);

  if (!title || !fileInput.files.length || !uploadedBy || !companyId) {
    customAlert("❌ All required fields must be filled.");
    return;
  }

  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", fileInput.files[0]);
  formData.append("companyId", companyId);
  formData.append("uploadedBy", uploadedBy);

  document.getElementById("taskLoading").style.display = "block";

  try {
    const response = await fetch("/api/task-files/upload", {
      method: "POST",
      body: formData,           // ✅ Do NOT manually set Content-Type
      credentials: 'include'    // Optional: for cookies/session
    });

    if (response.ok) {
      const result = await response.text();
      customAlert("✅ " + result);
      document.getElementById("taskForm").reset();
    } else {
      const errorText = await response.text();
      customAlert("❌ Upload failed: " + errorText);
    }
  } catch (error) {
    customAlert("❌ Error uploading: " + error.message);
  } finally {
    document.getElementById("taskLoading").style.display = "none";
  }
}

function getTaskTableTemplate() {
  return `
    <h2>Task List</h2>
    <input type="text" id="taskSearch" class="search-box" placeholder="Search tasks..." onkeyup="filterTable('taskTable', 'taskSearch')" />
    <div id="taskTableLoading" style="display: none;">Loading tasks...</div>
    <table id="taskTable">
      <thead>
        <tr>
          <th>Title</th>
          <th>Created At</th>
          <th>Assigned To</th>
          <th>Action</th>
        </tr>
      </thead>
      <tbody></tbody>
    </table>
  `;
}

async function loadTasks() {
  const loading = document.getElementById("taskTableLoading");
  loading.style.display = "block";

  const companyId = parseInt(localStorage.getItem("companyId"), 10);
  const localUser = JSON.parse(localStorage.getItem("user") || "{}");
  const adminId = localUser.userId || localUser.id;

  if (!companyId || !adminId) {
    customAlert("❌ Company ID or Admin ID not found in localStorage.");
    loading.style.display = "none";
    return;
  }

  try {
    const response = await fetch(`/api/task-files/admin-all?adminId=${adminId}&companyId=${companyId}`, {
      method: 'GET',
      credentials: 'include'
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || "Failed to load admin tasks");
    }

    const tasks = await response.json();
    const tbody = document.querySelector("#taskTable tbody");
    tbody.innerHTML = "";

    tasks.forEach(task => {
      const row = document.createElement("tr");
      const isAssigned = task.assignedTo !== null;

      row.innerHTML = `
          <td>${task.title}</td>
          <td>${new Date(task.uploadDate).toLocaleString()}</td>
          <td>${isAssigned ? task.assignedTo.name : "Unassigned"}</td>
          <td>
            <button onclick="openExcelPreviewInNewTab(${task.id}, '${task.title}')">📄 Open</button>
            ${isAssigned
                  ? `<button onclick="unassignTask(${task.id})">🚫 Unassign</button>`
                  : `<button onclick="assignTask(${task.id})">👤 Assign</button>`
                }
            <button onclick="deleteTask(${task.id})">🗑️ Delete</button>
            <button onclick="downloadTaskFile(${task.id}, '${task.title}')">⬇️ Download</button>
          </td>
        `;
      tbody.appendChild(row);
    });
  } catch (error) {
    customAlert("❌ " + error.message);
  } finally {
    loading.style.display = "none";
  }
}


function downloadTaskFile(taskId, title) {
  const companyId = parseInt(localStorage.getItem("companyId"), 10);
  if (!companyId) {
    customAlert("Company ID not found");
    return;
  }

  fetch(`/api/task-files/${taskId}/download?companyId=${companyId}`, {
    method: 'GET',
    credentials: 'include'
  })
    .then(response => {
      if (!response.ok) {
        throw new Error("Download failed");
      }
      return response.blob();
    })
    .then(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = title.endsWith(".xlsx") ? title : `${title}.xlsx`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    })
    .catch(err => {
      console.error(err);
      customAlert("❌ Failed to download file.");
    });
}




// ✅ OPEN BUTTON — Open in new tab and render preview view
function openExcelPreviewInNewTab(taskId) {
  const companyId = localStorage.getItem("companyId");
  if (!companyId) {
    customAlert("❌ Company ID not found.");
    return;
  }

  const previewWindow = window.open("", "_blank");
  previewWindow.document.write("<title>Excel Editor</title><h2>Loading...</h2>");

  fetch(`/api/task-files/${taskId}/preview?companyId=${companyId}`, {
    method: "GET",
    credentials: "include"
  })
    .then(res => res.ok ? res.json() : res.text().then(msg => { throw new Error(msg); }))
    .then(data => loadPreview(previewWindow, data, taskId, companyId))
    .catch(err => {
      previewWindow.document.body.innerHTML = `<p style="color:red;">❌ ${err.message}</p>`;
    });
}

 function getColumnLetter(colIndex) {
        let letter = '';
        let tempIndex = colIndex;
        while (tempIndex >= 0) {
            letter = String.fromCharCode(65 + (tempIndex % 26)) + letter;
            tempIndex = Math.floor(tempIndex / 26) - 1;
        }
        return letter;
    }

    function loadPreview(previewWindow, data, taskId, companyId) {
        const doc = previewWindow.document;
        doc.title = "📄 Excel Editor";

        // Inject the styles into the head of the preview window
        doc.head.innerHTML = `
            <link href="[https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap](https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap)" rel="stylesheet">
            ${doc.head.innerHTML}
            <style>
                /* Styles defined above */
                html, body {
                    margin: 0;
                    padding: 0;
                    height: 100%;
                    font-family: 'Inter', sans-serif; /* Using Inter font */
                    background-color: #f8fafc; /* Tailwind gray-50 */
                    color: #1e293b; /* Tailwind slate-800 */
                    overflow: hidden;
                }
                #toolbar {
                    padding: 12px 16px;
                    background: #e2e8f0; /* Tailwind slate-200 */
                    position: sticky;
                    top: 0;
                    z-index: 10;
                    display: flex;
                    flex-wrap: wrap;
                    gap: 10px;
                    border-bottom: 1px solid #cbd5e1; /* Tailwind slate-300 */
                    box-shadow: 0 2px 4px rgba(0,0,0,0.05);
                }
                button {
                    padding: 10px 16px;
                    background-color: #3b82f6; /* Tailwind blue-500 */
                    border: none;
                    border-radius: 8px; /* More rounded corners */
                    color: white;
                    cursor: pointer;
                    font-weight: 600;
                    transition: background-color 0.2s ease, transform 0.1s ease;
                    box-shadow: 0 2px 5px rgba(0,0,0,0.1);
                }
                button:hover {
                    background-color: #2563eb; /* Tailwind blue-600 */
                    transform: translateY(-1px);
                }
                button:active {
                    transform: translateY(0);
                    box-shadow: 0 1px 3px rgba(0,0,0,0.1);
                }
                #scrollContainer {
                    overflow: auto; /* Enables both horizontal and vertical scrolling */
                    height: calc(100vh - 70px); /* Adjust height based on toolbar */
                    padding: 10px;
                }
                table {
                    border-collapse: collapse;
                    /* Remove fixed layout to allow dynamic column widths */
                    /* table-layout: fixed; */ /* Removed */
                    /* width: 100%; */ /* Removed to allow table to expand beyond container */
                    min-width: 100%; /* Ensure it takes at least 100% of container width */
                }
                th, td {
                    border: 1px solid #94a3b8; /* Tailwind slate-400 */
                    padding: 8px 12px;
                    text-align: center;
                    background-color: white;
                    color: #1e293b;
                    white-space: nowrap; /* Prevent text wrapping to allow natural width */
                    min-width: 80px; /* Minimum width for columns */
                    box-sizing: border-box; /* Include padding and border in element's total width */
                }
                th {
                    background-color: #f1f5f9; /* Tailwind slate-100 */
                    font-weight: 700;
                    position: sticky;
                    top: 0; /* Sticky header for vertical scroll */
                    z-index: 5; /* Ensure header is above cells when scrolling */
                }
                td[contenteditable="true"]:focus, th[contenteditable="true"]:focus {
                    outline: 2px solid #60a5fa; /* Tailwind blue-400 */
                    background-color: #eff6ff; /* Tailwind blue-50 */
                    box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.5); /* Soft focus ring */
                }
                /* Style for the S.No column (first column) */
                th:first-child, td:first-child {
                    position: sticky;
                    left: 0; /* Sticky for horizontal scroll */
                    z-index: 6; /* Ensure S.No is above other cells and header when scrolling */
                    background-color: #e2e8f0; /* Different background for sticky column */
                    border-right: 2px solid #64748b; /* Stronger border for separation */
                    min-width: 60px; /* Smaller min-width for S.No */
                }
                th:first-child {
                    z-index: 7; /* S.No header should be above all when both sticky */
                    background-color: #cbd5e1; /* Even darker background for S.No header */
                }
                .col-check {
                    margin-left: 5px;
                    transform: scale(1.2);
                }
            </style>
        `;

        let undoStack = [];
        let currentData = JSON.parse(JSON.stringify(data)); // Deep copy for client-side state

        // Function to render the table (useful for re-rendering after structural changes)
        function renderTable() {
            let html = `
                <div id="toolbar">
                    <button onclick="window.addNewRow()">➕ Add Row</button>
                    <button onclick="window.addNewColumn()">➕ Add Column</button>
                    <button onclick="window.deleteSelectedColumns()">🗑️ Delete Selected Columns</button>
                    <button onclick="window.undoLast()">↩️ Undo</button>
                </div>
                <div id="scrollContainer">
                    <table id="editableExcel">
            `;

            // Determine max columns for consistent rendering
            let maxCols = 0;
            if (currentData.length > 0) {
                maxCols = Math.max(...currentData.map(row => row.length));
            }

            // Header Row (S.No + Column Letters)
            html += "<tr>";
            html += `<th>S No</th>`; // Sticky S.No column header
            for (let colIndex = 0; colIndex < maxCols; colIndex++) {
                const colLetter = getColumnLetter(colIndex);
                const headerContent = currentData[0] && currentData[0][colIndex] ? currentData[0][colIndex] : colLetter;
                html += `<th contenteditable="true" onblur="window.updateCell(${taskId}, ${companyId}, 0, ${colIndex}, this.innerText)">
                            ${headerContent}<br><input type="checkbox" class="col-check" data-col="${colIndex}">
                        </th>`;
            }
            html += "</tr>";

            // Data Rows
            currentData.forEach((row, rowIndex) => {
                // Skip the first row if it's already used as header content
                if (rowIndex === 0 && currentData[0] && currentData[0].length > 0) return;

                html += "<tr>";
                html += `<td>${rowIndex + 1}</td>`; // S.No for data rows (1-indexed)

                for (let colIndex = 0; colIndex < maxCols; colIndex++) {
                    const safeCell = row[colIndex] ?? "";
                    html += `<td contenteditable="true"
                                onblur="window.updateCell(${taskId}, ${companyId}, ${rowIndex}, ${colIndex}, this.innerText)">
                                ${safeCell}
                            </td>`;
                }
                html += "</tr>";
            });

            html += `</table></div>`;
            doc.body.innerHTML = html;
        }

        // Initial render
        renderTable();

        const table = doc.getElementById("editableExcel");

        // === Update Cell ===
        previewWindow.updateCell = async function (taskId, companyId, row, col, newValue) {
            // Adjust col index for S.No column in HTML table
            const htmlCol = col + 1;
            const oldValue = table.rows[row + 1]?.cells[htmlCol]?.innerText; // +1 for S.No header row
            undoStack.push({ type: "update", row, col, oldValue, newValue }); // Store old and new for undo

            // Update client-side data immediately for responsiveness
            if (currentData[row]) {
                currentData[row][col] = newValue;
            } else {
                // Handle case where row might not exist yet in currentData (e.g., new row added)
                currentData[row] = Array(col + 1).fill("");
                currentData[row][col] = newValue;
            }

            try {
                const response = await fetch(`/api/task-files/${taskId}/update-cell?companyId=${companyId}&row=${row}&col=${col}&newValue=${encodeURIComponent(newValue)}`, {
                    method: "PATCH",
                    credentials: "include"
                });
                if (!response.ok) {
                    const errorText = await response.text();
                    console.error("❌ Backend update failed:", errorText);
                    // Revert UI if backend update fails
                    if (table.rows[row + 1]?.cells[htmlCol]) {
                        table.rows[row + 1].cells[htmlCol].innerText = oldValue;
                    }
                    // Remove the failed action from undoStack
                    undoStack.pop();
                }
            } catch (e) {
                console.error("❌ Network or unexpected error:", e.message);
                // Revert UI if network error
                if (table.rows[row + 1]?.cells[htmlCol]) {
                    table.rows[row + 1].cells[htmlCol].innerText = oldValue;
                }
                // Remove the failed action from undoStack
                undoStack.pop();
            }
        };

        // === Add Row ===
        previewWindow.addNewRow = async function () {
            const newRowIndex = currentData.length;
            const newRowData = Array(currentData[0] ? currentData[0].length : 5).fill(""); // Default 5 columns if no data
            currentData.push(newRowData);
            renderTable(); // Re-render the entire table

            // No backend call for add row, as cells are updated individually
            // This assumes row creation happens implicitly on cell update
            // If you have a backend API for adding rows, call it here
        };

        // === Add Column ===
        previewWindow.addNewColumn = async function () {
            const newColIndex = currentData.length > 0 ? Math.max(...currentData.map(row => row.length)) : 0;
            currentData.forEach(row => row.push("")); // Add empty cell to each existing row
            if (currentData.length === 0) { // If table was empty, add a default row
                currentData.push([""]);
            }
            renderTable(); // Re-render the entire table

            // No backend call for add column, as cells are updated individually
            // This assumes column creation happens implicitly on cell update
            // If you have a backend API for adding columns, call it here
        };

        // === Delete Columns ===
        previewWindow.deleteSelectedColumns = async function () {
            const checkboxes = doc.querySelectorAll(".col-check:checked");
            const indexesToDelete = Array.from(checkboxes).map(cb => parseInt(cb.dataset.col)).sort((a, b) => b - a); // Sort descending

            if (indexesToDelete.length === 0) {
                alert("Please select columns to delete.");
                return;
            }

            // Store current state for undo
            const deletedColumnData = [];
            indexesToDelete.forEach(colIndex => {
                const columnContent = currentData.map(row => row[colIndex] || "");
                deletedColumnData.push({ colIndex, content: columnContent });
            });
            undoStack.push({ type: "deleteCols", data: deletedColumnData });

            // Update client-side data
            indexesToDelete.forEach(colIndex => {
                currentData.forEach(row => {
                    if (colIndex < row.length) {
                        row.splice(colIndex, 1); // Remove the column data
                    }
                });
            });
            renderTable(); // Re-render the entire table

            // Call backend for each deleted column
            for (const colIndex of indexesToDelete) {
                try {
                    const response = await fetch(`/api/task-files/${taskId}/delete-column?companyId=${companyId}&colIndex=${colIndex}`, {
                        method: "DELETE", // Use DELETE verb as per backend
                        credentials: "include"
                    });
                    if (!response.ok) {
                        const errorText = await response.text();
                        console.error(`❌ Backend delete column ${colIndex} failed:`, errorText);
                        // TODO: Implement more robust error handling / partial revert if backend fails for some columns
                    }
                } catch (e) {
                    console.error(`❌ Network or unexpected error deleting column ${colIndex}:`, e.message);
                }
            }
        };

        // === Undo ===
        previewWindow.undoLast = function () {
            const last = undoStack.pop();
            if (!last) return;

            if (last.type === "update") {
                // Revert cell content
                currentData[last.row][last.col] = last.oldValue;
                renderTable(); // Re-render to show change
            } else if (last.type === "deleteCols") {
                // Re-insert columns in reverse order of deletion (ascending colIndex)
                last.data.sort((a, b) => a.colIndex - b.colIndex).forEach(deletedCol => {
                    const colIndexToRestore = deletedCol.colIndex;
                    currentData.forEach((row, rowIndex) => {
                        // Ensure row has enough elements to insert at colIndexToRestore
                        while (row.length < colIndexToRestore) {
                            row.push("");
                        }
                        row.splice(colIndexToRestore, 0, deletedCol.content[rowIndex]);
                    });
                });
                renderTable(); // Re-render the entire table
            }
        };
    }




async function deleteTask(taskId) {
  if (!confirm("Are you sure you want to delete this task?")) return;

  const companyId = localStorage.getItem("companyId");

  if (!companyId) {
    customAlert("❌ Company ID not found.");
    return;
  }

  try {
    const response = await fetch(`/api/task-files/${taskId}?companyId=${companyId}`, {
      method: 'DELETE',
      credentials: 'include'
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || "Failed to delete task");
    }

    customAlert("✅ Task deleted successfully!");
    await loadTasks();
  } catch (error) {
    customAlert("❌ " + error.message);
  }
}


async function assignTask(taskId) {
  try {
    const companyId = parseInt(localStorage.getItem("companyId"), 10);
    if (!companyId) {
      customAlert("Company ID not found in localStorage!");
      return;
    }

    const response = await fetch(`/api/users/company/${companyId}`, {
      method: 'GET',
      credentials: 'include'
    });

    if (!response.ok) throw new Error("Failed to fetch users");

    const users = await response.json();
    const employeeUsers = users.filter(u => u.role === 'USER');

    if (employeeUsers.length === 0) {
      customAlert("No users available for assignment.");
      return;
    }

    const modal = document.createElement("div");
    modal.className = "modal-overlay";
    modal.innerHTML = `
      <div class="modal-content1">
        <span class="close-btn" onclick="this.parentElement.parentElement.remove()">×</span>
        <h3>Assign Task #${taskId}</h3>
        <div class="user-list">
          ${employeeUsers.map(user => `
            <div class="user-item" onclick="assignTaskToUser(${taskId}, ${user.userId}, '${user.name}', this)">
              👤 ${user.name}
            </div>
          `).join("")}
        </div>
      </div>
    `;

    modal.onclick = (e) => e.target === modal && modal.remove();
    document.body.appendChild(modal);
  } catch (error) {
    customAlert(`❌ ${error.message}`);
  }
}



async function assignTaskToUser(taskId, userId, userName, el) {
  const companyId = localStorage.getItem("companyId");
  if (!companyId) {
    customAlert("Company ID missing.");
    return;
  }

  el.style.opacity = "0.5";

  try {
    const response = await fetch(`/api/task-files/${taskId}/assign?userId=${userId}&companyId=${companyId}`, {
      method: 'PUT',
      credentials: 'include'
    });

    if (!response.ok) {
      const err = await response.text();
      throw new Error(err || "Assignment failed.");
    }

    customAlert(`✅ Assigned to ${userName}`);
    document.querySelector('.modal-overlay')?.remove();
    await loadTasks();
  } catch (err) {
    customAlert(`❌ ${err.message}`);
  }
}


async function unassignTask(taskId) {
  const companyId = localStorage.getItem("companyId");
  if (!companyId) {
    customAlert("❌ Company ID not found.");
    return;
  }

  if (!confirm("Are you sure you want to unassign this task?")) return;

  try {
    const response = await fetch(`/api/task-files/${taskId}/assign?userId=&companyId=${companyId}`, {
      method: 'PUT',
      credentials: 'include'
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || "Failed to unassign task");
    }

    customAlert("✅ Task unassigned successfully!");
    await loadTasks();
  } catch (error) {
    customAlert("❌ " + error.message);
  }
}
