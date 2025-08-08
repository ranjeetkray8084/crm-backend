// image.js

// Function to upload image
function uploadImage(userId, file, callback) {
  const formData = new FormData();
  formData.append("avatar", file);
  formData.append("avatarName", file.name);

  fetch(`/api/users/${userId}/upload-avatar`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: formData
  })
    .then(response => {
      if (!response.ok) throw new Error("Upload failed");
      return response.text();
    })
    .then(message => {
      callback(null, message); // Success callback
    })
    .catch(error => {
      callback(error, null); // Error callback
    });
}

// Function to fetch avatar image by userId
function fetchAvatar(userId, callback) {
  fetch(`/api/users/${userId}/avatar`, {
    method: "GET",
    headers: { "Content-Type": "application/json" },
    credentials: "include"
  })
    .then(response => {
      if (!response.ok) {
        throw new Error("Failed to fetch avatar.");
      }
      return response.blob();
    })
    .then(imageBlob => {
      z
      const imageUrl = URL.createObjectURL(imageBlob);
      callback(null, imageUrl); // Success callback
    })
    .catch(error => {
      callback(error, "assets/default-avatar.png"); // Fallback image
    });
}
