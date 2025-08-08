 fetch("footer.html")
      .then(response => response.text())
      .then(data => {
        document.getElementById("footer").innerHTML = data;
      })
      .catch(error => {
        console.error("Error loading footer:", error);
      });


      function openModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.style.display = 'block';
}

function closeModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.style.display = 'none';
}

// Updated to reflect correct modal IDs
window.onclick = function(event) {
  ['helpModal', 'crmcontactModal', 'privacyModal'].forEach(id => {
    const modal = document.getElementById(id);
    if (event.target === modal) closeModal(id);
  });
};

function submitContactForm(event) {
  event.preventDefault();
  document.getElementById('thankYouMsg').style.display = 'block';
  setTimeout(() => closeModal('crmcontactModal'), 2500); // Fixed ID
}
