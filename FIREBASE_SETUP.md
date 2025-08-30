# 🔥 Firebase Push Notification Setup

## Overview
This guide will help you set up Firebase Cloud Messaging (FCM) for push notifications in your CRM backend.

## 🚨 **IMPORTANT: You Need the LEGACY SERVER KEY, Not the Service Account Key!**

### **What You Currently Have (Wrong):**
- ✅ Service Account JSON file
- ✅ Project ID: `crmnativeexpo`
- ❌ **Wrong Key**: You're using the `private_key_id` which won't work for FCM

### **What You Need (Correct):**
- 🔑 **Legacy Server Key** from Firebase Console

## 🚀 **Quick Setup - Get the Correct Key:**

### **Step 1: Go to Firebase Console**
1. Visit [Firebase Console](https://console.firebase.google.com/)
2. Select your project: `crmnativeexpo`

### **Step 2: Get Legacy Server Key**
1. Click **Project Settings** (gear icon)
2. Go to **Cloud Messaging** tab
3. Look for **"Server key"** section
4. **Copy the Legacy Server Key** (starts with `AAAA...`)

### **Step 3: Update Backend Configuration**
Replace `YOUR_LEGACY_SERVER_KEY_HERE` in `application.properties`:

```properties
firebase.server.key=AAAA...your_actual_legacy_server_key_here
```

## 🔍 **How to Identify the Correct Key:**

### **✅ Correct Key (Legacy Server Key):**
- Starts with `AAAA...`
- Usually 152 characters long
- Found in **Cloud Messaging** tab
- Used for **FCM HTTP API**

### **❌ Wrong Key (Private Key ID):**
- Starts with `7f6d1a...`
- Only 40 characters long
- Found in **Service Accounts** tab
- Used for **Admin SDK authentication**

## 🧪 **Test After Setup:**

1. **Restart your backend**
2. **Test status API:**
   ```
   GET http://localhost:8082/api/push-notifications/status
   ```
3. **Test push notification:**
   ```
   POST http://localhost:8082/api/push-notifications/test
   ```

## 🎯 **Expected Result:**
```json
{
  "status": {
    "firebaseServerKeyConfigured": true,
    "firebaseServerKeyLength": 152,
    "firebaseProjectIdConfigured": true
  }
}
```

## 🚨 **Common Mistakes:**
1. **Using private_key_id instead of server key**
2. **Using service account private key**
3. **Not restarting backend after config change**

**Get the Legacy Server Key from Cloud Messaging tab and your push notifications will work!** 🎉
