# 🔔 Push Notifications Setup Guide

## Overview
This guide explains how to set up and use push notifications in your CRM backend. The system automatically sends push notifications for all existing notifications and provides new endpoints for announcements.

## 🚀 What's Been Added

### New Models
- **PushToken**: Stores user's push notification tokens
- **Announcement**: Company-wide announcements with push notifications

### New Services
- **ExpoPushNotificationService**: Handles sending notifications via Expo
- **PushTokenService**: Manages push token registration and retrieval

### New Controllers
- **PushNotificationController**: Push token management and sending
- **AnnouncementController**: Company announcements with push notifications

### Updated Services
- **NotificationService**: Now automatically sends push notifications

## 📱 How It Works

### 1. Automatic Push Notifications
All existing notifications (leads, tasks, follow-ups) now automatically send push notifications to users' devices.

### 2. Push Token Registration
Users register their push tokens when they open the mobile app:
```http
POST /api/push-notifications/register
{
  "pushToken": "ExponentPushToken[...]",
  "deviceType": "android"
}
```

### 3. Announcements
Admins can create company-wide announcements that automatically send push notifications:
```http
POST /api/announcements
{
  "title": "Important Update",
  "message": "New features are available!",
  "priority": "HIGH",
  "sendPushNotification": true
}
```

## 🗄️ Database Setup

### Run the SQL Script
Execute `push-notifications-setup.sql` in your database to create the required tables.

### Tables Created
- `push_tokens`: User device tokens
- `announcements`: Company announcements

## 🔧 Configuration

### Dependencies Added
- OkHttp for HTTP requests
- Jackson for JSON processing

### Environment Variables
No additional environment variables required.

## 📋 API Endpoints

### Push Notifications
- `POST /api/push-notifications/register` - Register push token
- `POST /api/push-notifications/send` - Send custom notification
- `POST /api/push-notifications/test` - Test notification
- `GET /api/push-notifications/tokens` - Get user's tokens
- `DELETE /api/push-notifications/tokens/{id}` - Deactivate token

### Announcements
- `POST /api/announcements` - Create announcement
- `GET /api/announcements` - Get company announcements
- `GET /api/announcements/{id}` - Get specific announcement
- `PUT /api/announcements/{id}` - Update announcement
- `DELETE /api/announcements/{id}` - Delete announcement
- `GET /api/announcements/priority/{priority}` - Get by priority

## 🔔 Notification Types

### Automatic Notifications
- **Lead Updates**: New leads, status changes, assignments
- **Task Reminders**: Due dates, assignments
- **Follow-up Reminders**: Scheduled follow-ups
- **Property Updates**: New properties, price changes

### Manual Notifications
- **Announcements**: Company-wide messages
- **Custom Notifications**: Send to specific users or company

## 🧪 Testing

### Test Push Notification
```http
POST /api/push-notifications/test
{
  "pushToken": "your-test-token",
  "deviceType": "android"
}
```

### Test Announcement
```http
POST /api/announcements
{
  "title": "Test Announcement",
  "message": "This is a test announcement",
  "priority": "NORMAL",
  "sendPushNotification": true
}
```

## 🔍 Monitoring

### Logs
All push notification activities are logged with emojis:
- ✅ Success operations
- ❌ Error operations
- 🔔 Push notifications sent
- 📱 Device information
- 🧪 Test operations

### Check Logs
Look for these log patterns:
- `🔔 Push notification sent to X users`
- `✅ Push token registered for user: email`
- `❌ Failed to send push notification`

## 🚨 Troubleshooting

### Common Issues

1. **Push Token Not Registered**
   - Check if user has registered token
   - Verify token format is correct

2. **Notifications Not Sent**
   - Check user has active push tokens
   - Verify notification permissions

3. **Database Errors**
   - Run the SQL setup script
   - Check table structure

### Debug Steps

1. Check application logs for errors
2. Verify database tables exist
3. Test with a simple notification
4. Check user's push token status

## 🔮 Future Enhancements

- **Rich Notifications**: Images, actions, custom layouts
- **Notification Groups**: Organize related notifications
- **Custom Sounds**: App-specific notification sounds
- **Deep Linking**: Navigate to specific screens
- **Analytics**: Track notification delivery and engagement

## 📚 Resources

- [Expo Push Notifications](https://docs.expo.dev/versions/latest/sdk/notifications/)
- [Spring Boot REST API](https://spring.io/guides/gs/rest-service/)
- [MySQL Documentation](https://dev.mysql.com/doc/)

## 🤝 Support

If you encounter issues:
1. Check the logs for error messages
2. Verify database setup
3. Test with simple endpoints
4. Check user permissions and tokens

---

**Note**: This system integrates seamlessly with your existing notification system. All current notifications will automatically send push notifications without any code changes required.
