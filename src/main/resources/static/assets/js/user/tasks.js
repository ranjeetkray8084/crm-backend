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
          <th>Action</th>
        </tr>
      </thead>
      <tbody></tbody>
    </table>
  `;
}


async function loadTasksForUser() {
  const loading = document.getElementById("taskTableLoading");
  loading.style.display = "block";

  const companyId = parseInt(localStorage.getItem("companyId"), 10);
  const user = JSON.parse(localStorage.getItem("user") || "{}");
  const userId = user.userId || user.id;

  if (!companyId || !userId) {
    customAlert("❌ Missing company or user ID in localStorage.");
    loading.style.display = "none";
    return;
  }

  try {
    const response = await fetch(`/api/task-files/assigned?companyId=${companyId}&userId=${userId}`, {
      method: "GET",
      credentials: "include"
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || "Failed to load assigned tasks.");
    }

    const tasks = await response.json();
    const tbody = document.querySelector("#taskTable tbody");
    tbody.innerHTML = "";

    if (tasks.length === 0) {
      tbody.innerHTML = "<tr><td colspan='3'>No assigned tasks found.</td></tr>";
      return;
    }

    tasks.forEach(task => {
      const row = document.createElement("tr");
      row.innerHTML = `
        <td>${task.title}</td>
        <td>${new Date(task.uploadDate).toLocaleString()}</td>
        <td>
          <button onclick="openExcelPreviewInNewTab(${task.id}, '${task.title}')">📄 Open</button>
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
  fetch(`/api/task-files/download/${taskId}`, {
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
      const a = document.createElement('a');
      a.href = url;
      a.download = `${title}.xlsx`; // adjust extension as needed
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    })
    .catch(err => {
      console.error(err);
      customAlert("❌ Download failed");
    });
}


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

function loadPreview(previewWindow, data, taskId, companyId) {
  const doc = previewWindow.document;
  doc.title = "📄 Excel Editor";

  doc.head.innerHTML = `
    <style>
      html, body {
        margin: 0;
        padding: 0;
        height: 100%;
        font-family: sans-serif;
        background-color: white;
        color: black;
        overflow: hidden;
      }
      #toolbar {
        padding: 10px;
        background: #f0f0f0;
        position: sticky;
        top: 0;
        z-index: 10;
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        border-bottom: 1px solid #ccc;
      }
      button {
        padding: 8px 12px;
        background-color: #4a90e2;
        border: none;
        border-radius: 4px;
        color: white;
        cursor: pointer;
      }
      button:hover {
        background-color: #357ae8;
      }
      #scrollContainer {
        overflow: auto;
        height: calc(100vh - 80px);
        padding: 10px;
      }
      table {
        width: 100%;
        border-collapse: collapse;
        table-layout: fixed;
      }
      th, td {
        border: 1px solid black;
        padding: 6px;
        text-align: center;
        background-color: white;
        color: black;
        word-break: break-word;
      }
      td[contenteditable="true"]:focus, th[contenteditable="true"]:focus {
        outline: 2px solid #4a90e2;
        background-color: #e0f0ff;
      }
    </style>
  `;

  let undoStack = [];

  let html = `
    <div id="toolbar">
      <button onclick="window.addNewRow(${taskId}, ${companyId})">➕ Add Row</button>
      <button onclick="window.addNewColumn(${taskId}, ${companyId})">➕ Add Column</button>
      <button onclick="window.deleteSelectedColumns()">🗑️ Delete Selected Columns</button>
      <button onclick="window.undoLast()">↩️ Undo</button>
    </div>
    <div id="scrollContainer">
      <table id="editableExcel">
  `;

  data.forEach((row, rowIndex) => {
    html += "<tr>";
    html += rowIndex === 0 ? `<th>S No</th>` : `<td>${rowIndex}</td>`;

    row.forEach((cell, colIndex) => {
      const safeCell = cell ?? "";
      if (rowIndex === 0) {
        html += `<th contenteditable="true" onblur="window.updateCell(${taskId}, ${companyId}, 0, ${colIndex}, this.innerText)">
                  ${safeCell}<br><input type="checkbox" class="col-check" data-col="${colIndex}">
                </th>`;
      } else {
        html += `<td contenteditable="true"
                   onblur="window.updateCell(${taskId}, ${companyId}, ${rowIndex}, ${colIndex}, this.innerText)">
                   ${safeCell}
               </td>`;
      }
    });

    html += "</tr>";
  });

  html += `</table></div>`;
  doc.body.innerHTML = html;

  const table = doc.getElementById("editableExcel");

  // === Update Cell ===
  previewWindow.updateCell = async function (taskId, companyId, row, col, newValue) {
    const oldValue = table.rows[row]?.cells[col + 1]?.innerText;
    undoStack.push({ row, col, oldValue });

    try {
      await fetch(`/api/task-files/${taskId}/update-cell?companyId=${companyId}&row=${row}&col=${col}&newValue=${encodeURIComponent(newValue)}`, {
        method: "PATCH",
        credentials: "include"
      });
    } catch (e) {
      console.error("❌ Update failed:", e.message);
    }
  };

  // === Add Row ===
  previewWindow.addNewRow = function () {
    const colCount = table.rows[0].cells.length - 1;
    const rowIndex = table.rows.length;
    const newRow = table.insertRow(-1);
    newRow.insertCell().innerText = rowIndex;

    for (let col = 0; col < colCount; col++) {
      const cell = newRow.insertCell();
      cell.contentEditable = "true";
      cell.innerText = "";
      cell.onblur = function () {
        previewWindow.updateCell(taskId, companyId, rowIndex, col, this.innerText);
      };
    }
  };

  // === Add Column ===
  previewWindow.addNewColumn = function () {
    const rowCount = table.rows.length;
    const colIndex = table.rows[0].cells.length - 1;

    for (let rowIndex = 0; rowIndex < rowCount; rowIndex++) {
      const row = table.rows[rowIndex];
      const cell = row.insertCell();

      if (rowIndex === 0) {
        cell.outerHTML = `<th contenteditable="true"
                            onblur="window.updateCell(${taskId}, ${companyId}, 0, ${colIndex}, this.innerText)">
                            New Col ${colIndex + 1}<br>
                            <input type="checkbox" class="col-check" data-col="${colIndex}">
                          </th>`;
      } else {
        cell.contentEditable = "true";
        cell.innerText = "";
        cell.onblur = function () {
          previewWindow.updateCell(taskId, companyId, rowIndex, colIndex, this.innerText);
        };
      }
    }
  };

  // === Delete Columns ===
  previewWindow.deleteSelectedColumns = function () {
    const checkboxes = doc.querySelectorAll(".col-check:checked");
    const indexes = Array.from(checkboxes).map(cb => parseInt(cb.dataset.col)).sort((a, b) => b - a);

    indexes.forEach(colIndex => {
      for (let i = 0; i < table.rows.length; i++) {
        undoStack.push({ type: "deleteCol", row: i, col: colIndex, value: table.rows[i].cells[colIndex + 1]?.innerText });
        table.rows[i].deleteCell(colIndex + 1); // +1 for S No.
      }
    });
  };

  // === Undo ===
  previewWindow.undoLast = function () {
    const last = undoStack.pop();
    if (!last) return;

    if (last.type === "deleteCol") {
      const row = table.rows[last.row];
      const cell = row.insertCell(last.col + 1);
      cell.innerText = last.value || "";
      cell.contentEditable = "true";
    } else if ("row" in last && "col" in last) {
      table.rows[last.row].cells[last.col + 1].innerText = last.oldValue;
    }
  };
}


