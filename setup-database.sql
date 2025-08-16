-- MySQL Database Setup Script for Real Estate CRM
-- Run this script as MySQL root user

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS chandra_realtors_copy;

-- Create user if not exists (adjust password as needed)
CREATE USER IF NOT EXISTS 'root'@'localhost' IDENTIFIED BY '1234';

-- Grant privileges to the user
GRANT ALL PRIVILEGES ON real_estate_crm.* TO 'root'@'localhost';



