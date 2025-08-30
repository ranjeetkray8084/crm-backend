# Firebase Setup Guide

## Setting up Firebase credentials for push notifications

1. **Download your Firebase service account key:**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Select your project
   - Go to Project Settings > Service Accounts
   - Click "Generate New Private Key"
   - Download the JSON file

2. **Place the credentials file:**
   - Rename the downloaded file to `firebase-service-account.json`
   - Place it in `src/main/resources/` directory

3. **Important Security Notes:**
   - Never commit this file to git
   - The file is already in `.gitignore`
   - Keep your credentials secure and don't share them

4. **File Structure:**
   ```
   src/main/resources/
   ├── firebase-service-account.json (your actual credentials - NOT tracked by git)
   └── firebase-service-account.template.json (template file - tracked by git)
   ```

5. **For Production:**
   - Use environment variables or secure secret management
   - Don't store credentials in the repository
