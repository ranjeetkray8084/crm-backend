-- Quick Fix: Create Missing Announcements Table
-- Run this script to create the missing announcements table
-- Database: chandra_realtors

-- Create announcements table if it doesn't exist
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
    INDEX idx_company_active (company_id, is_active),
    INDEX idx_priority (priority),
    INDEX idx_expires (expires_at)
);

-- Verify table was created
SHOW TABLES LIKE 'announcements';
DESCRIBE announcements;

-- Insert a sample announcement (optional)
-- INSERT INTO announcements (title, message, company_id, created_by, priority) 
-- VALUES ('Welcome to CRM App', 'Welcome to your new CRM application! This is your first announcement.', 1, 1, 'NORMAL');

SELECT 'Announcements table created successfully!' as status;
