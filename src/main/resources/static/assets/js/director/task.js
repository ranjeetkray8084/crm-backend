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

  if (!title || !fileInput.files.length || !uploadedBy || isNaN(companyId)) {
    customAlert("❌ All required fields must be filled.");
    return;
  }

  const formData = new FormData();
  formData.append("title", title.endsWith(".xlsx") ? title : `${title}.xlsx`);
  formData.append("file", fileInput.files[0]);
  formData.append("companyId", companyId);
  formData.append("uploadedBy", uploadedBy); // ✅ Pass only userId

  // Show loading spinner or message
  document.getElementById("taskLoading").style.display = "block";

  try {
    const response = await fetch("/api/task-files/upload", {
      method: "POST",
      body: formData,
      credentials: 'include'
    });

    const result = await response.text();
    
    if (response.ok) {
      customAlert("✅ " + result);
      document.getElementById("taskForm").reset();
    } else {
      customAlert("❌ Upload failed: " + result);
    }
  } catch (error) {
    console.error("Upload error:", error);
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
          <th>Created By</th>
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
  if (!companyId) {
    customAlert("❌ Company ID not found in localStorage.");
    loading.style.display = "none";
    return;
  }

  try {
    const response = await fetch(`/api/task-files?companyId=${companyId}`, {
      method: 'GET',
      credentials: 'include'
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || "Failed to load tasks");
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
        <td>${task.uploadedByName}</td>
        <td>
          <button onclick="openExcelPreviewInNewTab(${task.id}, '${task.title}')">📄 Open</button>
          ${
            isAssigned
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

    // Main function to load and manage the Excel preview/editor
    function loadPreview(previewWindow, data, taskId, companyId) {
        const doc = previewWindow.document;
        doc.title = "📄 Excel Editor";

        // Inject the styles into the head of the preview window
        // Ensure <link> tags are direct children of <head>, not inside <style>
        doc.head.innerHTML = `
            <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap" rel="stylesheet">
            <style>
                /* Base styles for the entire page */
                html, body {
                    margin: 0;
                    padding: 0;
                    height: 100%;
                    font-family: 'Inter', sans-serif; /* Using Inter font */
                    background-color: #f8fafc; /* Tailwind gray-50 */
                    color: #1e293b; /* Tailwind slate-800 */
                    overflow: hidden; /* Prevent main body scroll, #scrollContainer manages content overflow */
                }

                /* Toolbar for actions like Add Row, Add Column, Delete Column, Undo */
                #toolbar {
                    padding: 12px 16px;
                    background: #e2e8f0; /* Tailwind slate-200 */
                    position: sticky; /* Makes toolbar stick to the top on vertical scroll */
                    top: 0;
                    z-index: 10; /* Ensures toolbar is above other content */
                    display: flex;
                    flex-wrap: wrap; /* Allows buttons to wrap on smaller screens */
                    gap: 10px; /* Spacing between buttons */
                    border-bottom: 1px solid #cbd5e1; /* Tailwind slate-300 */
                    box-shadow: 0 2px 4px rgba(0,0,0,0.05); /* Subtle shadow for depth */
                }

                /* Styling for toolbar buttons */
                button {
                    padding: 10px 16px;
                    background-color: #3b82f6; /* Tailwind blue-500 */
                    border: none;
                    border-radius: 8px; /* Rounded corners for buttons */
                    color: white;
                    cursor: pointer;
                    font-weight: 600;
                    transition: background-color 0.2s ease, transform 0.1s ease; /* Smooth transitions for hover/active */
                    box-shadow: 0 2px 5px rgba(0,0,0,0.1); /* Soft shadow */
                }
                button:hover {
                    background-color: #2563eb; /* Darker blue on hover */
                    transform: translateY(-1px); /* Slight lift effect */
                }
                button:active {
                    transform: translateY(0); /* Press down effect */
                    box-shadow: 0 1px 3px rgba(0,0,0,0.1);
                }

                /* Container for the Excel table, enabling scrolling */
                #scrollContainer {
                    overflow: auto; /* Enables both horizontal and vertical scrollbars when content overflows */
                    height: calc(100vh - 70px); /* Adjust height to fill remaining viewport space below toolbar */
                    padding: 10px; /* Padding around the table */
                }

                /* Table specific styles */
                table {
                    border-collapse: collapse; /* Collapses borders for a cleaner look */
                    /* table-layout: auto; is default. It allows columns to size based on content. */
                    /* min-width ensures the table always takes at least the container's width */
                    min-width: 100%;
                }

                /* Styles for table header and data cells */
                th, td {
                    border: 1px solid #94a3b8; /* Tailwind slate-400 border */
                    padding: 8px 12px; /* Padding inside cells */
                    text-align: center;
                    background-color: white;
                    color: #1e293b;
                    white-space: nowrap; /* Prevents text from wrapping, forcing column to expand to content width */
                    min-width: 80px; /* Minimum width for all data/header columns */
                    box-sizing: border-box; /* Includes padding and border in the element's total width */
                }

                /* Header specific styles */
                th {
                    background-color: #f1f5f9; /* Tailwind slate-100 */
                    font-weight: 700;
                    position: sticky;
                    top: 0; /* Makes the header row sticky for vertical scrolling */
                    z-index: 5; /* Ensures header is above data cells when scrolling */
                }

                /* Focus styles for editable cells */
                td[contenteditable="true"]:focus, th[contenteditable="true"]:focus {
                    outline: 2px solid #60a5fa; /* Tailwind blue-400 outline */
                    background-color: #eff6ff; /* Tailwind blue-50 background */
                    box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.5); /* Soft focus ring effect */
                }

                /* Style for the S.No column (first column) to make it sticky on horizontal scroll */
                th:first-child, td:first-child {
                    position: sticky;
                    left: 0; /* Makes the S.No column sticky for horizontal scrolling */
                    z-index: 6; /* Ensures S.No column is above other cells and header when scrolling */
                    background-color: #e2e8f0; /* Different background for sticky column */
                    border-right: 2px solid #64748b; /* Stronger border for visual separation */
                    min-width: 60px; /* Slightly smaller minimum width for the S.No column */
                }
                th:first-child {
                    z-index: 7; /* S.No header should be above all when both header and S.No column are sticky */
                    background-color: #cbd5e1; /* Even darker background for S.No header */
                }

                /* Styling for column selection checkboxes */
                .col-check {
                    margin-left: 5px;
                    transform: scale(1.2); /* Slightly larger checkbox */
                }
            </style>
        `;

        let undoStack = [];
        // Deep copy the initial data to manage client-side state independently
        let currentData = JSON.parse(JSON.stringify(data)); 

        // Function to render or re-render the entire table based on currentData
        function renderTable() {
            let html = `
                <div id="toolbar">
                    <button onclick="window.addNewRow()">➕ Add Row</button>
                    <button onclick="window.addNewColumn()">➕ Add Column</button>
                    <button onclick="window.deleteSelectedColumns()">🗑️ Delete Selected Columns</button>
                    <button onclick="window.undoLast()">↩️ Undo</button>
                    <button onclick="window.scrollTableLeft()">⬅️ Scroll Left</button>
                    <button onclick="window.scrollTableRight()">➡️ Scroll Right</button>
                </div>
                <div id="scrollContainer">
                    <table id="editableExcel">
            `;

            // Determine maximum columns for consistent rendering across all rows
            let maxCols = 0;
            if (currentData.length > 0) {
                maxCols = Math.max(...currentData.map(row => row.length));
            }

            // --- Header Row (S.No + Column Letters/Content) ---
            html += "<tr>";
            html += `<th>S No</th>`; // Sticky S.No column header
            for (let colIndex = 0; colIndex < maxCols; colIndex++) {
                const colLetter = getColumnLetter(colIndex);
                // Use actual data from the first row as header content if available, fallback to column letter
                const headerContent = currentData[0] && currentData[0][colIndex] ? currentData[0][colIndex] : colLetter;
                html += `<th contenteditable="true" onblur="window.updateCell(${taskId}, ${companyId}, 0, ${colIndex}, this.innerText)">
                            ${headerContent}<br><input type="checkbox" class="col-check" data-col="${colIndex}">
                        </th>`;
            }
            html += "</tr>";

            // --- Data Rows ---
            currentData.forEach((row, rowIndex) => {
                // Skip the first row if its content is used as the header (to avoid duplicate header content)
                // This assumes the first row of 'data' is intended to be the header if it has content.
                let isHeaderRowPopulated = currentData[0] && currentData[0].some(cellContent => cellContent !== null && cellContent !== '');
                if (rowIndex === 0 && isHeaderRowPopulated) {
                    return; // Skip rendering this row as a data row if it's acting as the header
                }

                html += "<tr>";
                // S.No for data rows (1-indexed). Adjust for header row if it was skipped.
                html += `<td>${isHeaderRowPopulated ? rowIndex : rowIndex + 1}</td>`;

                for (let colIndex = 0; colIndex < maxCols; colIndex++) {
                    const safeCell = row[colIndex] ?? ""; // Use empty string for null/undefined cells
                    html += `<td contenteditable="true"
                                onblur="window.updateCell(${taskId}, ${companyId}, ${rowIndex}, ${colIndex}, this.innerText)">
                                ${safeCell}
                            </td>`;
                }
                html += "</tr>";
            });

            html += `</table></div>`;
            doc.body.innerHTML = html; // Inject generated HTML into the preview window's body
        }

        // Initial render of the table when the preview is loaded
        renderTable();

        // Get a reference to the table element after it's rendered
        const table = doc.getElementById("editableExcel");
        const scrollContainer = doc.getElementById("scrollContainer"); // Get scroll container reference

        // === Scroll Left Functionality ===
        previewWindow.scrollTableLeft = function() {
            scrollContainer.scrollBy({
                left: -200, // Scroll 200px to the left
                behavior: 'smooth' // Smooth scrolling
            });
        };

        // === Scroll Right Functionality ===
        previewWindow.scrollTableRight = function() {
            scrollContainer.scrollBy({
                left: 200, // Scroll 200px to the right
                behavior: 'smooth' // Smooth scrolling
            });
        };

        // === Update Cell Functionality ===
        previewWindow.updateCell = async function (taskId, companyId, row, col, newValue) {
            // Determine the correct HTML table row index (0 for header, then 1-indexed for data rows)
            const isHeaderRowPopulated = currentData[0] && currentData[0].some(cellContent => cellContent !== null && cellContent !== '');
            const htmlTableRowIndex = (row === 0 && isHeaderRowPopulated) ? 0 : row + 1;
            const htmlTableCellIndex = col + 1; // +1 because of the S.No column in HTML

            const oldValue = table.rows[htmlTableRowIndex]?.cells[htmlTableCellIndex]?.innerText;
            
            // Push to undo stack BEFORE making changes to clientData
            undoStack.push({ type: "update", row, col, oldValue, newValue });

            // Update client-side data immediately for responsiveness
            if (!currentData[row]) {
                // If row doesn't exist yet (e.g., updating a cell in a newly added row)
                // Create the row with enough columns, padding with empty strings
                const newRowLength = Math.max(col + 1, currentData[0] ? currentData[0].length : 0);
                currentData[row] = Array(newRowLength).fill("");
            } else {
                // Ensure the row has enough columns for the update
                while (currentData[row].length <= col) {
                    currentData[row].push(""); // Pad with empty strings if necessary
                }
            }
            currentData[row][col] = newValue; // Apply the new value to client-side data

            try {
                const response = await fetch(`/api/task-files/${taskId}/update-cell?companyId=${companyId}&row=${row}&col=${col}&newValue=${encodeURIComponent(newValue)}`, {
                    method: "PATCH",
                    credentials: "include"
                });
                if (!response.ok) {
                    const errorText = await response.text();
                    console.error("❌ Backend update failed:", errorText);
                    // Revert UI if backend update fails
                    if (table.rows[htmlTableRowIndex]?.cells[htmlTableCellIndex]) {
                        table.rows[htmlTableRowIndex].cells[htmlTableCellIndex].innerText = oldValue;
                    }
                    // Remove the failed action from undoStack
                    undoStack.pop(); // Remove the failed action from undo history
                }
            } catch (e) {
                console.error("❌ Network or unexpected error:", e.message);
                // Revert UI if network error
                if (table.rows[htmlTableRowIndex]?.cells[htmlTableCellIndex]) {
                    table.rows[htmlTableRowIndex].cells[htmlTableCellIndex].innerText = oldValue;
                }
                // Remove the failed action from undoStack
                undoStack.pop(); // Remove the failed action from undo
            }
        };

        // === Add Row Functionality ===
        previewWindow.addNewRow = async function () {
            // Determine max columns for the new row to match existing data width
            const colCount = currentData.length > 0 ? Math.max(...currentData.map(row => row.length)) : 5; // Default 5 columns if table empty
            const newRowIndex = currentData.length;
            const newRowData = Array(colCount).fill("");
            currentData.push(newRowData); // Add new row to client-side data
            renderTable(); // Re-render the entire table to show the new row
            // No direct backend call for adding a row as cells are updated individually.
            // If you need a specific backend endpoint for adding blank rows, call it here.
        };

        // === Add Column Functionality ===
        previewWindow.addNewColumn = async function () {
            const newColIndex = currentData.length > 0 ? Math.max(...currentData.map(row => row.length)) : 0;
            
            // Add a new empty column to all existing rows
            currentData.forEach(row => row.push(""));
            
            // If the table was empty, add a default row to hold the new column
            if (currentData.length === 0) {
                currentData.push([""]); // Start with one row, one empty column
            }

            renderTable(); // Re-render the entire table to show the new column
            // No direct backend call for adding a column as cells are updated individually.
            // If you need a specific backend endpoint for adding blank columns, call it here.
        };

        // === Delete Columns Functionality ===
        previewWindow.deleteSelectedColumns = async function () {
            const checkboxes = doc.querySelectorAll(".col-check:checked");
            // Get selected column indexes and sort them in descending order to avoid index issues during deletion
            const indexesToDelete = Array.from(checkboxes).map(cb => parseInt(cb.dataset.col)).sort((a, b) => b - a);

            if (indexesToDelete.length === 0) {
                alert("Please select columns to delete.");
                return;
            }

            // Store current state for undo: content of deleted columns
            const deletedColumnData = [];
            indexesToDelete.forEach(colIndex => {
                const columnContent = currentData.map(row => row[colIndex] || "");
                deletedColumnData.push({ colIndex, content: columnContent });
            });
            undoStack.push({ type: "deleteCols", data: deletedColumnData });

            // Update client-side data: remove columns
            indexesToDelete.forEach(colIndex => {
                currentData.forEach(row => {
                    if (colIndex < row.length) {
                        row.splice(colIndex, 1); // Remove the column data from each row
                    }
                });
            });
            renderTable(); // Re-render the entire table to reflect deletion

            // Call backend for each deleted column (assuming backend handles deletion one by one)
            for (const colIndex of indexesToDelete) {
                try {
                    const response = await fetch(`/api/task-files/${taskId}/delete-column?companyId=${companyId}&colIndex=${colIndex}`, {
                        method: "DELETE",
                        credentials: "include"
                    });
                    if (!response.ok) {
                        const errorText = await response.text();
                        console.error(`❌ Backend delete column ${colIndex} failed:`, errorText);
                        // TODO: Implement more robust error handling if backend fails for some columns
                        // For example, you might need to re-add the column data on the client if backend fails.
                    }
                } catch (e) {
                    console.error(`❌ Network or unexpected error deleting column ${colIndex}:`, e.message);
                }
            }
        };

        // === Undo Functionality ===
        previewWindow.undoLast = function () {
            const last = undoStack.pop();
            if (!last) return; // Nothing to undo

            if (last.type === "update") {
                // Revert cell content on client side
                if (currentData[last.row] && currentData[last.row][last.col] !== undefined) {
                    currentData[last.row][last.col] = last.oldValue;
                }
                renderTable(); // Re-render to show reverted change
            } else if (last.type === "deleteCols") {
                // Re-insert columns in their original positions (sort ascending by index)
                last.data.sort((a, b) => a.colIndex - b.colIndex).forEach(deletedCol => {
                    const colIndexToRestore = deletedCol.colIndex;
                    currentData.forEach((row, rowIndex) => {
                        // Ensure the row exists and has enough capacity to insert the column
                        while (row.length < colIndexToRestore) {
                            row.push(""); // Pad with empty strings if necessary
                        }
                        row.splice(colIndexToRestore, 0, deletedCol.content[rowIndex]); // Insert the old content
                    });
                });
                renderTable(); // Re-render the entire table after restoring columns
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
