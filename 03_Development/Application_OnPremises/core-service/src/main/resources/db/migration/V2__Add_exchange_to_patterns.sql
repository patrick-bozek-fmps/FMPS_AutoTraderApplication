-- =====================================================================
-- MIGRATION: V2__Add_exchange_to_patterns.sql
-- Description: Add exchange column to patterns table for multi-exchange support
-- Date: November 6, 2025
-- Issue: #15 - Pattern Storage System
-- =====================================================================

-- Add exchange column to patterns table
-- This enables proper pattern matching across different exchanges (BINANCE, BITGET)
ALTER TABLE patterns ADD COLUMN exchange VARCHAR(20) DEFAULT 'BINANCE';

-- Update existing patterns to have BINANCE as default (backward compatibility)
UPDATE patterns SET exchange = 'BINANCE' WHERE exchange IS NULL;

-- Add index for exchange column to improve query performance
CREATE INDEX idx_patterns_exchange ON patterns(exchange);

-- Add composite index for common query pattern: exchange + trading_pair
CREATE INDEX idx_patterns_exchange_trading_pair ON patterns(exchange, trading_pair);

-- Add constraint to ensure exchange is one of the valid values
-- Note: SQLite doesn't support CHECK constraints on ALTER TABLE, so this is informational
-- The application layer should validate exchange values

-- =====================================================================
-- Migration Notes:
-- - This migration adds support for multi-exchange pattern storage
-- - Existing patterns default to BINANCE for backward compatibility
-- - The PatternService will now use the stored exchange instead of defaulting
-- - PatternRepository.create() method should be updated to accept exchange parameter
-- - PatternService.convertToTradingPattern() should use stored exchange
-- =====================================================================

