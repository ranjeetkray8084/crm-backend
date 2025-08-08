function revokeUser(userId) {
  fetch(`/api/users/${userId}/revoke`, { method: "PUT" })
    .then(() => loadUsers())
    .catch(err => console.error("Error deactivating user:", err));
}

function activateUser(userId) {
  fetch(`/api/users/${userId}/unrevoke`, { method: "PUT" })
    .then(() => loadUsers())
    .catch(err => console.error("Error activating user:", err));
}




function revokeAdmin(userId) {
  fetch(`/api/users/${userId}/revoke`, { method: "PUT" })
    .then(() => loadAdmins())
    .catch(err => console.error("Error revoking admin:", err));
}

function activateAdmin(userId) {
  fetch(`/api/users/${userId}/unrevoke`, { method: "PUT" })
    .then(() => loadAdmins())
    .catch(err => console.error("Error activating admin:", err));
}

