-- Push Notifications and Announcements Setup
-- Run this script in your database to create the required tables

-- Create push_tokens table
CREATE TABLE IF NOT EXISTS push_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    push_token VARCHAR(500) NOT NULL,
    device_type VARCHAR(20) DEFAULT 'android',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_device (user_id, device_type),
    INDEX idx_active_tokens (is_active)
);

-- Create announcements table
CREATE TABLE IF NOT EXISTS announcements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    company_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    priority ENUM('LOW', 'NORMAL', 'HIGH', 'URGENT') DEFAULT 'NORMAL',
    is_active BOOLEAN DEFAULT TRUE,
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES company(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_company_active (company_id, is_active),
    INDEX idx_priority (priority),
    INDEX idx_expires (expires_at)
);

-- Add indexes for better performance
CREATE INDEX IF NOT EXISTS idx_push_tokens_user ON push_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_push_tokens_active ON push_tokens(is_active);
CREATE INDEX IF NOT EXISTS idx_announcements_company ON announcements(company_id);
CREATE INDEX IF NOT EXISTS idx_announcements_created_by ON announcements(created_by);
CREATE INDEX IF NOT EXISTS idx_announcements_priority ON announcements(priority);

-- Insert sample announcement (optional)
-- INSERT INTO announcements (title, message, company_id, created_by, priority) 
-- VALUES ('Welcome to CRM App', 'Welcome to your new CRM application! This is your first announcement.', 1, 1, 'NORMAL');

-- Show created tables
SHOW TABLES LIKE 'push_tokens';
SHOW TABLES LIKE 'announcements';

-- Show table structure
DESCRIBE push_tokens;
DESCRIBE announcements;
