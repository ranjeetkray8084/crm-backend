function toggleSidebar() {
  const sidebar = document.getElementById("sidebar");
  const overlay = document.getElementById("overlay");

  sidebar.classList.toggle("active");

  // Show or hide overlay
  if (sidebar.classList.contains("active")) {
    overlay.classList.add("active");
    document.body.style.overflow = "hidden"; // Prevent scroll
  } else {
    overlay.classList.remove("active");
    document.body.style.overflow = ""; // Restore scroll
  }
}
function toggleSidebar() {
  const sidebar = document.getElementById("sidebar");
  const overlay = document.getElementById("overlay");

  sidebar.classList.toggle("active");

  // Show or hide overlay
  if (sidebar.classList.contains("active")) {
    overlay.classList.add("active");
    document.body.style.overflow = "hidden"; // Prevent scroll
  } else {
    overlay.classList.remove("active");
    document.body.style.overflow = ""; // Restore scroll
  }
}


document.addEventListener("click", function (event) {
  const sidebar = document.getElementById("sidebar");
  const overlay = document.getElementById("overlay");
  const toggleButton = document.querySelector(".sidebar-toggle");

  // If sidebar is active and click is outside both sidebar and toggle button
  if (
    sidebar.classList.contains("active") &&
    !sidebar.contains(event.target) &&
    !toggleButton.contains(event.target)
  ) {
    sidebar.classList.remove("active");
    overlay.classList.remove("active");
    document.body.style.overflow = "";
  }
});
