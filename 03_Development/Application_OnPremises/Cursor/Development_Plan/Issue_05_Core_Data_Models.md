# Issue #5: Core Data Models

**Status**:  **PLANNED** (Not Started)  
**Assigned**: TBD  
**Created**: October 28, 2025  
**Started**: TBD  
**Completed**: TBD  
**Epic**: Foundation & Infrastructure (Phase 1)  
**Priority**: P1 (High)  
**Dependencies**: Issue #1 (Gradle) , Issue #2 (Database) 

---

##  **Objective**

Define and implement core domain models and DTOs that will be shared across the application.

---

##  **Goals**

1. Define domain models in the shared module
2. Create DTOs for API communication
3. Implement data validation and constraints
4. Add serialization support (JSON)
5. Write comprehensive unit tests

---

##  **Task Breakdown**

### Phase 1: Market Data Models
- [ ] Create MarketData domain model
- [ ] Create Candlestick data class
- [ ] Create OrderBook data class
- [ ] Add validation logic

### Phase 2: Trading Models
- [ ] Create TradingStrategy abstract class
- [ ] Create Order domain model
- [ ] Create Position domain model
- [ ] Create Trade domain model

### Phase 3: Configuration Models
- [ ] Create ExchangeConfig data class
- [ ] Create StrategyConfig data class
- [ ] Create RiskConfig data class

### Phase 4: DTOs
- [ ] Create request/response DTOs
- [ ] Add JSON serialization
- [ ] Add validation annotations

### Phase 5: Testing
- [ ] Write unit tests for each model
- [ ] Test serialization/deserialization
- [ ] Test validation logic

---

##  **Related Issues**

- **Depends On**: Issue #1 (Gradle) , Issue #2 (Database) 
- **Blocks**: Issue #6 (Configuration Management), Phase 2 (Market Data Integration)
- **Related**: Issue #3 (REST API), Issue #4 (Logging)

---

**Last Updated**: October 28, 2025
