-- Fix visibility column in notes table
-- This migration will handle existing data and update the column definition

-- First, update any NULL or invalid visibility values
UPDATE notes SET visibility = 'ONLY_ME' WHERE visibility IS NULL OR visibility = '';

-- Now safely modify the column to include all enum values
-- We need to do this in steps to avoid data truncation

-- Step 1: Add new enum values to the existing column
ALTER TABLE notes MODIFY COLUMN visibility ENUM(
    'ONLY_ME',
    'ME_AND_ADMIN', 
    'ALL_USERS',
    'SPECIFIC_USERS',
    'ME_AND_DIRECTOR',
    'ALL_ADMIN',
    'SPECIFIC_ADMIN'
) NOT NULL DEFAULT 'ONLY_ME';