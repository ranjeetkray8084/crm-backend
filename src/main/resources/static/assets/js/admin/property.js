function getAddPropertyForm() {
  return `
    <div class="addform">
      <h2>Add Property</h2>
      <div class="addproperty" style="max-height: 500px; overflow-y: auto; border: 1px solid #ccc; padding: 15px;">
        <form id="addPropertyForm" onsubmit="saveProperty(event)">
          <div>
            <label for="propertyName">Property Name:</label>
            <input type="text" id="propertyName" name="propertyName" required>
          </div>
          <div>
            <label for="type">Type:</label>
            <select id="type" name="type" required onchange="toggleBhkField(); togglePriceLabel()">
              <option value="Residential">Residential</option>
              <option value="Retail">Retail</option>
              <option value="Office">Office</option>
              <option value="Plot">Plot</option>
            </select>
          </div>
          <div>
            <label for="bhk">BHK:</label>
            <input type="text" id="bhk" name="bhk" placeholder="e.g., 2BHK" required />
          </div>
          <div style="display: flex; gap: 10px; width: 100%;">
            <div style="flex: 1;">
              <label for="unit">Unit Details:</label>
              <input type="text" id="unit" name="unit" placeholder="Unit Details" style="width: 100%;" />
            </div>
            <div style="flex: 1;">
              <label for="floor">Floor:</label>
              <input type="text" id="floor" name="floor" placeholder="Floor" style="width: 100%;" />
            </div>
          </div>
          <div>
            <label for="size">Size:</label>
            <div style="display: flex; gap: 10px;">
              <input type="number" id="sizeValue" name="sizeValue" placeholder="1200" required>
              <select id="sizeUnit" name="sizeUnit" required>
                <option value="sqft">sqft</option>
                <option value="sqyd">sqyd</option>
              </select>
            </div>
          </div>
          <div>
            <label for="status">Status:</label>
            <select id="status" name="status" required>
              <option value="AVAILABLE_FOR_SALE">Available for Sale</option>
              <option value="AVAILABLE_FOR_RENT">Available for Rent</option>
              <option value="RENT_OUT">Rented Out</option>
              <option value="SOLD_OUT">Sold Out</option>
            </select>
          </div>
          <div>
            <label id="priceLabel" for="price">Price / Lease Amount:</label>
            <input type="number" id="price" name="price" required>
          </div>
          <div>
            <label for="sector">Sector:</label>
            <input type="text" id="sector" name="sector" required>
          </div>
          <div>
            <label for="location">Location:</label>
            <input type="text" id="location" name="location">
          </div>
          <div>
            <label for="source">Source:</label>
            <select id="source" name="source" required onchange="handleSourceChange()">
              <option value="Social Media">Social Media</option>
              <option value="Cold Call">Cold Call</option>
              <option value="Project Call">Project Call</option>
              <option value="Reference">Reference</option>
              <option value="Broker">Broker</option>
            </select>
          </div>
          <div id="referenceNameField" style="display: none;">
            <label for="referenceName">Enter Name (Reference):</label>
            <input type="text" id="referenceName" name="referenceName" placeholder="Enter reference name">
          </div>
          <div>
            <label for="ownerName">Owner Name:</label>
            <input type="text" id="ownerName" name="ownerName" required>
          </div>
          <div>
            <label for="ownerContact">Owner Contact:</label>
            <input type="text" id="ownerContact" name="ownerContact" required>
          </div>
          <div style="display: flex; justify-content: center;">
            <button type="submit">Save Property</button>
          </div>
        </form>
      </div>
    </div>
  `;
}

function handleSourceChange() {
  toggleReferenceNameField();
  toggleBroker();
}

// function togglePriceLabel() {
//   const typeSelect = document.getElementById('type');
//   const priceLabel = document.getElementById('priceLabel');
//   const leaseTypes = ['Office', 'Retail'];

//   priceLabel.textContent = leaseTypes.includes(typeSelect.value) ? 'Lease Amount:' : 'Price:';
// }

function toggleBhkField() {
  const typeSelect = document.getElementById('type');
  const bhkInput = document.getElementById('bhk');
  const disable = ['Office', 'Retail', 'Plot'].includes(typeSelect.value);
  bhkInput.value = '';
  bhkInput.disabled = disable;
  bhkInput.required = !disable;
}

function toggleReferenceNameField() {
  const source = document.getElementById("source").value;
  const referenceField = document.getElementById("referenceNameField");
  referenceField.style.display = (source === "Reference") ? "block" : "none";
}

function toggleBroker() {
  const source = document.getElementById("source").value;
  const ownerNameLabel = document.querySelector("label[for='ownerName']");
  const ownerContactLabel = document.querySelector("label[for='ownerContact']");

  if (source === "Broker") {
    ownerNameLabel.textContent = "Broker Name:";
    ownerContactLabel.textContent = "Broker Contact:";
  } else {
    ownerNameLabel.textContent = "Owner Name:";
    ownerContactLabel.textContent = "Owner Contact:";
  }
}

async function saveProperty(event) {
  event.preventDefault();

  const localUser = JSON.parse(localStorage.getItem("user") || "{}");
  const userId = localUser.userId;
  const companyId = parseInt(localStorage.getItem("companyId"), 10);
  if (!userId || !companyId) return customAlert("User or Company ID missing");

  const sourceValue = document.getElementById("source").value;
  const referenceNameInput = document.getElementById("referenceName");
  const referenceName = referenceNameInput ? referenceNameInput.value.trim() : "";

  if (sourceValue === "Reference" && !referenceName) {
    return customAlert("Please enter the reference name");
  }

const typeValue = document.getElementById("type").value;
let bhkValue = document.getElementById("bhk").value.trim();

if (["Office", "Retail", "Plot"].includes(typeValue)) {
  bhkValue = "";
}

  const property = {
    propertyName: document.getElementById("propertyName").value.trim(),
    createdBy: { userId: userId },
    type: typeValue,
    bhk: bhkValue,
    unitDetails: document.getElementById("unit").value.trim(),
    size: document.getElementById("sizeValue").value.trim(),
    floor: document.getElementById("floor").value.trim(),
    ownerName: document.getElementById("ownerName").value.trim(),
    ownerContact: document.getElementById("ownerContact").value.trim(),
    price: document.getElementById("price").value.trim(),
    status: document.getElementById("status").value,
    sector: document.getElementById("sector").value.trim(),
    location: document.getElementById("location").value.trim(),
    source: sourceValue
  };

  if (sourceValue === "Reference") {
    property.referenceName = referenceName;
  }

  const phoneRegex = /^[+]?[0-9]{10,15}$/;
  if (!phoneRegex.test(property.ownerContact)) return customAlert("Invalid contact number");

  try {
    const response = await fetch(`/api/companies/${companyId}/properties`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify(property)
    });

    if (!response.ok) throw new Error("Save failed");

    customAlert("Property saved successfully");
    document.getElementById("addPropertyForm").reset();
    toggleReferenceNameField();
    toggleBhkField();
    loadProperty();
  } catch (error) {
    customAlert(`Error: ${error.message}`);
  }
}


function getPropertyTableTemplate() {
  return `
    <h2>View Properties</h2>

    <div style="display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 15px; justify-content: space-between;">
      <div id="tagContainer" class="tag-container">
          <input id="tagInput" type="text" placeholder="Add more..." onkeydown="handleTagInput(event)">
        </div>


      <button onclick="exportToExcelProperty()"> Export</button>
      <button class="mobile-filter-btn" onclick="toggleLeadFilters()" style="padding: 5px 10px;">Show Filters</button>

    </div>


     <!-- 🖥️ Desktop Filter Section -->
        <div id="leadFiltersSection" class="desktop-filters">
      ${getPropertyFilterOptionsHTML("desktop-")}
    </div>


    <div class="table-responsive" style="position: relative; min-height: 300px;">
      <div id="property-glass-loader" class="glass-loader" style="display: none;">
        <div class="spinner"></div>
        <div class="loading-text">Loading Properties...</div>
      </div>

      <table id="propertyTable">
        <thead>
          <tr class="sticky">
            <th>Project Name</th>
            <th>Type</th>
            <th>BHK</th>
            <th>Unit Details</th>
            <th>Floor</th>
            <th>Size (sqft/sqyd)</th>
            <th>Sector</th>
            <th>Location</th>
            <th>Status</th>
            <th>Price / Lease</th>
            <th>Source</th>
            <th>Remarks</th>
            <th>Owner Name</th>
            <th>Owner Number</th>
            <th>Created By</th>
            <th>Created On</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody></tbody>
      </table>

      <div id="mobileFilterPanel" class="mobile-filter-panel">
        <div class="mobile-filter-header">
          <span><b>Filters</b></span>
          <button class="close-filter-btn" onclick="toggleLeadFilters()">❌</button>
        </div>
        <div class="mobile-filter-body">
          ${getPropertyFilterOptionsHTML("mobile-")}
        </div>
      </div>
    </div>

    <div id="paginationControls" style="margin-top: 10px; text-align: center;"></div>
  `;
}


function getPropertyFilterOptionsHTML(prefix = "") {
  return `
    <div>
      <select id="${prefix}budgetFilter" onchange="filterProperties()" style="padding: 5px;">
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
        <option value="50000000-100000000">₹5 Cr - ₹10 Cr</option>
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

      <select id="${prefix}createdByFilter" onchange="filterProperties()" style="padding: 5px;">
        <option value="">Created By</option>
      </select>

      <select id="${prefix}statusFilter" onchange="filterProperties()" style="padding: 5px;">
        <option value="">Status</option>
        <option value="AVAILABLE_FOR_SALE">For Sale</option>
        <option value="AVAILABLE_FOR_RENT">For Rent</option>
        <option value="RENT_OUT">Rented Out</option>
        <option value="SOLD_OUT">Sold Out</option>
      </select>

      <select id="${prefix}typeFilter" onchange="filterProperties()" style="padding: 5px;">
        <option value="">Type</option>
        <option value="Office">Office</option>
        <option value="Retail">Retail</option>
        <option value="Residential">Residential</option>
        <option value="Plot">Plot</option>
      </select>

      <select id="${prefix}sourceFilter" onchange="filterProperties()" style="padding: 5px;">
        <option value="">Source</option>
        <option value="Social Media">Social Media</option>
        <option value="Cold Call">Cold Call</option>
        <option value="Reference">Reference</option>
        <option value="Broker">Broker</option>
      </select>

      <select id="${prefix}bhkFilter" onchange="filterProperties()" style="padding: 5px;">
        <option value="">BHK</option>
        <option value="1">1 BHK</option>
        <option value="2">2 BHK</option>
        <option value="3">3 BHK</option>
        <option value="4">4 BHK</option>
      </select>

      <button onclick="clearAllPropertyFilters()">Clear All Filters</button>
    </div>
  `;
}


function toggleLeadFilters() {
  const panel = document.getElementById("mobileFilterPanel");
  const tableContainer = document.querySelector(".table-responsive");

  if (panel && tableContainer) {
    panel.classList.toggle("show");
    tableContainer.classList.toggle("blur-table"); // Optional blur
  } else {
    console.warn("Filter panel or table container not found in DOM");
  }
}





function loadCreatedByOptions() {
  const companyId = parseInt(localStorage.getItem('companyId'), 10);
  const localUser = JSON.parse(localStorage.getItem("user") || "{}");
  const currentUserId = localUser.userId || localUser.id;
  const currentUserName = localUser.name || "Me";

  const desktopSelect = document.getElementById("desktop-createdByFilter");
  const mobileSelect = document.getElementById("mobile-createdByFilter");

  // Clear old options
  if (desktopSelect) desktopSelect.innerHTML = '<option value="">Created By</option>';
  if (mobileSelect) mobileSelect.innerHTML = '<option value="">Created By</option>';

  // ✅ Add "Me" option
  const meOptionDesktop = new Option("Me", currentUserId);
  const meOptionMobile = new Option("Me", currentUserId);
  if (desktopSelect) desktopSelect.appendChild(meOptionDesktop);
  if (mobileSelect) mobileSelect.appendChild(meOptionMobile);

  // ✅ Fetch users
  fetch(`/api/users/all-users/${companyId}`, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
  })
    .then(res => res.json())
    .then(data => {
      data.forEach(user => {
        if (!user.name || !user.userId || user.userId === currentUserId) return;

        const optionDesktop = new Option(user.name, user.userId);
        const optionMobile = new Option(user.name, user.userId);

        if (desktopSelect) desktopSelect.appendChild(optionDesktop);
        if (mobileSelect) mobileSelect.appendChild(optionMobile);
      });
    })
    .catch(err => console.error("❌ Error loading createdBy options:", err));
}







function exportToExcelProperty() {
  const table = document.getElementById("propertyTable");
  const rows = table.querySelectorAll("tr");

  let excelData = "<table border='1'>";

  rows.forEach((row) => {
    excelData += "<tr>";
    const cells = row.querySelectorAll("th, td");

    cells.forEach((cell, cellIndex) => {
      const cellText = cell.innerText.trim().toLowerCase();

      // Skip if header is "action" or "remark" or if it's the last column
      if (
        cellText === "action" ||
        cellText === "remark" ||
        cellIndex === cells.length - 1
      ) {
        return;
      }

      excelData += `<td>${cell.innerText}</td>`;
    });

    excelData += "</tr>";
  });

  excelData += "</table>";

  const blob = new Blob([excelData], { type: "application/vnd.ms-excel" });
  const link = document.createElement("a");
  link.href = URL.createObjectURL(blob);
  link.download = "Properties.xls";
  link.click();
}




// Save Property to backend



// Load all properties and render them into the table
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

const propertyPageSize = 10;
let propertyCurrentPage = 0;
let propertyCurrentCompanyId = null;
let allProperty = [];

async function loadProperty(passedCompanyId, page = 0) {
  const tableBody = document.querySelector("#propertyTable tbody");
  const loading = document.getElementById("property-glass-loader");

  if (loading) loading.style.display = "flex";
  tableBody.innerHTML = "";

  const safeDisplay = (value) => {
    return value === null || value === undefined || value === "" ? "N/A" : value;
  };

  const formatToINR = (amount) => {
    if (amount === null || amount === undefined || isNaN(amount)) return 'N/A';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR'
    }).format(amount);
  };

  try {
    const companyId = passedCompanyId || parseInt(localStorage.getItem("companyId"), 10);
    const userId = parseInt(localStorage.getItem("userId"), 10);

    if (!companyId || isNaN(userId)) {
      console.error("❌ Missing companyId or userId");
      customAlert("Missing required session. Please log in again.");
      return;
    }

    propertyCurrentCompanyId = companyId;
    propertyCurrentPage = page;

    const response = await fetch(`/api/companies/${companyId}/properties/paged?page=${page}&size=${propertyPageSize}`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
    });
    if (!response.ok) throw new Error("Failed to fetch properties");

    const data = await response.json();
    const properties = data.content || [];
        allProperty = properties;
    const totalPages = data.totalPages || 1;

    properties.forEach((property) => {
      try {
        const isOwner = property.createdBy?.userId === userId;
        const formattedPrice = formatToINR(property.price);

        let sourceDisplay = safeDisplay(property.source);
        if (property.source === "Reference" && property.referenceName) {
          sourceDisplay += ` (${safeDisplay(property.referenceName)})`;
        }

        let createdOn = "N/A";
        if (property.createdAt) {
          const date = new Date(property.createdAt);
          const day = date.getDate().toString().padStart(2, '0');
          const month = (date.getMonth() + 1).toString().padStart(2, '0');
          const year = date.getFullYear();
          const hours = date.getHours().toString().padStart(2, '0');
          const minutes = date.getMinutes().toString().padStart(2, '0');
          createdOn = `${day}/${month}/${year} ${hours}:${minutes}`;
        }

        const row = document.createElement("tr");
        row.innerHTML = `
          <td>${safeDisplay(property.propertyName)}</td>
          <td>${safeDisplay(property.type)}</td>
          <td>${safeDisplay(property.bhk)}</td>
          <td>${isOwner ? safeDisplay(property.unitDetails) : '🔒 Hidden'}</td>
          <td>${isOwner ? safeDisplay(property.floor) : '🔒 Hidden'}</td>
          <td>${safeDisplay(property.size)}</td>
          <td>${safeDisplay(property.sector)}</td>
          <td>${safeDisplay(property.location)}</td>
          <td>${safeDisplay(property.status)}</td>
          <td>${formattedPrice}</td>
          <td>${sourceDisplay}</td>
          <td>
            <button onclick="openAddPropertyRemarkModal(${property.propertyId})">Add</button>
            <button onclick="viewPropertyRemarks(${property.propertyId})">View</button>
          </td>
          <td>${safeDisplay(property.ownerName)}</td> <!-- Always show -->
          <td>${isOwner ? safeDisplay(property.ownerContact) : '🔒 Hidden'}</td>
          <td>${safeDisplay(property.createdByName)}</td>
          <td>${createdOn}</td>
          <td>
            ${isOwner
            ? `<button onclick="handleShowUpdateProperty(this)" 
                        data-property='${encodeURIComponent(JSON.stringify(property))}' 
                        data-company-id="${companyId}">Update</button>`
            : `<button disabled style="opacity: 0.5; cursor: not-allowed;">Update</button>`
          }
          </td>
        `;
        tableBody.appendChild(row);
      } catch (rowError) {
        console.error("⚠️ Error rendering property row:", rowError);
      }
    });

    renderPropertyPaginationControls(companyId, totalPages, propertyCurrentPage);

  } catch (error) {
    console.error("❌ Error loading properties:", error);
    customAlert("Error loading properties. Please check your network or session.");
  } finally {
    if (loading) loading.style.display = "none";
  }
}



// ------------------- Keyword Tag Input -------------------

const tags = [];

function handleTagInput(event) {
  const input = event.target;
  const value = input.value.trim();

  if ((event.key === 'Enter' || event.key === ',') && value) {
    event.preventDefault();
    if (!tags.includes(value)) {
      tags.push(value);
      renderTags();
      filterProperties(); // ðŸ” Trigger filter on tag add
    }
    input.value = '';
  }
}

function renderTags() {
  const container = document.getElementById('tagContainer');
  const input = document.getElementById('tagInput');

  // Clear all tags but preserve input field temporarily
  container.innerHTML = ''; // removes everything including input
  tags.forEach((tag, index) => {
    const tagElem = document.createElement('span');
    tagElem.className = 'tag';
    tagElem.innerHTML = `
      ${tag}
      <button class="remove-tag" onclick="removeTag(${index})">&times;</button>
    `;
    container.appendChild(tagElem);
  });

  container.appendChild(input); // safely re-append the input field at the end
}

function removeTag(index) {
  tags.splice(index, 1);
  renderTags();
  filterProperties(); // ðŸ” Trigger filter on tag remove
}




let currentPropertyFilters = {
  keyword: "",
  status: "",
  budget: "",
  createdBy: null,
  source: "",
  type: "",
  bhk: ""
};





function getPropertyFilterValue(id) {
  return (
    document.getElementById("desktop-" + id)?.value ||
    document.getElementById("mobile-" + id)?.value ||
    ''
  );
}

async function filterProperties(page = 0) {
  const companyId = parseInt(localStorage.getItem("companyId"), 10);
  const user = JSON.parse(localStorage.getItem("user") || "{}");
  const userId = user.userId;

  if (!companyId || isNaN(userId)) {
    customAlert("Company ID or User ID missing. Please login again.");
    return;
  }

  const keywordsArr = [...tags];
  const status = getPropertyFilterValue("statusFilter");
  const budgetRange = getPropertyFilterValue("budgetFilter");
  const createdByName = getPropertyFilterValue("createdByFilter");
  const source = getPropertyFilterValue("sourceFilter");
  const type = getPropertyFilterValue("typeFilter");
  const bhk = getPropertyFilterValue("bhkFilter");

  let minPrice = null, maxPrice = null;
  if (budgetRange) {
    const [min, max] = budgetRange.split("-").map(v => parseInt(v, 10));
    minPrice = !isNaN(min) ? min : null;
    maxPrice = !isNaN(max) ? max : null;
  }

  currentPropertyFilters = {
    keyword: keywordsArr.join(","),
    status,
    budget: budgetRange,
    createdBy: createdByName,
    source,
    type,
    bhk
  };

  const isEmpty = keywordsArr.length === 0 && !status && !budgetRange && !createdByName && !source && !type && !bhk;
  if (isEmpty) {
    await loadProperty(companyId, page);
    return;
  }

  const loader = document.getElementById("property-glass-loader");
  const tableBody = document.querySelector("#propertyTable tbody");
  if (loader) loader.style.display = "flex";
  if (tableBody) tableBody.innerHTML = "";

  const safeDisplay = (value) => {
    return value === null || value === undefined || value === "" ? "N/A" : value;
  };

  const formatToINR = (amount) => {
    if (amount === null || amount === undefined || isNaN(amount)) return 'N/A';
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount);
  };

  try {
    const params = new URLSearchParams();
    params.append("page", page);
    params.append("size", 10);

    keywordsArr.forEach(k => k && params.append("keywords", k));
    if (minPrice !== null) params.append("minPrice", minPrice);
    if (maxPrice !== null) params.append("maxPrice", maxPrice);
    if (status) params.append("status", status);
    if (type) params.append("type", type);
    if (bhk) params.append("bhk", bhk);
    if (source) params.append("source", source);
    if (createdByName) params.append("createdByName", createdByName);

    const url = `/api/companies/${companyId}/properties/search-paged?${params.toString()}`;

    const response = await fetch(url, {
      method: "GET",
      headers: { "Content-Type": "application/json" },
      credentials: "include"
    });

    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const { content: properties = [], totalPages = 1 } = await response.json();

    allProperty = properties;

    properties.forEach((property) => {
      const isOwner = property.createdBy?.userId === userId;
      const formattedPrice = formatToINR(property.price);

      let sourceDisplay = safeDisplay(property.source);
      if (property.source === "Reference" && property.referenceName) {
        sourceDisplay += ` (${safeDisplay(property.referenceName)})`;
      }

      let createdOn = "N/A";
      if (property.createdAt) {
        const date = new Date(property.createdAt);
        const day = date.getDate().toString().padStart(2, '0');
        const month = (date.getMonth() + 1).toString().padStart(2, '0');
        const year = date.getFullYear();
        const hours = date.getHours().toString().padStart(2, '0');
        const minutes = date.getMinutes().toString().padStart(2, '0');
        createdOn = `${day}/${month}/${year} ${hours}:${minutes}`;
      }

      const row = document.createElement("tr");
      row.innerHTML = `
        <td>${safeDisplay(property.propertyName)}</td>
        <td>${safeDisplay(property.type)}</td>
        <td>${safeDisplay(property.bhk)}</td>
        <td>${isOwner ? safeDisplay(property.unitDetails) : '🔒 Hidden'}</td>
        <td>${isOwner ? safeDisplay(property.floor) : '🔒 Hidden'}</td>
        <td>${safeDisplay(property.size)}</td>
        <td>${safeDisplay(property.sector)}</td>
        <td>${safeDisplay(property.location)}</td>
        <td>${safeDisplay(property.status)}</td>
        <td>${formattedPrice}</td>
        <td>${sourceDisplay}</td>
        <td>
          <button onclick="openAddPropertyRemarkModal(${property.propertyId})">Add</button>
          <button onclick="viewPropertyRemarks(${property.propertyId})">View</button>
        </td>
        <td>${safeDisplay(property.ownerName)}</td>
        <td>${isOwner ? safeDisplay(property.ownerContact) : '🔒 Hidden'}</td>
        <td>${safeDisplay(property.createdByName)}</td>
        <td>${createdOn}</td>
        <td>
          ${isOwner
            ? `<button onclick="handleShowUpdateProperty(this)" 
                        data-property='${encodeURIComponent(JSON.stringify(property))}' 
                        data-company-id="${companyId}">Update</button>`
            : `<button disabled style="opacity: 0.5; cursor: not-allowed;">Update</button>`}
        </td>
      `;
      tableBody.appendChild(row);
    });

    renderPropertyPaginationControls(companyId, totalPages, page);

  } catch (err) {
    console.error("❌ Error filtering properties:", err);
    customAlert("Something went wrong while filtering properties.");
  } finally {
    if (loader) loader.style.display = "none";
  }
}






function clearAllPropertyFilters() {
  const ids = ["budgetFilter", "createdByFilter", "statusFilter", "typeFilter", "sourceFilter", "bhkFilter"];

  ids.forEach(id => {
    const desktop = document.getElementById("desktop-" + id);
    const mobile = document.getElementById("mobile-" + id);
    if (desktop) desktop.value = "";
    if (mobile) mobile.value = "";
  });

  // Clear tags array and refresh UI
  if (typeof tags !== 'undefined' && Array.isArray(tags)) {
    tags.length = 0;
    renderTags();
  }

  // Reset search input if present
  const searchInput = document.getElementById("propertySearchInput");
  if (searchInput) searchInput.value = "";

  // Reload properties
  loadProperty(localStorage.getItem("companyId"), 0);
}





function renderPropertyPaginationControls(companyId, totalPages, currentPage) {
  const container = document.getElementById("paginationControls");
  if (!container) return;

  // Check if any filters are applied (including keyword array)
  const isFiltering = Object.values(currentPropertyFilters).some(val => {
    if (Array.isArray(val)) return val.length > 0;
    return val !== "" && val !== null;
  });

  const prevFunc = isFiltering
    ? `filterProperties(${currentPage - 1})`
    : `loadProperty(${companyId}, ${currentPage - 1})`;

  const nextFunc = isFiltering
    ? `filterProperties(${currentPage + 1})`
    : `loadProperty(${companyId}, ${currentPage + 1})`;

  container.innerHTML = `
    <button onclick="${prevFunc}" ${currentPage === 0 ? 'disabled' : ''}>Previous</button>
    <span style="margin: 0 10px;">Page ${currentPage + 1} of ${totalPages}</span>
    <button onclick="${nextFunc}" ${currentPage >= totalPages - 1 ? 'disabled' : ''}>Next</button>
  `;
}













//  <button onclick="deleteProperty(${property.propertyId})">Delete</button>


// Property Remark Logic




/* --- shared XSSâ€‘safe helper --------------------------------------------- */
function safeDisplay(value) {
  return value
    ? String(value).replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;")
    : "N/A";
}

/* ------------------------------------------------------------------------ */
/* 1) ADDâ€‘REMARK MODAL (optional propertyName param for nicer UX)           */
let currentPropertyId = null;

function openAddPropertyRemarkModal(propertyId) {
  currentPropertyId = propertyId;

   const property = allProperty.find(p => p.propertyId === propertyId);
  if (!property) {
    customAlert("Property not found. Please refresh the page.");
    return;
  }

  const headerHtml = `
    <div style="margin-bottom:15px;padding:10px;font-size:18px;font-weight:bold;color:black;border-bottom:2px solid #ccc;">
      Property: ${safeDisplay(property.propertyName)}
    </div>`;

  document.getElementById("propertyRemarkInput").value = "";
  document.getElementById("propertyModalBody").innerHTML = headerHtml;
  document.getElementById("propertyRemarkInputContainer").style.display = "block";
  document.getElementById("propertyRemarkModal").style.display = "block";
}

/* ------------------------------------------------------------------------ */
/* 2) VIEWâ€‘REMARKS MODAL                                                    */
function viewPropertyRemarks(propertyId) {
  currentPropertyId = propertyId;
  document.getElementById("propertyRemarkInputContainer").style.display = "none";

  const property = allProperty.find(p => p.propertyId === propertyId);
  if (!property) {
    customAlert("Property not found. Please refresh the page.");
    return;
  }

  const remarks = property.remarks || [];
  remarks.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

  let html = `
    <div style="margin-bottom:15px;padding:10px;font-size:18px;font-weight:bold;color:black;border-bottom:2px solid #ccc;">
      Property: ${safeDisplay(property.propertyName)}
    </div>`;

  if (remarks.length === 0) {
    html += "<p>No remarks found.</p>";
  } else {
    html += `
      <div style="max-height: 300px; overflow-y: auto; border: 1px solid #ccc; border-radius: 4px;">
        <table style="width:100%; border-collapse:collapse;">
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

      html += `
        <tr>
          <td style="padding:10px; border-bottom:1px solid #eee; color:black; white-space:normal; word-break:break-word;">${safeDisplay(r.remark)}</td>
          <td style="padding:10px; border-bottom:1px solid #eee; color:black;">${createdDateTime}</td>
          <td style="padding:10px; border-bottom:1px solid #eee; color:black;">${safeDisplay(createdBy)}</td>
        </tr>`;
    });

    html += `</tbody></table></div>`;
  }

  document.getElementById("propertyModalBody").innerHTML = html;
  document.getElementById("propertyRemarkModal").style.display = "block";
}



function submitPropertyRemark() {
  const remark = document.getElementById("propertyRemarkInput").value.trim();
  const companyId = parseInt(localStorage.getItem("companyId"), 10);
  const user = JSON.parse(localStorage.getItem("user") || "{}");
  const userId = user.userId;

  if (!companyId) return customAlert("Company ID not found in local storage!");
  if (!userId) return customAlert("User ID not found. Please login again.");
  if (!remark) return customAlert("Remark cannot be empty!");

  fetch(`/api/companies/${companyId}/properties/${currentPropertyId}/remarks`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ remark, userId })
  })
    .then(res => {
      if (!res.ok) throw new Error("Failed to add remark");
      return res.text(); // assuming backend returns plain message
    })
    .then(async message => {
      customAlert(message); // e.g. "Remark added successfully!"
      await filterProperties(propertyCurrentPage);
            document.getElementById("propertyRemarkModal").style.display = "none"; // ✅ modal close

       // refresh property list   
    })
    .catch(err => customAlert(err.message));
}

function closePropertyRemarkModal() {
  document.getElementById("propertyRemarkModal").style.display = "none";
  currentPropertyId = null;
}




function handleShowUpdateProperty(btn) {
  try {
    const propertyData = btn.getAttribute("data-property");
    // console.log("Encoded data-property:", propertyData);

    const decodedData = decodeURIComponent(propertyData);
    // console.log("Decoded data-property:", decodedData);

    const property = JSON.parse(decodedData);
    // console.log("Parsed Property Object:", property);

    showUpdatePropertyModal(property);
  } catch (error) {
    console.error("Failed to parse property data:", error);

  }
}

function showUpdatePropertyModal(property) {
  // console.log("Opening update modal for property:", property);

  const existingModal = document.querySelector('.modal2');
  if (existingModal) existingModal.remove();

  const modal = document.createElement("div");
  modal.className = "modal2";

  modal.innerHTML = `
    <div class="modal-content1">
      <span class="close">×</span>
      <h2>Update Property</h2>
      <form id="updatePropertyForm">
        <div>
          <label for="updatePropertyName">Property Name:</label>
          <input type="text" id="updatePropertyName" name="propertyName" value="${property.propertyName || ''}" required>
        </div>
        <div>
          <label for="updateType">Type:</label>
          <select id="updateType" name="type" required>
            <option value="Office" ${property.type === 'Office' ? 'selected' : ''}>Office</option>
            <option value="Retail" ${property.type === 'Retail' ? 'selected' : ''}>Retail</option>
            <option value="Residential" ${property.type === 'Residential' ? 'selected' : ''}>Residential</option>
            <option value="Plot" ${property.type === 'Plot' ? 'selected' : ''}>Plot</option>
          </select>
        </div>
        <div>
          <label for="updateBhk">BHK:</label>
          <input type="text" id="updateBhk" name="bhk" placeholder="e.g., 2BHK"  />
        </div>

        <div>
          <label for="updateUnit">Unit Details:</label>
              <input type="text" id="updateUnit" name="unitDetails" placeholder="Unit Details" value="${property.unitDetails || ''}" />
        </div>
         <div>
          <label for="updateFloor">Floor:</label>
              <input type="text" id="updateFloor" name="updateFloor" placeholder="Floor" value="${property.floor || ''}" />
        </div>
        <div>
          <label for="updateSize">Size (sqft):</label>
          <input type="number" id="updateSize" name="size" value="${property.size || ''}" >
        </div>
        <div>
          <label for="updateOwnerName">Owner Name:</label>
          <input type="text" id="updateOwnerName" name="ownerName" value="${property.ownerName || ''}" >
        </div>
        <div>
          <label for="updateLocation">Location:</label>
          <input type="text" id="updateLocation" name="location" value="${property.location || ''}" >
        </div>
        <div>
          <label for="updateOwnerContact">Owner Contact:</label>
          <input type="text" id="updateOwnerContact" name="ownerContact" value="${property.ownerContact || ''}" >
        </div>
        <div>
          <label for="updatePrice">Price:</label>
          <input type="text" id="updatePrice" name="price" value="${property.price || ''}" >
        </div>
        <div>
          <label for="updateStatus">Status:</label>
          <select id="updateStatus" name="status" required>
            <option value="AVAILABLE_FOR_SALE" ${property.status === 'AVAILABLE_FOR_SALE' ? 'selected' : ''}>Available for Sale</option>
            <option value="AVAILABLE_FOR_RENT" ${property.status === 'AVAILABLE_FOR_RENT' ? 'selected' : ''}>Available for Rent</option>
            <option value="RENT_OUT" ${property.status === 'RENT_OUT' ? 'selected' : ''}>Rent Out</option>
            <option value="SOLD_OUT" ${property.status === 'SOLD_OUT' ? 'selected' : ''}>Sold Out</option>
          </select>
        </div>
        <div>
          <label for="updateSector">Sector:</label>
          <input type="text" id="updateSector" name="sector" value="${property.sector || ''}" required>
        </div>
        <div style="display: flex; justify-content: center;">
          <button type="submit">Update Property</button>
        </div>
      </form>
    </div>
  `;

  document.body.appendChild(modal);
  // console.log("Modal appended to body.");

  // Close modal event handler
  modal.querySelector('.close').addEventListener('click', () => modal.remove());

  const updateTypeSelect = modal.querySelector('#updateType');
  const bhkInput = modal.querySelector('#updateBhk');

  // Function to update BHK input state and value
function updateBhkState() {
  const type = updateTypeSelect.value;

  if (type === 'Office' || type === 'Retail') {
    bhkInput.readOnly = true;
    bhkInput.value = "";
  } else {
    bhkInput.readOnly = false;
    bhkInput.value = property.bhk || '';
  }
}



  // Initialize state
  updateBhkState();

  // Listener on type change
  updateTypeSelect.addEventListener('change', updateBhkState);

  // Submit handler
  document.getElementById("updatePropertyForm").addEventListener("submit", (e) => {
    e.preventDefault();
    // console.log("Update form submitted for property ID:", property.propertyId);
    submitUpdatedProperty(property.propertyId);
  });
}


async function submitUpdatedProperty(propertyId) {
  const companyId = parseInt(localStorage.getItem("companyId"), 10);

  // Helper to safely get trimmed input value or null if empty
  function getInputValue(id) {
    const el = document.getElementById(id);
    if (!el) return null;
    const val = el.value.trim();
    return val === "" ? null : val;
  }

  const updateType = getInputValue("updateType") || null;

  // Handle BHK for Commercial type
  let bhkValue = null;
  if (updateType !== "Commercial") {
    const bhkRaw = getInputValue("updateBhk");
    bhkValue = bhkRaw || null;
  }

  // Prepare updated object with nulls for empty fields to avoid sending undefined
  const updated = {
    propertyName: getInputValue("updatePropertyName"),
    type: updateType,
    bhk: bhkValue,
    unitDetails: getInputValue("updateUnit"),
    floor: getInputValue("updateFloor"),
    size: (() => {
      const val = document.getElementById("updateSize")?.value;
      if (!val) return null;
      const parsed = parseInt(val);
      return isNaN(parsed) ? null : parsed;
    })(),
    ownerName: getInputValue("updateOwnerName"),
    ownerContact: getInputValue("updateOwnerContact"),
    price: getInputValue("updatePrice"),
    sector: getInputValue("updateSector"),
    status: getInputValue("updateStatus"),
    location:getInputValue("updateLocation"),
  };

  // console.log("Updated data to send:", updated);

  try {
    const res = await fetch(`/api/companies/${companyId}/properties/${propertyId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify(updated)
    });

    if (!res.ok) {
      const errorText = await res.text();
      throw new Error(`Update failed: ${res.status} ${res.statusText} - ${errorText}`);
    }

    customAlert("Property updated successfully");
    document.querySelector(".modal2")?.remove();
    await filterProperties(currentPage);
  } catch (err) {
    customAlert("Error: " + err.message);
  }
}



// Delete property
async function deleteProperty(propertyId) {
  const companyId = parseInt(localStorage.getItem("companyId"), 10);

  if (!confirm("Delete this property?")) return;
  try {
    const res = await fetch(`/api/companies/${companyId}/properties/${propertyId}`, {
      method: "DELETE",
      headers: { "Content-Type": "application/json" },
      credentials: "include"
    });
    if (!res.ok) throw new Error("Delete failed");
    customAlert("Property deleted");
    await filterProperties(currentPage);
  } catch (err) {
    customAlert(err.message);
  }
}