function getAddLeadForm() {
  return `
 <div class="addform">
  <h2>Add Lead</h2>
  <div style="max-height: 500px; overflow-y: auto; border: 1px solid #ccc; padding: 15px;">
  <form id="add-lead-form" onsubmit="event.preventDefault(); saveLead()">
    <label for="leadName">Name:</label>
    <input type="text" id="leadName" name="leadName" required /><br />

    <label for="email">Email:</label>
    <input type="email" id="email" name="email" /><br />

    <label for="phone">Phone:</label>
    <input type="tel" id="phone" name="phone" required
           pattern="^[+]?[0-9]{10,15}$"
           title="Phone number must be 10 to 15 digits" /><br />

    <label for="source">Source:</label>
    <select id="source" name="source" required onchange="toggleReferenceNameField()">
      <option value="INSTAGRAM">INSTAGRAM</option>
      <option value="FACEBOOK">FACEBOOK</option>
      <option value="YOUTUBE">YOUTUBE</option>
      <option value="Reference">Reference</option>
    </select><br />

    <div id="referenceNameField" style="display: none;">
      <label for="referenceName">Enter Name (Reference):</label>
      <input type="text" id="referenceName" name="referenceName" placeholder="Enter reference name" />
    </div><br />

    <label for="status">Status:</label>
    <select id="status" name="status" required>
      <option value="">Select Status</option>
      <option value="NEW">New</option>
      <option value="CONTACTED">Contacted</option>
      <option value="CLOSED">Dropped</option>
    </select><br />

    <label for="budget">Budget (₹):</label>
<input id="budget" name="budget" type="number" min="0" step="1" required /><br />


    <label for="location">Location:</label>
    <input id="location" name="location" /><br />

    <label>Requirement:</label><br />
      <div style="display: flex; flex-wrap: wrap; gap: 20px; align-items: center; margin-bottom: 10px;">

      <label style="display: flex; align-items: center; gap: 5px;">
        Commercial <input type="checkbox" class="req-checkbox" value="Commercial" />
      </label>

      <label style="display: flex; align-items: center; gap: 5px;">
        Residential <input type="checkbox" class="req-checkbox" value="Residential" />
      </label>

      <label id="rent-label" style="display: flex; align-items: center; gap: 5px;">
        Rent <input type="checkbox" class="req-checkbox" value="Rent" />
      </label>

      <label style="display: flex; align-items: center; gap: 5px;">
        Purchase <input type="checkbox" class="req-checkbox" value="Purchase" />
      </label>

      <label id="lease-label" style="display: flex; align-items: center; gap: 5px;">
        Lease <input type="checkbox" class="req-checkbox" value="Lease" />


      </label>
        <label id="plot-label" style="display:none;align-items:center;gap:5px;">
        Plot <input type="checkbox" class="req-checkbox" value="Plot" />
      </label>


    </div>

    <input type="text" id="customRequirement" name="customRequirement" placeholder="Type custom requirement" /><br />

    <textarea id="requirement" name="requirement" style="display:none;"></textarea>

  

    <button type="submit">Save Lead</button>
    <div id="loading" style="display:none;">Saving lead...</div>
  </form>
</div>
</div>
  `;
}

// This function should be called after the form is rendered in DOM
function setupRequirementCheckboxes() {
  // main checkboxes
  const commercialCheckbox = document.querySelector('input[value="Commercial"]');
  const residentialCheckbox = document.querySelector('input[value="Residential"]');

  // secondary option labels
  const rentLabel = document.getElementById('rent-label');
  const leaseLabel = document.getElementById('lease-label');
  const purchaseLabel = document.querySelector('input[value="Purchase"]').parentElement;
  const plotLabel = document.getElementById('plot-label');          // NEW

  function updateVisibility() {
    const isCommercial = commercialCheckbox.checked;
    const isResidential = residentialCheckbox.checked;
    const anyPrimary = isCommercial || isResidential;                // NEW

    // toggle the mutually‑exclusive primary choices
    if (isCommercial) {
      residentialCheckbox.checked = false;
      residentialCheckbox.disabled = true;

      rentLabel.style.display = 'none';
      leaseLabel.style.display = 'flex';
    } else if (isResidential) {
      commercialCheckbox.checked = false;
      commercialCheckbox.disabled = true;

      rentLabel.style.display = 'flex';
      leaseLabel.style.display = 'none';
    } else {
      // re‑enable if none selected
      commercialCheckbox.disabled = false;
      residentialCheckbox.disabled = false;

      rentLabel.style.display = 'none';
      leaseLabel.style.display = 'none';
    }

    // Purchase always visible when any primary selected
    purchaseLabel.style.display = anyPrimary ? 'flex' : 'none';

    // Plot shown whenever ANY primary selected               // NEW
    plotLabel.style.display = anyPrimary ? 'flex' : 'none';
  }

  commercialCheckbox.addEventListener('change', updateVisibility);
  residentialCheckbox.addEventListener('change', updateVisibility);

  updateVisibility(); // initial call
}



function updateRequirementField() {
  const checkboxes = document.querySelectorAll('.req-checkbox:checked');
  const customInput = document.getElementById('customRequirement').value.trim();

  const selectedValues = Array.from(checkboxes).map(cb => cb.value);

  if (customInput) {
    selectedValues.push(customInput);
  }

  document.getElementById('requirement').value = selectedValues.join(', ');
}

function updateRequirementField() {
  const selected = [];
  document.querySelectorAll('.req-checkbox:checked').forEach(cb => selected.push(cb.value));

  const custom = document.getElementById('customRequirement').value.trim();
  if (custom) selected.push(custom);

  document.getElementById('requirement').value = selected.join(', ');
}

document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.req-checkbox').forEach(cb => {
    cb.addEventListener('change', updateRequirementField);
  });
  document.getElementById('customRequirement').addEventListener('input', updateRequirementField);
  document.getElementById('source').addEventListener('change', toggleReferenceNameField);
});

async function saveLead() {
  const loadingDiv = document.getElementById('loading');
  loadingDiv.style.display = 'block';

  try {
    const localUser = JSON.parse(localStorage.getItem("user") || "{}");
    const userId = localUser.userId || localUser.id; // fallback if key is `id`
    const companyId = parseInt(localStorage.getItem("companyId"), 10);



    updateRequirementField();

    const leadData = {
      name: document.getElementById('leadName').value.trim(),
      email: document.getElementById('email').value.trim() || null,
      phone: document.getElementById('phone').value.trim(),
      source: document.getElementById('source').value,
      status: document.getElementById('status').value,
      budget: document.getElementById('budget').value.trim() || null,
      requirement: document.getElementById('requirement').value.trim() || null,
      createdBy: { userId: userId },
      location:document.getElementById('location').value.trim() || null,
      // company: { id: companyId } // <- ✅ correctly formatted company object
    };

    if (leadData.source === 'Reference') {
      const refName = document.getElementById('referenceName').value.trim();
      if (refName) {
        leadData.referenceName = refName;
      }
    }

    const leadResponse = await fetch(`/api/companies/${companyId}/leads`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(leadData),
    });

    if (!leadResponse.ok) {
      const errorText = await leadResponse.text();
      throw new Error('Failed to save lead: ' + errorText);
    }

    const savedLead = await leadResponse.json();

    

    customAlert('Lead saved successfully!');
    document.getElementById('add-lead-form').reset();
    toggleReferenceNameField();
  } catch (error) {
    customAlert('Error: ' + error.message);
  } finally {
    loadingDiv.style.display = 'none';
  }
}


function toggleReferenceNameField() {
  const source = document.getElementById("source").value;
  const referenceField = document.getElementById("referenceNameField");
  if (source === "Reference") {
    referenceField.style.display = "block";
    document.getElementById("referenceName").setAttribute("required", "required");
  } else {
    referenceField.style.display = "none";
    document.getElementById("referenceName").removeAttribute("required");
    document.getElementById("referenceName").value = "";
  }
}








// Call toggleReferenceNameField once when form loads (optional)
document.addEventListener("DOMContentLoaded", () => {
  toggleReferenceNameField();
});





function getLeadTableTemplate() {
  return `
    <h2>View Leads</h2>

    <!-- 🔍 Search & Export Section -->
    <div style="display: flex; flex-wrap: wrap; gap: 10px; align-items: center; justify-content: space-between; margin-bottom: 15px;">
      <input 
        type="text" 
        id="searchLeadsInput" 
        placeholder="Search by Name, Phone, Location, or Assign To..." 
        oninput="filterLeads()" 
        style="padding: 8px 12px; flex: 1; max-width: 400px;"
      />

      <div style="display: flex; gap: 10px;">
        <button onclick="exportToExcelLead()" style="padding: 8px 12px;">Export to Excel</button>
        <button class="mobile-filter-btn" onclick="toggleLeadFilters()" style="padding: 8px 12px;">Show Filters</button>
      </div>
    </div>

    <!-- 🖥️ Desktop Filter Section -->
    <div id="leadFiltersSection" class="desktop-filters">
      ${getFilterOptionsHTML("desktop-")}
    </div>

    <!-- 📊 Table and Filters Container -->
    <div class="filter-slide">
      <div class="table-responsive" style="position: relative; min-height: 300px;">
        
        <!-- Loader -->
        <div id="glass-loader" class="glass-loader" style="display: none;">
          <div class="spinner"></div>
          <div class="loading-text">Loading Leads...</div>
        </div>

        <!-- 📋 Leads Table -->
        <table id="leadTable">
          <thead>
            <tr class="sticky">
              <th>Name</th>
              <th>Phone</th>
              <th>Status</th>
              <th>Remark</th>
              <th>Requirement</th>
              <th>Budget</th>
              <th>Location</th>
              <th>Created On</th>
              <th>Created By</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody></tbody>
        </table>

        <!-- 📱 Mobile Filter Panel -->
        <div id="mobileFilterPanel" class="mobile-filter-panel">
          <div class="mobile-filter-header" style="display: flex; justify-content: space-between; align-items: center;">
            <span><b>Filters</b></span>
            <button class="close-filter-btn" onclick="toggleLeadFilters()">❌</button>
          </div>
          <div class="mobile-filter-body">
            ${getFilterOptionsHTML("mobile-")}
          </div>
        </div>
      </div>
    </div>

    <!-- 📃 Pagination -->
    <div id="paginationControls" style="margin-top: 10px; text-align: center;"></div>
  `;
}

function getFilterOptionsHTML(prefix = "") {
  return `
    <div>
      <select id="${prefix}statusFilter" onchange="filterLeads()" style="padding: 5px;">
        <option value="">Status</option>
        <option value="NEW">NEW</option>
        <option value="CONTACTED">CONTACTED</option>
        <option value="CLOSED">CLOSED</option>
        <option value="DROPED">DROPED</option>
      </select>

      <select id="${prefix}budgetFilter" onchange="filterLeads()" style="padding: 5px;">
        <option value="">Budget</option>
      <option value="">Budget</option>
        <option value="0-500000">Below ₹5 Lakh</option>
        <option value="500000-1000000">₹5 Lakh - ₹10 Lakh</option>
        <option value="1000000-1500000">₹10 Lakh - ₹15 Lakh</option>
        <option value="1500000-2000000">₹15 Lakh - ₹20 Lakh</option>
        <option value="2000000-2500000">₹20 Lakh - ₹25 Lakh</option>
        <option value="2500000-3000000">₹25 Lakh - ₹30 Lakh</option>
        <option value="3000000-3500000">₹30 Lakh - ₹35 Lakh</option>
        <option value="3500000-4000000">₹35 Lakh - ₹40 Lakh</option>
        <option value="4000000-4500000">₹40 Lakh - ₹45 Lakh</option>
        <option value="4500000-5000000">₹45 Lakh - ₹50 Lakh</option>
        <option value="5000000-10000000">₹50 Lakh - ₹1 Cr</option>
        <option value="10000000-20000000">₹1 Cr - ₹2 Cr</option>
        <option value="20000000-30000000">₹2 Cr - ₹3 Cr</option>
        <option value="30000000-40000000">₹3 Cr - ₹4 Cr</option>
        <option value="40000000-50000000">₹4 Cr - ₹5 Cr</option>
        <option value="50000000-60000000">₹5 Cr - ₹6 Cr</option>
        <option value="60000000-70000000">₹6 Cr - ₹7 Cr</option>
        <option value="70000000-80000000">₹7 Cr - ₹8 Cr</option>
        <option value="80000000-90000000">₹8 Cr - ₹9 Cr</option>
        <option value="90000000-100000000">₹9 Cr - ₹10 Cr</option>
        <option value="100000000-200000000">₹10 Cr - ₹20 Cr</option>
        <option value="200000000-300000000">₹20 Cr - ₹30 Cr</option>
        <option value="300000000-400000000">₹30 Cr - ₹40 Cr</option>
        <option value="400000000-500000000">₹40 Cr - ₹50 Cr</option>
        <option value="500000000-600000000">₹50 Cr - ₹60 Cr</option>
        <option value="600000000-700000000">₹60 Cr - ₹70 Cr</option>
        <option value="700000000-800000000">₹70 Cr - ₹80 Cr</option>
        <option value="800000000-900000000">₹80 Cr - ₹90 Cr</option>
        <option value="900000000-1000000000">₹90 Cr - ₹100 Cr</option>
      </select>



      <select id="${prefix}sourceFilter" onchange="filterLeads()" style="padding: 5px;">
        <option value="">Sources</option>
        <option value="INSTAGRAM">Instagram</option>
        <option value="FACEBOOK">Facebook</option>
        <option value="YOUTUBE">YouTube</option>
        <option value="REFERENCE">Reference</option>
      </select>

      <select id="${prefix}actionFilter" onchange="filterLeads()" style="padding: 5px;">
        <option value="">Actions</option>
        <option value="ASSIGNED">Assigned</option>
        <option value="UNASSIGNED">Unassigned</option>
      </select>
    </div>
    <div>
      <button onclick="clearAllLeadFilters()" style="width: 100%;">Clear All Filters</button>
    </div>
  `;
}


function toggleLeadFilters() {
  const panel = document.getElementById("mobileFilterPanel");
  const tableContainer = document.querySelector(".table-responsive");

  panel.classList.toggle("show");
  tableContainer.classList.toggle("blur-table"); // Optional blur
}







function exportToExcelLead() {
  const table = document.getElementById("leadTable");
  const rows = table.querySelectorAll("tr");
  const data = [];

  // Extract table headers
  const headers = Array.from(rows[0].querySelectorAll("th"))
    .filter(th => th.innerText.trim().toLowerCase() !== "action") // Ignore 'Action' header
    .map(th => th.innerText.trim());
  data.push(headers);

  // Extract table rows
  for (let i = 1; i < rows.length; i++) {
    const cells = Array.from(rows[i].querySelectorAll("td"));
    const rowData = [];

    for (let j = 0; j < cells.length; j++) {
      const headerText = rows[0].querySelectorAll("th")[j].innerText.trim().toLowerCase();
      if (headerText !== "action") { // Skip cells where the header is 'Action'
        rowData.push(cells[j].innerText.trim());
      }
    }
    data.push(rowData);
  }

  // Convert data array to CSV string
  const csvContent = data.map(e => e.join(",")).join("\n");

  // Create a blob and trigger download
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);

  const a = document.createElement('a');
  a.href = url;
  a.download = 'leads_export.xls';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);

  URL.revokeObjectURL(url);
}





function formatToINR(price) {
  const num = Number(price);
  if (isNaN(num)) return price;
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
    minimumFractionDigits: 0
  }).format(num);
}


const pageSize = 10;
let currentPage = 0;
let currentCompanyId = null;
let previousLeadsHash = "";
let allLeads = [];

async function loadLeads(passedCompanyId, page = 0) {
  const localUser = JSON.parse(localStorage.getItem("user") || "{}");
  const userId = localUser?.userId;
  const companyId = passedCompanyId || localStorage.getItem("companyId");

  if (!userId || !companyId || companyId === "undefined") {
    console.error("❌ User ID or Company ID is missing. Cannot load leads.");
    customAlert("Login session expired. Please log in again.");
    return;
  }

  currentCompanyId = companyId;
  currentPage = page;

  const loading = document.getElementById("glass-loader");
  const tbody = document.querySelector("#leadTable tbody");

  if (loading) loading.style.display = "flex";
  if (tbody) tbody.innerHTML = "";

  const safeDisplay = (value) => {
    if (!value) return "N/A";
    const temp = document.createElement("div");
    temp.textContent = value;
    return temp.innerHTML;
  };

  const formatToINR = (amount) =>
    amount !== null && amount !== undefined && !isNaN(amount)
      ? new Intl.NumberFormat("en-IN", {
          style: "currency",
          currency: "INR",
        }).format(amount)
      : "N/A";

  try {
    const response = await fetch(
      `/api/companies/${companyId}/leads/created-or-assigned/${userId}?page=${page}&size=${pageSize}`,
      {
        method: "GET",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to load leads: ${response.statusText}`);
    }

    const data = await response.json();
    const leads = data.content || [];
    totalPages = data.totalPages || 1;

    allLeads = leads;

    const nonClosedLeads = leads.filter((lead) => lead.status !== "CLOSED" && lead.status !== "DROPED");
    const closedLeads = leads.filter((lead) => lead.status === "CLOSED");
    const droppedLeads = leads.filter((lead) => lead.status === "DROPED");

    const generateLeadRow = (lead) => {
      const createdOn = lead.createdAt
        ? `${new Date(lead.createdAt).toLocaleDateString()} ${new Date(lead.createdAt).toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
          })}`
        : "N/A";

      const sourceDisplay =
        lead.source === "Reference" && lead.referenceName
          ? `${safeDisplay(lead.source)} (${safeDisplay(lead.referenceName)})`
          : safeDisplay(lead.source);

      const formattedPrice = formatToINR(lead.budget);

      const rowStyle = {
        NEW: "background-color: #fff8e1;",
        CONTACTED: "background-color: #e0f7fa;",
        CLOSED: "background-color: #f8d7da;",
        DROPED: "background-color: #000; color: white;",
      }[lead.status] || "";

      const statusBgColor = {
        NEW: "#f0ad4e",
        CONTACTED: "#5bc0de",
        CLOSED: "#d9534f",
        DROPED: "#000",
      }[lead.status] || "#ccc";

      const canUpdate = lead?.createdBy?.userId === userId;

      return `
        <tr style="${rowStyle}">
          <td>${safeDisplay(lead.name)}</td>
          <td>${safeDisplay(lead.phone)}</td>
          <td>
            <select onchange="updateStatus(${lead.leadId}, this.value)"
              style="background-color: ${statusBgColor}; color: white; border: none; padding: 2px 6px; border-radius: 4px;">
              <option value="NEW" ${lead.status === "NEW" ? "selected" : ""}>NEW</option>
              <option value="CONTACTED" ${lead.status === "CONTACTED" ? "selected" : ""}>CONTACTED</option>
              <option value="CLOSED" ${lead.status === "CLOSED" ? "selected" : ""}>CLOSED</option>
              <option value="DROPED" ${lead.status === "DROPED" ? "selected" : ""}>DROPED</option>
            </select>
          </td>
          <td>
            <button onclick="openAddLeadRemarkModal(${lead.leadId})">Add</button>
            <button onclick="viewLeadRemarks(${lead.leadId})">View</button>
          </td>
          <td>${safeDisplay(lead.requirement)}</td>
          <td>${formattedPrice}</td>
          <td>${safeDisplay(lead.location)}</td>
          <td>${createdOn}</td>
          <td>${safeDisplay(lead?.createdBy?.name)}</td>
          <td>
            ${
              canUpdate
                ? `<button onclick="handleShowUpdateLead(this)" 
                      data-lead='${encodeURIComponent(JSON.stringify(lead))}' 
                      data-company-id="${companyId}">Update</button>`
                : `<button disabled style="opacity: 0.5; cursor: not-allowed;">Update</button>`
            }
          </td>
        </tr>
      `;
    };

    const rows =
      nonClosedLeads.map(generateLeadRow).join("") +
      closedLeads.map(generateLeadRow).join("") +
      droppedLeads.map(generateLeadRow).join("");

    tbody.innerHTML = rows;
    renderPaginationControls(companyId, totalPages, currentPage);
  } catch (error) {
    console.error("❌ Error loading leads:", error);
    customAlert("Error loading leads. Please check your network or session.");
  } finally {
    if (loading) loading.style.display = "none";
  }
}



let currentFilters = {
  search: "",
  status: "",
  budget: "",
  source: "",
  action: ""
};


function getFilterValue(id) {
  return (
    document.getElementById("desktop-" + id)?.value ||
    document.getElementById("mobile-" + id)?.value ||
    ''
  );
}

async function filterLeads(page = 0) {
  const companyId = localStorage.getItem("companyId");
  const loggedInUserId = parseInt(localStorage.getItem("userId"), 10);

  if (!companyId || companyId === "undefined") {
    customAlert("Company ID missing. Please log in again.");
    return;
  }

  const search = document.getElementById("searchLeadsInput")?.value.trim();
  const status = getFilterValue("statusFilter");
  const budgetRange = getFilterValue("budgetFilter");
  const source = getFilterValue("sourceFilter");
  const action = getFilterValue("actionFilter");

  let minBudget = null;
  let maxBudget = null;

  if (budgetRange) {
    const [min, max] = budgetRange.split("-");
    minBudget = min && !isNaN(min) ? min : null;
    maxBudget = max && !isNaN(max) ? max : null;
  }

  const loading = document.getElementById("glass-loader");
  const tbody = document.querySelector("#leadTable tbody");

  if (loading) loading.style.display = "flex";
  if (tbody) tbody.innerHTML = "";

  try {
    const params = new URLSearchParams();
    if (search) params.append("search", search);
    if (status) params.append("status", status);
    if (minBudget !== null) params.append("minBudget", minBudget);
    if (maxBudget !== null) params.append("maxBudget", maxBudget);
    if (source) params.append("source", source);
    if (action) params.append("action", action);
    params.append("page", page);
    params.append("size", pageSize);

    const response = await fetch(
      `/api/companies/${companyId}/leads/created-or-assigned/${loggedInUserId}/search?${params.toString()}`,
      {
        method: "GET",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch leads: ${response.statusText}`);
    }

    const data = await response.json();
    const rawLeads = data.content || [];
    const totalPages = data.totalPages || 1;

    // Only show DROPED leads if explicitly filtered
    const filteredLeads = status === "DROPED"
      ? rawLeads.filter((lead) => lead.status === "DROPED")
      : rawLeads.filter((lead) => lead.status !== "DROPED");

    const nonClosedLeads = filteredLeads.filter((lead) => lead.status !== "CLOSED");
    const closedLeads = filteredLeads.filter((lead) => lead.status === "CLOSED");

    const formatToINR = (amount) =>
      amount !== null && amount !== undefined && !isNaN(amount)
        ? new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(amount)
        : "N/A";

    const safeDisplay = (str) => {
      if (!str) return "N/A";
      const temp = document.createElement("div");
      temp.textContent = str;
      return temp.innerHTML;
    };

    const generateLeadRow = (lead) => {
      const canUpdate = lead?.createdBy?.userId === loggedInUserId;

      const createdOn = lead.createdAt
        ? `${new Date(lead.createdAt).toLocaleDateString()} ${new Date(lead.createdAt).toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
          })}`
        : "N/A";

      const sourceDisplay =
        lead.source === "Reference" && lead.referenceName
          ? `${safeDisplay(lead.source)} (${safeDisplay(lead.referenceName)})`
          : safeDisplay(lead.source);

      const rowStyle = {
        NEW: "#fff8e1",
        CONTACTED: "#e0f7fa",
        CLOSED: "#f8d7da",
        DROPED: "#000",
      }[lead.status] || "transparent";

      const selectBg = {
        NEW: "#f0ad4e",
        CONTACTED: "#5bc0de",
        CLOSED: "#d9534f",
        DROPED: "#000",
      }[lead.status] || "#ccc";

      return `
        <tr style="background-color: ${rowStyle}; ${lead.status === "DROPED" ? 'color: white;' : ''}">
          <td>${safeDisplay(lead.name)}</td>
          <td>${safeDisplay(lead.phone)}</td>
          <td>
            <select onchange="updateStatus(${lead.leadId}, this.value)"
              style="background-color: ${selectBg}; color: white; border: none; padding: 2px 6px; border-radius: 4px;">
              <option value="NEW" ${lead.status === "NEW" ? "selected" : ""}>NEW</option>
              <option value="CONTACTED" ${lead.status === "CONTACTED" ? "selected" : ""}>CONTACTED</option>
              <option value="CLOSED" ${lead.status === "CLOSED" ? "selected" : ""}>CLOSED</option>
              <option value="DROPED" ${lead.status === "DROPED" ? "selected" : ""}>DROPED</option>
            </select>
          </td>
          <td>
            <button onclick="openAddLeadRemarkModal(${lead.leadId})">Add</button>
            <button onclick="viewLeadRemarks(${lead.leadId})">View</button>
          </td>
          <td>${safeDisplay(lead.requirement)}</td>
          <td>${formatToINR(lead.budget)}</td>
          <td>${safeDisplay(lead.location)}</td>
          <td>${createdOn}</td>
          <td>${safeDisplay(lead?.createdBy?.name)}</td>
          <td>
            ${
              lead.status === "CLOSED"
                ? `<button disabled style="opacity: 0.6; background-color: #d9534f; color: white; border: none; padding: 5px 10px; border-radius: 4px; cursor: not-allowed;">Closed</button>`
                : canUpdate
                ? `<button onclick="handleShowUpdateLead(this)"
                      data-lead='${encodeURIComponent(JSON.stringify(lead))}'
                      data-company-id="${companyId}">Update</button>`
                : `<button disabled style="opacity: 0.5; cursor: not-allowed;">Update</button>`
            }
          </td>
        </tr>
      `;
    };

    tbody.innerHTML =
      nonClosedLeads.map(generateLeadRow).join("") +
      closedLeads.map(generateLeadRow).join("");

    renderPaginationControls(companyId, totalPages, page);
  } catch (err) {
    console.error("❌ Error filtering leads:", err);
    customAlert("Something went wrong while filtering leads.");
  } finally {
    if (loading) loading.style.display = "none";
  }
}





function clearAllLeadFilters() {
  document.getElementById('searchLeadsInput').value = '';

  const filterIds = ["statusFilter", "budgetFilter", "sourceFilter", "actionFilter"];

  filterIds.forEach(id => {
    const desktopEl = document.getElementById(`desktop-${id}`);
    const mobileEl = document.getElementById(`mobile-${id}`);

    if (desktopEl) desktopEl.value = '';
    if (mobileEl) mobileEl.value = '';
  });

  filterLeads(); // Re-filter after clearing
}




function renderPaginationControls(companyId, totalPages, currentPage) {
  const container = document.getElementById("paginationControls");
  if (!container) return;

  let html = `
    <button onclick="filterLeads(${currentPage - 1})" ${currentPage === 0 ? 'disabled' : ''}>Previous</button>
    <span style="margin: 0 10px;">Page ${currentPage + 1} of ${totalPages}</span>
    <button onclick="filterLeads(${currentPage + 1})" ${currentPage >= totalPages - 1 ? 'disabled' : ''}>Next</button>
  `;

  container.innerHTML = html;
}


// Utility to safely render HTML text


// let allLeads = [];



function openAddLeadRemarkModal(leadId) {
  currentLeadId = leadId;
  document.getElementById("leadRemarkInput").value = "";
  document.getElementById("leadModalBody").innerHTML = "";
  document.getElementById("leadRemarkInputContainer").style.display = "block";
  document.getElementById("leadRemarkModal").style.display = "block";
}


function safeDisplay(value) {
  return value ? String(value).replace(/</g, "&lt;").replace(/>/g, "&gt;") : "N/A";
}

function viewLeadRemarks(leadId) {
  currentLeadId = leadId;
  document.getElementById("leadRemarkInputContainer").style.display = "none";

  const lead = allLeads.find(l => l.leadId === leadId);
  if (!lead) {
    customAlert("Lead not found. Please refresh the page.");
    return;
  }

  const remarks = lead.remarks || [];
  remarks.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

  let html = `
    <div style="margin-bottom:15px;padding:10px;font-size:18px;font-weight:bold;color:black;border-bottom:2px solid #ccc;">
      Lead Name: ${safeDisplay(lead.name)}
    </div>`;

  if (remarks.length === 0) {
    html += "<p>No remarks found.</p>";
  } else {
    html += `
        <table class="remak-table" style="width:100%; border-collapse:collapse;">
          <thead style="position: sticky; top: 0; background-color: #f0f0f0; z-index: 1;">
            <tr>
              <th style="padding:10px; border-bottom:1px solid #ccc; color:black;">Remark</th>
              <th style="padding:10px; border-bottom:1px solid #ccc; color:black;">Date & Time</th>
              <th style="padding:10px; border-bottom:1px solid #ccc; color:black;">Created By</th>
            </tr>
          </thead>
          <tbody>`;

    remarks.forEach(r => {
      const createdDateTime = r.createdAt
        ? new Date(r.createdAt).toLocaleString(undefined, {
          day: '2-digit',
          month: 'short',
          year: 'numeric',
          hour: '2-digit',
          minute: '2-digit',
          hour12: true
        })

        : "-";

      const createdBy = r.createdBy?.name || "Unknown";

      // // ⬇️ Word wrap after every 15 words
      // const wrappedRemark = safeDisplay(r.remark)
      //   .split(" ")
      //   .reduce((acc, word, index) => {
      //     acc += word + " ";
      //     if ((index + 1) % 15 === 0) acc += "<br>";
      //     return acc;
      //   }, "").trim();

      html += `
            <tr>
              <td style="padding:10px; border-bottom:1px solid #eee; color:black; white-space:normal; word-break:break-word;">${r.remark}</td>
              <td style="padding:10px; border-bottom:1px solid #eee; color:black;">${createdDateTime}</td>
              <td style="padding:10px; border-bottom:1px solid #eee; color:black;">${safeDisplay(createdBy)}</td>
            </tr>`;
    });

    html += `</tbody></table></div>`;
  }

  document.getElementById("leadModalBody").innerHTML = html;
  document.getElementById("leadRemarkModal").style.display = "block";
}







function submitLeadRemark() {
  const remark = document.getElementById("leadRemarkInput").value.trim();
  const companyId = parseInt(localStorage.getItem("companyId"), 10);
  const user = JSON.parse(localStorage.getItem("user") || "{}");
  const userId = user.userId;

  if (!companyId) return customAlert("Company ID not found in local storage!");
  if (!userId) return customAlert("User ID not found. Please login again.");
  if (!remark) return customAlert("Remark cannot be empty!");

  fetch(`/api/companies/${companyId}/leads/${currentLeadId}/remarks`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: 'include',
    body: JSON.stringify({ remark, userId })
  })
    .then(res => {
      if (!res.ok) throw new Error("Failed to add remark");
      return res.text();
    })
    .then(message => {
      customAlert(message);
      document.getElementById("leadRemarkModal").style.display = "none";
      
      // ✅ 1. Reload allLeads by calling filterLeads() or loadLeads()
      const isFiltering = Object.values(currentFilters).some(val => !!val); // check filters
      if (isFiltering) {
        filterLeads(currentPage); // filters applied
      } else {
        loadLeads(currentCompanyId, currentPage); // no filters
      }

      // ✅ 2. Delay + reopen remarks modal
    })
    .catch(err => customAlert(err.message));
}


function closeLeadRemarkModal() {
  document.getElementById("leadRemarkModal").style.display = "none";
  currentLeadId = null;
}



function handleShowUpdateLead(btn) {
  try {
    const leadData = btn.getAttribute("data-lead");
    const lead = JSON.parse(decodeURIComponent(leadData));
    // //console.log("Parsed Lead:", lead);
    showUpdateLeadModal(lead);
  } catch (error) {
    console.error("Failed to parse lead data:", error);
    // customAlert("Error loading lead data.");
  }
}





function showUpdateLeadModal(lead) {
  // Remove any existing modal
  const existingModal = document.querySelector('.modal2');
  if (existingModal) existingModal.remove();

  // Escape values for safety
  const safe = (value) => String(value || '').replace(/"/g, '&quot;');

  const modalHtml = `
    <div class="modal2" role="dialog" aria-modal="true">
      <div class="modal-content1">
        <span class="close" onclick="document.querySelector('.modal2').remove()" role="button" aria-label="Close" style="position:absolute; top:10px; right:20px; cursor:pointer; color:red;">&times;</span>
        <h2>Update Lead</h2>
        <form id="updateLeadForm">
          <label for="updateName">Name:</label>
          <input type="text" id="updateName" value="${safe(lead.name)}" placeholder="Name" required />

          <label for="updateEmail">Email:</label>
          <input type="email" id="updateEmail" value="${safe(lead.email)}" placeholder="Email" />

          <label for="updatePhone">Phone:</label>
          <input type="tel" id="updatePhone" value="${safe(lead.phone)}" placeholder="Phone" required />

          <label for="updateStatus">Status:</label>
          <select id="updateStatus" required>
            <option value="NEW" ${lead.status === "NEW" ? "selected" : ""}>New</option>
            <option value="CONTACTED" ${lead.status === "CONTACTED" ? "selected" : ""}>Contacted</option>
            <option value="CLOSED" ${lead.status === "CLOSED" ? "selected" : ""}>Closed</option>
          </select>

          <label for="updateBudget">Budget (₹):</label>
          <input type="number" id="updateBudget" value="${safe(lead.budget)}" placeholder="Budget" step="0.01" />

           <label for="updateLocation">Location (₹):</label>
          <input type="text" id="updateLocation" value="${safe(lead.location)}" placeholder="Location"  />

          <label for="updateRequirement">Requirement:</label>
          <textarea id="updateRequirement" rows="3" cols="40">${safe(lead.requirement)}</textarea>

          <!-- Source field is not editable, so not included here -->
          <!-- If you want to show source for info, add a disabled input or span -->

          <button type="submit">Update Lead</button>
        </form>
      </div>
    </div>
  `;

  // Append modal
  document.body.insertAdjacentHTML('beforeend', modalHtml);

  // Form submission handler
  document.getElementById("updateLeadForm").addEventListener("submit", function (event) {
    event.preventDefault();
    submitUpdatedLead(lead); // pass full original lead object
  });
}



function submitUpdatedLead(originalLead) {
  const name = document.getElementById("updateName").value.trim();
  const email = document.getElementById("updateEmail").value.trim();
  const phone = document.getElementById("updatePhone").value.trim();
  const status = document.getElementById("updateStatus").value;
  const budgetValue = document.getElementById("updateBudget").value.trim();
  const requirement = document.getElementById("updateRequirement").value.trim();
  const location = document.getElementById("updateLocation").value.trim();

  const companyId = parseInt(localStorage.getItem("companyId"), 10);

  if (!companyId || companyId === "undefined") {
    customAlert("Company ID is missing. Please login again.");
    return;
  }


  const updatedLead = {
    ...originalLead,
    name,
    email,
    phone,
    status,
    budget: budgetValue ? parseFloat(budgetValue) : null,
    requirement,
    location,
  };

  fetch(`/api/companies/${companyId}/leads/${originalLead.leadId}`, {
    method: "PUT",
    headers: {
      'Content-Type': 'application/json',
      // 'Authorization': `Bearer ${yourToken}`, // Uncomment if using token-based auth
    },
    credentials: 'include',
    body: JSON.stringify(updatedLead),
  })
    .then(async (response) => {
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to update lead: ${response.status} - ${errorText}`);
      }
      return response.json();
    })
    .then((data) => {
      customAlert("Lead updated successfully!");
      document.querySelector(".modal2").remove();
     filterLeads(currentPage); // Refresh list
    })
    .catch((error) => {
      console.error("Error updating lead:", error);
      customAlert("Error updating lead: " + error.message);
    });
}




async function updateStatus(leadId, newStatus) {
  const companyId = parseInt(localStorage.getItem("companyId"), 10);

  if (!companyId || companyId === "undefined") {
    customAlert("Company ID is missing. Please login again.");
    return;
  }

  try {
    const response = await fetch(`/api/companies/${companyId}/leads/${leadId}/status?status=${encodeURIComponent(newStatus)}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        // 'Authorization': `Bearer ${yourToken}`, // Uncomment if using token-based auth
      },
      credentials: 'include'
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Failed to update status: ${errorText}`);
    }

    customAlert("Status updated successfully");
    await filterLeads(currentPage);

  } catch (error) {
    console.error("Error:", error);
    customAlert("Failed to update status");
  }
}

