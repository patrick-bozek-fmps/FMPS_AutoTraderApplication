-- FMPS AutoTrader Database Schema v1.0
-- Migration: V1__Initial_schema.sql
-- Description: Initial database schema for AI traders, trades, patterns, configurations, and exchange accounts

-- =====================================================================
-- TABLE: ai_traders
-- Description: AI Trader instances (max 3 concurrent)
-- =====================================================================
CREATE TABLE ai_traders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'STOPPED',
    
    -- Trading Configuration
    exchange VARCHAR(20) NOT NULL,
    trading_pair VARCHAR(20) NOT NULL,
    leverage INTEGER NOT NULL DEFAULT 10,
    initial_balance DECIMAL(18, 8) NOT NULL,
    current_balance DECIMAL(18, 8) NOT NULL,
    
    -- Risk Management
    stop_loss_percent DECIMAL(5, 4) NOT NULL DEFAULT 0.02,
    take_profit_percent DECIMAL(5, 4) NOT NULL DEFAULT 0.05,
    max_daily_loss DECIMAL(5, 4) NOT NULL DEFAULT 0.10,
    
    -- Technical Indicators Config
    rsi_period INTEGER NOT NULL DEFAULT 14,
    rsi_overbought INTEGER NOT NULL DEFAULT 70,
    rsi_oversold INTEGER NOT NULL DEFAULT 30,
    macd_fast INTEGER NOT NULL DEFAULT 12,
    macd_slow INTEGER NOT NULL DEFAULT 26,
    macd_signal INTEGER NOT NULL DEFAULT 9,
    sma_short INTEGER NOT NULL DEFAULT 20,
    sma_long INTEGER NOT NULL DEFAULT 50,
    
    -- Timestamps
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active_at DATETIME,
    
    -- Constraints
    CHECK (leverage > 0 AND leverage <= 125),
    CHECK (status IN ('ACTIVE', 'PAUSED', 'STOPPED')),
    CHECK (exchange IN ('BINANCE', 'BITGET', 'TRADINGVIEW'))
);

CREATE INDEX idx_ai_traders_status ON ai_traders(status);
CREATE INDEX idx_ai_traders_exchange ON ai_traders(exchange);

-- =====================================================================
-- TABLE: trades
-- Description: Trade history (demo/virtual money only for v1.0)
-- =====================================================================
CREATE TABLE trades (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ai_trader_id INTEGER NOT NULL,
    
    -- Trade Details
    trade_type VARCHAR(10) NOT NULL,
    exchange VARCHAR(20) NOT NULL,
    trading_pair VARCHAR(20) NOT NULL,
    leverage INTEGER NOT NULL,
    
    -- Entry
    entry_price DECIMAL(18, 8) NOT NULL,
    entry_amount DECIMAL(18, 8) NOT NULL,
    entry_timestamp DATETIME NOT NULL,
    entry_order_id VARCHAR(100),
    
    -- Exit
    exit_price DECIMAL(18, 8),
    exit_amount DECIMAL(18, 8),
    exit_timestamp DATETIME,
    exit_order_id VARCHAR(100),
    exit_reason VARCHAR(50),
    
    -- P&L
    profit_loss DECIMAL(18, 8),
    profit_loss_percent DECIMAL(10, 6),
    fees DECIMAL(18, 8) NOT NULL DEFAULT 0,
    
    -- Risk Management
    stop_loss_price DECIMAL(18, 8) NOT NULL,
    take_profit_price DECIMAL(18, 8) NOT NULL,
    trailing_stop_activated BOOLEAN NOT NULL DEFAULT 0,
    
    -- Technical Indicators at Entry
    rsi_value DECIMAL(5, 2),
    macd_value DECIMAL(10, 4),
    sma_short_value DECIMAL(18, 8),
    sma_long_value DECIMAL(18, 8),
    
    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    
    -- Metadata
    notes TEXT,
    pattern_id INTEGER,
    
    -- Timestamps
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CHECK (trade_type IN ('LONG', 'SHORT')),
    CHECK (status IN ('OPEN', 'CLOSED', 'CANCELLED')),
    CHECK (exit_reason IS NULL OR exit_reason IN ('TAKE_PROFIT', 'STOP_LOSS', 'MANUAL', 'SIGNAL', 'MAX_LOSS')),
    FOREIGN KEY (ai_trader_id) REFERENCES ai_traders(id) ON DELETE CASCADE
);

CREATE INDEX idx_trades_ai_trader ON trades(ai_trader_id);
CREATE INDEX idx_trades_status ON trades(status);
CREATE INDEX idx_trades_entry_timestamp ON trades(entry_timestamp);
CREATE INDEX idx_trades_trading_pair ON trades(trading_pair);
CREATE INDEX idx_trades_pattern_id ON trades(pattern_id);

-- =====================================================================
-- TABLE: patterns
-- Description: Successful trading patterns (simple pattern storage)
-- =====================================================================
CREATE TABLE patterns (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100),
    pattern_type VARCHAR(50) NOT NULL,
    
    -- Market Conditions
    trading_pair VARCHAR(20) NOT NULL,
    timeframe VARCHAR(10) NOT NULL,
    trade_type VARCHAR(10) NOT NULL,
    
    -- Technical Indicator Ranges
    rsi_min DECIMAL(5, 2),
    rsi_max DECIMAL(5, 2),
    macd_min DECIMAL(10, 4),
    macd_max DECIMAL(10, 4),
    volume_change_min DECIMAL(10, 4),
    volume_change_max DECIMAL(10, 4),
    price_change_min DECIMAL(10, 6),
    price_change_max DECIMAL(10, 6),
    
    -- Outcome Statistics
    total_occurrences INTEGER NOT NULL DEFAULT 0,
    successful_trades INTEGER NOT NULL DEFAULT 0,
    failed_trades INTEGER NOT NULL DEFAULT 0,
    success_rate DECIMAL(5, 2) NOT NULL DEFAULT 0,
    average_profit_loss DECIMAL(10, 6) NOT NULL DEFAULT 0,
    best_profit_loss DECIMAL(10, 6) NOT NULL DEFAULT 0,
    worst_profit_loss DECIMAL(10, 6) NOT NULL DEFAULT 0,
    
    -- Confidence Metrics
    confidence DECIMAL(5, 2) NOT NULL DEFAULT 0,
    min_occurrences INTEGER NOT NULL DEFAULT 10,
    
    -- Status
    is_active BOOLEAN NOT NULL DEFAULT 1,
    
    -- Metadata
    description TEXT,
    tags VARCHAR(500),
    
    -- Timestamps
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_matched_at DATETIME,
    
    -- Constraints
    CHECK (trade_type IN ('LONG', 'SHORT')),
    CHECK (confidence >= 0 AND confidence <= 100),
    CHECK (success_rate >= 0 AND success_rate <= 100)
);

CREATE INDEX idx_patterns_trading_pair ON patterns(trading_pair);
CREATE INDEX idx_patterns_pattern_type ON patterns(pattern_type);
CREATE INDEX idx_patterns_success_rate ON patterns(success_rate);
CREATE INDEX idx_patterns_is_active ON patterns(is_active);

-- =====================================================================
-- TABLE: configurations
-- Description: System and trader configurations (key-value store)
-- =====================================================================
CREATE TABLE configurations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    key VARCHAR(100) NOT NULL UNIQUE,
    value TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    data_type VARCHAR(20) NOT NULL,
    description TEXT,
    is_encrypted BOOLEAN NOT NULL DEFAULT 0,
    is_editable BOOLEAN NOT NULL DEFAULT 1,
    
    -- Timestamps
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CHECK (category IN ('SYSTEM', 'TRADER', 'EXCHANGE', 'UI')),
    CHECK (data_type IN ('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'JSON'))
);

CREATE UNIQUE INDEX idx_configurations_key ON configurations(key);
CREATE INDEX idx_configurations_category ON configurations(category);

-- =====================================================================
-- TABLE: exchange_accounts
-- Description: Exchange account credentials (encrypted, demo/testnet only)
-- =====================================================================
CREATE TABLE exchange_accounts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    exchange_name VARCHAR(20) NOT NULL,
    account_name VARCHAR(100) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    
    -- Encrypted Credentials
    api_key TEXT NOT NULL,
    api_secret TEXT NOT NULL,
    passphrase TEXT,
    encryption_iv VARCHAR(64) NOT NULL,
    
    -- Account Status
    is_active BOOLEAN NOT NULL DEFAULT 1,
    is_default BOOLEAN NOT NULL DEFAULT 0,
    last_connection_test DATETIME,
    connection_status VARCHAR(20),
    last_error_message TEXT,
    
    -- Balance Tracking
    initial_balance DECIMAL(18, 8),
    current_balance DECIMAL(18, 8),
    currency VARCHAR(10) NOT NULL DEFAULT 'USDT',
    
    -- Timestamps
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CHECK (exchange_name IN ('BINANCE', 'BITGET', 'TRADINGVIEW')),
    CHECK (account_type IN ('DEMO', 'TESTNET')),
    CHECK (connection_status IS NULL OR connection_status IN ('SUCCESS', 'FAILED', 'UNKNOWN'))
);

CREATE INDEX idx_exchange_accounts_exchange ON exchange_accounts(exchange_name);
CREATE INDEX idx_exchange_accounts_is_active ON exchange_accounts(is_active);
CREATE INDEX idx_exchange_accounts_is_default ON exchange_accounts(is_default);

-- =====================================================================
-- SEED DATA: Default Configurations
-- =====================================================================
INSERT INTO configurations (key, value, category, data_type, description, is_editable) VALUES
('max_ai_traders', '3', 'SYSTEM', 'INTEGER', 'Maximum number of concurrent AI traders (v1.0 limit)', 0),
('default_leverage', '10', 'TRADER', 'INTEGER', 'Default leverage for new AI traders', 1),
('default_stop_loss', '0.02', 'TRADER', 'DECIMAL', 'Default stop loss percentage (2%)', 1),
('default_take_profit', '0.05', 'TRADER', 'DECIMAL', 'Default take profit percentage (5%)', 1),
('max_daily_loss', '0.10', 'TRADER', 'DECIMAL', 'Maximum daily loss percentage (10%)', 1),
('pattern_min_occurrences', '10', 'SYSTEM', 'INTEGER', 'Minimum occurrences for pattern confidence', 1),
('pattern_confidence_threshold', '60', 'SYSTEM', 'DECIMAL', 'Minimum confidence for pattern matching (60%)', 1),
('demo_mode_only', 'true', 'SYSTEM', 'BOOLEAN', 'Demo/virtual money only (v1.0 restriction)', 0),
('app_version', '1.0.0-SNAPSHOT', 'SYSTEM', 'STRING', 'Application version', 0);

-- =====================================================================
-- END OF MIGRATION
-- =====================================================================

