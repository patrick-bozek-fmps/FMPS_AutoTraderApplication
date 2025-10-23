# Missing Information & Open Points

**Project**: FMPS_AutoTraderApplication  
**Date**: October 23, 2025  
**Status**: Awaiting Clarification

---

## 1. REQUIREMENTS CLARIFICATION NEEDED

### 1.1 Excel Specification Files
The following Excel files in `02_ReqMgn/` need to be reviewed but cannot be read automatically:

- ❓ **CommonDefinitionOfTermsAndAbbreviations.xlsx**
  - Need: Standard terminology and abbreviations used in the project
  - Impact: Ensures consistent naming and documentation

- ❓ **FMPS_AutoTraderApplication_Customer_Specification.xlsx**
  - Need: Customer requirements, use cases, user stories
  - Impact: Defines what the customer expects from the application
  - Questions:
    - What are the primary use cases?
    - Who are the target users?
    - What are the must-have vs nice-to-have features?
    - Are there specific trading strategies requested?
    - What exchanges must be supported initially?
    - What reporting/analytics are required?

- ❓ **FMPS_AutoTraderApplication_System_Specification.xlsx**
  - Need: System requirements, technical specifications, constraints
  - Impact: Technical architecture and implementation decisions
  - Questions:
    - What are the performance requirements?
    - What are the security requirements?
    - What are the regulatory/compliance requirements?
    - Are there specific data retention policies?
    - What are the system resource constraints?
    - Are there integration requirements with other systems?

- ❓ **Template_BaseFunction_Specification.xlsx**
  - Need: Base function templates and patterns to follow
  - Impact: Code structure and implementation patterns

**Action Required**: Please provide either:
- Access to read these Excel files (export as CSV/JSON/TXT), OR
- Summary document of key requirements, OR
- Schedule a requirements review meeting

---

## 2. BUSINESS REQUIREMENTS

### 2.1 Trading Strategy
- ❓ What trading strategies should be implemented?
  - Currently have basic: RSI + MACD + SMA crossover
  - Are additional strategies needed?
  - Should users be able to create custom strategies?
  - Are there specific trading timeframes to target?

### 2.2 Asset Types
- ❓ Which asset types need to be supported?
  - Cryptocurrencies (confirmed)
  - Stocks (mentioned in README)
  - ETFs (mentioned in README)
  - Forex?
  - Commodities?
  - Derivatives?

### 2.3 Exchange Priority
- ❓ Exchange implementation priority?
  - Binance (started)
  - Bitget (started)
  - Other exchanges needed?
  - Which should be completed first?

### 2.4 Trading Modes
- ❓ What trading modes are required?
  - Fully automated (AI-driven)
  - Semi-automated (AI suggests, user approves)
  - Manual trading with indicators
  - Paper trading / Backtesting
  - All of the above?

### 2.5 Risk Management
- ❓ What risk management rules are required?
  - Maximum daily loss limit?
  - Maximum position size?
  - Maximum leverage limits?
  - Exposure limits per asset?
  - Stop-loss requirements?
  - Take-profit targets?

### 2.6 Reporting & Analytics
- ❓ What reports/analytics are needed?
  - Real-time P&L?
  - Historical performance?
  - Tax reporting?
  - Trade journal?
  - Performance metrics (Sharpe ratio, win rate, etc.)?
  - Export formats (PDF, CSV, Excel)?

---

## 3. TECHNICAL QUESTIONS

### 3.1 User Authentication
- ❓ Is user authentication/login required?
  - Single user application?
  - Multi-user with accounts?
  - If multi-user, what authentication method?

### 3.2 Data Storage
- ❓ Data retention requirements?
  - How long to keep trade history?
  - Local database only or cloud backup?
  - Database size limits?
  - Data export/import functionality?

### 3.3 API Keys Management
- ❓ How should API keys be managed?
  - Stored locally encrypted?
  - User enters on each startup?
  - Hardware token integration?
  - Multiple exchange accounts per user?

### 3.4 Network & Connectivity
- ❓ Network requirements?
  - Minimum bandwidth?
  - Required ports for connections?
  - VPN support needed?
  - Proxy server support?
  - Offline mode capabilities?

### 3.5 Monitoring & Alerts
- ❓ What notification/alert mechanisms?
  - In-app notifications only?
  - Email notifications?
  - SMS/Phone notifications?
  - Telegram/Discord webhooks?
  - Sound alerts?

### 3.6 UI/UX Requirements
- ❓ User interface preferences?
  - Light/dark mode?
  - Customizable dashboards?
  - Multi-monitor support?
  - Accessibility requirements?
  - Language localization needed?

### 3.7 Backup & Recovery
- ❓ Backup strategy?
  - Automatic backups?
  - Manual export/import?
  - Cloud sync?
  - Disaster recovery plan?

### 3.8 Updates & Maintenance
- ❓ Update mechanism?
  - Auto-update?
  - Manual download and install?
  - Notify user of updates?
  - Update frequency?

---

## 4. REGULATORY & COMPLIANCE

### 4.1 Legal Requirements
- ❓ Are there regulatory requirements?
  - Which jurisdictions?
  - KYC/AML requirements?
  - Trading licenses needed?
  - Audit trail requirements?
  - Data privacy (GDPR, CCPA)?

### 4.2 Disclaimers & Terms
- ❓ Legal disclaimers needed?
  - Terms of service?
  - Risk disclosure?
  - Liability limitations?
  - User agreement acceptance flow?

---

## 5. INTEGRATION POINTS

### 5.1 TradingView Integration
- ✅ Webhook server planned
- ❓ Authentication method for webhooks?
- ❓ Signal format specification?
- ❓ Rate limiting on webhooks?

### 5.2 External Systems
- ❓ Integration with other systems needed?
  - Portfolio management tools?
  - Tax software?
  - Accounting systems?
  - Other trading platforms?

### 5.3 APIs to Expose
- ❓ Should the application expose APIs?
  - REST API for external tools?
  - WebSocket for real-time data?
  - Plugin system for extensions?

---

## 6. TESTING & QUALITY ASSURANCE

### 6.1 Testing Environments
- ❓ Testing strategy?
  - Use exchange testnet/sandbox?
  - Mock trading mode required?
  - Paper trading mode required?
  - Real money testing approach?

### 6.2 Performance Requirements
- ❓ Specific performance targets?
  - Maximum order execution time?
  - UI responsiveness requirements?
  - Maximum concurrent positions?
  - Data processing throughput?

### 6.3 Reliability Requirements
- ❓ Uptime requirements?
  - 99%, 99.9%, 99.99%?
  - Acceptable downtime windows?
  - Failover mechanisms?

---

## 7. DEPLOYMENT & DISTRIBUTION

### 7.1 Installation
- ❓ Installation requirements?
  - Installer (MSI/EXE) or portable?
  - Administrator privileges required?
  - Installation size limits?
  - System requirements document?

### 7.2 Licensing
- ❓ Software licensing?
  - Open source or proprietary?
  - If open source, which license?
  - License key validation needed?
  - Trial period functionality?

### 7.3 Distribution Channels
- ❓ How will software be distributed?
  - GitHub releases only?
  - Company website?
  - Microsoft Store?
  - Direct download links?

---

## 8. DOCUMENTATION

### 8.1 User Documentation
- ❓ Documentation format?
  - In-app help?
  - PDF user manual?
  - Online wiki/docs?
  - Video tutorials?

### 8.2 Developer Documentation
- ❓ Developer docs needed?
  - API documentation?
  - Architecture documentation?
  - Plugin development guide?
  - Contribution guidelines?

---

## 9. SUPPORT & MAINTENANCE

### 9.1 Support Channels
- ❓ User support method?
  - Email support?
  - Issue tracker (GitHub)?
  - Forum/community?
  - Paid support options?

### 9.2 Bug Reporting
- ❓ Bug reporting process?
  - GitHub issues?
  - In-app bug reporter?
  - Email to support?
  - Crash report collection?

---

## 10. BUDGET & RESOURCES

### 10.1 Third-Party Services
- ❓ Budget for third-party services?
  - Market data subscriptions?
  - Cloud services (if any)?
  - Code signing certificates?
  - CI/CD infrastructure?

### 10.2 Development Team
- ❓ Team composition?
  - Team size?
  - Roles and responsibilities?
  - Full-time or part-time?
  - Development timeline constraints?

### 10.3 Exchange Fees
- ❓ Who covers exchange trading fees?
  - Development testing fees?
  - Commission structure?

---

## 11. OPEN TECHNICAL DECISIONS

### 11.1 Architecture Decisions
- ⏳ **Database Choice**
  - H2 for embedded, zero-config
  - SQLite for broader compatibility
  - PostgreSQL for robustness (requires separate install)
  - **Recommendation**: SQLite (good balance)

- ⏳ **HTTP Client**
  - Ktor Client (modern, Kotlin-first)
  - OkHttp (mature, widely used)
  - **Recommendation**: Ktor Client for new development

- ⏳ **UI Framework**
  - JavaFX (standard, mature)
  - Compose for Desktop (modern, experimental)
  - Swing (legacy, not recommended)
  - **Recommendation**: JavaFX with TornadoFX

- ⏳ **Configuration Format**
  - HOCON (Typesafe Config)
  - YAML
  - Properties files
  - **Recommendation**: HOCON for flexibility

### 11.2 Package Naming
- ⚠️ **Need to standardize package names**
  - Current inconsistencies:
    - `de.fmps.onprem`
    - `com.fmps.app_onprem`
    - `de.fmps.app_onprem`
  - **Recommendation**: `com.fmps.autotrader` (consistent, professional)
  - Breakdown:
    - `com.fmps.autotrader.core`
    - `com.fmps.autotrader.ui`
    - `com.fmps.autotrader.connectors`
    - etc.

### 11.3 Project Structure
- ⚠️ **Gradle module structure issue**
  - `settings.gradle.kts` references `:app` module
  - No `app/` folder exists in `Application_OnPremises/`
  - **Options**:
    1. Create `app/` module and move code there (multi-module)
    2. Remove `:app` reference and use single module (simpler)
  - **Recommendation**: Single module for now, can refactor later if needed

---

## 12. CODE CLEANUP PRIORITIES

### 12.1 Immediate Fixes Needed
1. ⚠️ **Fix package declarations**
   - Standardize to `com.fmps.autotrader.*`
   - Update all imports

2. ⚠️ **Remove duplicate code**
   - `TechnicalIndicators` class duplicated in `AI_Trading_Agent.kt`
   - Multiple MACD data class definitions

3. ⚠️ **Fix syntax errors**
   - `Connector_Binance.kt`: Misplaced braces and imports
   - `Connector_Bitget.kt`: Duplicate package declarations
   - `Connector_TradingView.kt`: Misplaced braces

4. ⚠️ **Define missing classes**
   - `Position`
   - `Candlestick`
   - `TradeAction` (enum)
   - `TradeDecision`
   - `TradeSignal`
   - `TradingState` (enum)

5. ⚠️ **Organize connector classes**
   - Move inline connectors to separate files
   - Create proper interfaces
   - Implement factory pattern

### 12.2 Code Quality Issues
- ❌ No error handling
- ❌ No input validation
- ❌ No logging
- ❌ Hardcoded values
- ❌ No configuration management
- ❌ No unit tests
- ❌ Missing documentation (KDoc)

---

## 13. DEPENDENCIES & TOOLS

### 13.1 Development Environment Setup
- ✅ IntelliJ IDEA installed?
- ✅ JDK 17+ installed?
- ✅ Git configured?
- ⏳ Gradle wrapper working?
- ⏳ Exchange testnet accounts created?

### 13.2 Access Requirements
- ⏳ GitHub repository access confirmed
- ⏳ Binance testnet API keys
- ⏳ Bitget testnet API keys
- ⏳ TradingView account for webhooks

---

## 14. PRIORITIZATION QUESTIONS

### 14.1 MVP Definition
- ❓ What is the Minimum Viable Product?
  - Which features are must-have for v1.0?
  - Which can be deferred to v1.1+?
  - Target release date?

### 14.2 Feature Prioritization
Please rank these features by priority (1=highest):
- [ ] Automated trading with basic strategy (AI agent)
- [ ] Multi-exchange support
- [ ] Real-time monitoring dashboard
- [ ] Manual trading interface
- [ ] TradingView webhook integration
- [ ] Backtesting capabilities
- [ ] Performance analytics and reporting
- [ ] Paper trading mode
- [ ] Mobile app integration (future)

---

## 15. ASSUMPTIONS MADE

Based on available information, the following assumptions were made in the development plan:

1. ✓ **Platform**: Windows 10/11 desktop application (64-bit)
2. ✓ **Single User**: No multi-user authentication required initially
3. ✓ **Local Storage**: SQLite for local data persistence
4. ✓ **Two Exchanges**: Binance and Bitget as primary targets
5. ✓ **Cryptocurrency Focus**: Primary asset type is crypto
6. ✓ **Automated Trading**: Main use case is AI-driven automated trading
7. ✓ **Self-Hosted**: On-premises installation, no cloud backend
8. ✓ **English Only**: Initial release in English only
9. ✓ **No Mobile**: Desktop only for v1.0
10. ✓ **Paper Trading**: Included for safety and testing

**Please confirm or correct these assumptions.**

---

## 16. NEXT STEPS

### Step 1: Requirements Review
- [ ] Schedule meeting to review Excel specifications
- [ ] Clarify open questions above
- [ ] Confirm assumptions
- [ ] Prioritize features for MVP

### Step 2: Technical Setup
- [ ] Finalize technology stack decisions
- [ ] Set up development environment
- [ ] Create exchange testnet accounts
- [ ] Configure CI/CD pipeline

### Step 3: Begin Development
- [ ] Start Phase 1: Foundation
- [ ] Clean up existing code
- [ ] Implement core data models
- [ ] Set up logging and configuration

---

## 17. DECISION LOG

| Date | Decision | Rationale | Status |
|------|----------|-----------|--------|
| 2025-10-23 | Use Kotlin/JVM for desktop app | Already started, good for trading apps | ✅ Confirmed |
| 2025-10-23 | Target Windows platform | User specified | ✅ Confirmed |
| 2025-10-23 | Use JavaFX for UI | Mature, good for desktop | ⏳ Pending approval |
| 2025-10-23 | SQLite for database | Simple, embedded, reliable | ⏳ Pending approval |
| 2025-10-23 | Ktor for HTTP client | Modern, Kotlin-first | ⏳ Pending approval |
| 2025-10-23 | Package: com.fmps.autotrader | Professional, consistent | ⏳ Pending approval |
| 2025-10-23 | Single Gradle module | Simpler for initial dev | ⏳ Pending approval |
| 2025-10-23 | 14-week timeline | Based on scope estimate | ⏳ Pending approval |

---

## 18. CONTACTS & RESOURCES

### 18.1 Key Contacts
- **Product Owner**: [TBD]
- **Lead Developer**: [TBD]
- **QA Lead**: [TBD]
- **DevOps**: [TBD]

### 18.2 Useful Resources
- **GitHub Repository**: https://github.com/patrick-bozek-fmps/FMPS_AutoTraderApplication
- **Project Directory**: C:\PABLO\AI_Projects\FMPS_AutoTraderApplication
- **Requirements**: C:\PABLO\AI_Projects\FMPS_AutoTraderApplication\02_ReqMgn

---

## SUMMARY OF CRITICAL BLOCKERS

### Must Resolve Before Starting Phase 1:
1. ⚠️ **Review Excel specification files** - Need requirements clarity
2. ⚠️ **Confirm MVP scope** - What features for v1.0?
3. ⚠️ **Approve technical stack** - Finalize technology choices
4. ⚠️ **Fix package naming** - Standardize before writing more code
5. ⚠️ **Create exchange testnet accounts** - Needed for development

### Can Be Resolved During Development:
- UI/UX details
- Specific trading strategies
- Reporting formats
- Advanced features
- Performance optimization

---

**Document Status**: DRAFT v1.0  
**Last Updated**: October 23, 2025  
**Owner**: Development Team  
**Next Review**: After stakeholder feedback

