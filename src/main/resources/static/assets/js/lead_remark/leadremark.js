// // let allLeads = [];


// function openAddLeadRemarkModal(leadId) {
//   currentLeadId = leadId;
//   document.getElementById("leadRemarkInput").value = "";
//   document.getElementById("leadModalBody").innerHTML = "";
//   document.getElementById("leadRemarkInputContainer").style.display = "block";
//   document.getElementById("leadRemarkModal").style.display = "block";
// }


// function safeDisplay(value) {
//   return value ? String(value).replace(/</g, "&lt;").replace(/>/g, "&gt;") : "N/A";
// }

// function viewLeadRemarks(leadId) {
//   currentLeadId = leadId;
//   document.getElementById("leadRemarkInputContainer").style.display = "none";

//   const lead = allLeads.find(l => l.leadId === leadId);
//   if (!lead) {
//     customAlert("Lead not found. Please refresh the page.");
//     return;
//   }

//   const remarks = lead.remarks || [];
//   remarks.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

//   let html = `
//     <div style="margin-bottom:15px;padding:10px;font-size:18px;font-weight:bold;color:black;border-bottom:2px solid #ccc;">
//       Lead Name: ${safeDisplay(lead.name)}
//     </div>`;

//   if (remarks.length === 0) {
//     html += "<p>No remarks found.</p>";
//   } else {
//     html += `
//       <div style="max-height: 300px; overflow-y: auto; border: 1px solid #ccc; border-radius: 4px;">
//         <table style="width:100%; border-collapse:collapse;">
//           <thead style="position: sticky; top: 0; background-color: #f0f0f0; z-index: 1;">
//             <tr>
//               <th style="padding:10px; border-bottom:1px solid #ccc; color:black;">Remark</th>
//               <th style="padding:10px; border-bottom:1px solid #ccc; color:black;">Date & Time</th>
//               <th style="padding:10px; border-bottom:1px solid #ccc; color:black;">Created By</th>
//             </tr>
//           </thead>
//           <tbody>`;

//     remarks.forEach(r => {
//       const createdDateTime = r.createdAt
//         ? new Date(r.createdAt).toLocaleString(undefined, {
//             year: 'numeric', month: 'short', day: 'numeric',
//             hour: '2-digit', minute: '2-digit', hour12: true
//           })
//         : "-";

//       const createdBy = r.createdBy?.name || "Unknown";

//       // // ⬇️ Word wrap after every 15 words
//       // const wrappedRemark = safeDisplay(r.remark)
//       //   .split(" ")
//       //   .reduce((acc, word, index) => {
//       //     acc += word + " ";
//       //     if ((index + 1) % 15 === 0) acc += "<br>";
//       //     return acc;
//       //   }, "").trim();

//       html += `
//             <tr>
//               <td style="padding:10px; border-bottom:1px solid #eee; color:black; white-space:normal; word-break:break-word;">${r.remark}</td>
//               <td style="padding:10px; border-bottom:1px solid #eee; color:black;">${createdDateTime}</td>
//               <td style="padding:10px; border-bottom:1px solid #eee; color:black;">${safeDisplay(createdBy)}</td>
//             </tr>`;
//     });

//     html += `</tbody></table></div>`;
//   }

//   document.getElementById("leadModalBody").innerHTML = html;
//   document.getElementById("leadRemarkModal").style.display = "block";
// }







// function submitLeadRemark() {
//   const remark = document.getElementById("leadRemarkInput").value.trim();
//   const companyId = parseInt(localStorage.getItem("companyId"), 10);
//   const user = JSON.parse(localStorage.getItem("user") || "{}");
//   const userId = user.userId;

//   if (!companyId) return customAlert("Company ID not found in local storage!");
//   if (!userId) return customAlert("User ID not found. Please login again.");
//   if (!remark) return customAlert("Remark cannot be empty!");

//   fetch(`/api/companies/${companyId}/leads/${currentLeadId}/remarks`, {
//     method: "POST",
//     headers: { "Content-Type": "application/json" },
//      credentials: 'include',
//     body: JSON.stringify({ remark, userId })
//   })
//     .then(res => {
//       if (!res.ok) throw new Error("Failed to add remark");
//       return res.text(); // 🔁 use text instead of json
//     })
//     .then(message => {
//       // No newRemark pushed here since backend didn't send it
//       customAlert(message); // ✅ show: Remark added successfully
//       loadLeads();
//     })
//     .catch(err => customAlert(err.message));
// }


// function closeLeadRemarkModal() {
//   document.getElementById("leadRemarkModal").style.display = "none";
//   currentLeadId = null;
// }