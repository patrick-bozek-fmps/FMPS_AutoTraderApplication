---
title: Project Handbook
version: 1.0
status: Draft
last_updated: 2025-10-31

> This Project Handbook defines the purpose, structure, environment, and processes of the FMPS Auto Trader Application project.  
> It serves as a reference and operational guide for all contributors, including human team members and AI Agents, ensuring consistent execution according to agile principles, quality standards, and defined system engineering processes.
---

# 1. Purpose of the Document

This document describes:
- The project environment, organization, collaboration, and communication rules.  
- The reporting structure, decision-making process, and performance metrics.  
- The applicable engineering processes and tools required to deliver the FMPS Auto Trader Application.  
- The behavior, scope, and references for all project roles and AI agents.

---

# 2. References

| ID | Document | Description | Location |
|----|-----------|-------------|-----------|
| REF-01 | FMPS_System_Engineering_Process_v3.1.yaml | Defines the system engineering process and derivation rules | /process |
| REF-02 | FMPS_AutoTraderApplication_Customer_Specification.md | Customer Requirements and Architecture | /specifications |
| REF-03 | FMPS_AutoTraderApplication_System_Specification.md | System-level requirements derived from CRS | /specifications |
| REF-04 | FMPS_AutoTraderApplication_System_Development_Plan.md | Development phases and deliverables | /plans |
| REF-05 | FMPS_Requirements_Guideline.md | Defines method for requirement writing | /guidelines |
| REF-06 | FMPS_Continuous_Integration_Guide.md | Defines CI/CD pipelines and quality checks | /ci |
| REF-07 | FMPS_Quality_Assurance_Plan.md | Defines test strategy and quality gates | /quality |

---

# 3. Project Environment

| Aspect | Description |
|--------|--------------|
| Project Type | FinTech – AI-based Trading Platform |
| Development Approach | Agile (Lean Delivery) |
| Sprint Cadence | 3 weeks |
| Technology Stack | Python, TypeScript, React, FastAPI, PostgreSQL |
| Infrastructure | Cloud-native (Docker/Kubernetes), CI/CD via GitLab |
| Version Control | Git (branching model: main / develop / feature/*) |
| Tooling | Jira, Confluence, GitLab, PlantUML, Draw.io, MkDocs |
| Quality Framework | FMPS Quality Management & ASPICE compliance |
| Security | Follows ISO/IEC 27001 guidelines for data protection and code security |

---

# 4. Project Organization

## 4.1 Organizational Structure

| Role | Responsibility | AI Agent Role | Reports To | AI Only |
|------|----------------|----------------|-------------|---------|
| Product Manager | Strategic alignment, business goals, roadmap ownership | PM Agent | Steering Committee |   |
| Product Owner | Tactical backlog management, prioritization, delivery coordination | PO Agent | Product Manager |   |
| System Engineer | Derive & validate system requirements | SE Agent | Product Owner | x |
| System Architect | Define architecture and interfaces | Architecture Agent | Product Owner | x |
| Software Engineer | Implement and test features | SWE Agent | Product Owner | x |
| QA Engineer | Verification & validation, test automation | QA Agent | Product Owner | x |
| DevOps Engineer | CI/CD, deployment automation, system monitoring | DevOps Agent | Product Owner | x |

---

## 4.2 Collaboration and Process Automation Flow

This diagram illustrates the **automated workflow** that connects all core engineering processes —  
from **Customer Specification** through **System Engineering**, **Software Implementation**, and **Release Management**.  

Each version change in Git acts as a trigger, activating the corresponding AI Agents and process pipelines.  
The **Product Owner** initiates the flow by updating the Customer Specification; this update automatically starts  
the **System Engineering Process**, followed by the **Software Engineering Process** and finally the **Release Management Phase**.  

The diagram emphasizes **continuous traceability**, **AI-assisted automation**, and **human approval gates**,  
ensuring that every artifact — from requirements to release — remains validated, version-controlled, and compliant.

 ┌──────────────────────────────┐
 │        Product Owner         │
 │ Publishes Customer Spec (CRS)│
 └──────────────┬───────────────┘
                │  Version Tag ↑
                │  (e.g. v1.3)
                ▼
 ┌──────────────────────────────┐
 │         SE Agent             │
 │ Executes System Eng. Process │
 │  (System Spec, Architecture) │
 └──────────────┬───────────────┘
                │  PO Approval → Merge
                │  Version Tag ↑ (e.g. v1.4)
                ▼
 ┌──────────────────────────────┐
 │   Software Engineering       │
 │ SWE + DevOps Build & Test    │
 │ → "Demo Mode Build"          │
 └──────────────┬───────────────┘
                │
                ▼
 ┌──────────────────────────────┐
 │     Demo Evaluation Phase    │
 │ AI Agents + QA Evaluate in   │
 │ Active Demo Environment (1wk)│
 └──────────────┬───────────────┘
                │  PO Decision
                │  ("Release Ready?")
          ┌─────┴──────────────────────┐
          │                            │
          ▼                            ▼
 ┌──────────────────────┐      ┌──────────────────────┐
 │   Rework (SWE Loop)  │      │ Release Management   │
 │  Implement Fixes/Mods │      │  Build & Package     │
 │  → Increment Version  │      │  Create Release Notes│
 │  → Redeploy Demo      │      │  Customer Delivery   │
 └──────────────────────┘      └──────────────────────┘
                                     │
                                     ▼
                           ┌──────────────────────┐
                           │  Customer Environment│
                           │  (Official Release)  │
                           └──────────────────────┘
# 5. Project Reporting

## 5.1 Decision-Making
| Decision Type                  | Responsible Role | Approval Level     | Documentation           |
| ------------------------------ | ---------------- | ------------------ | ----------------------- |
| Process Changes                | DevOps Engineer  | Product Manager    | /process/changes        |
| Architecture Changes           | System Architect | Product Owner      | /architecture/decisions |
| Scope / Backlog Prioritization | Product Owner    | Product Manager    | /plans/backlog          |
| Strategic Roadmap              | Product Manager  | Steering Committee | /plans/roadmap          |

---

## 5.2 Metrics and Performance Indicators
| Metric                | Description                            | Measured By   | Target      |
| --------------------- | -------------------------------------- | ------------- | ----------- |
| Velocity              | Story points completed per sprint      | Product Owner | Stable ±10% |
| Requirements Coverage | % of CRs traced to SYS & SWE           | SE Agent      | 100%        |
| Test Coverage         | % of automated tests per release       | QA Agent      | > 85%       |
| CI Success Rate       | Ratio of passing builds                | DevOps Agent  | > 95%       |
| Defect Leakage        | Bugs found post-release                | QA Agent      | < 3%        |
| Process Compliance    | CI validation success (YAML/processes) | DevOps Agent  | 100%        |

---

# 6. Project Processes

> All processes are under strict configuration control and must follow YAML definitions in `/process`.

---

## 6.1 Product Engineering

### Description
Transforms business goals and stakeholder needs into **Customer Requirements Specifications (CRS)**.  
Defines customer functions, architecture, and pre-development planning basis for System Engineering.

### Responsible Role
- **Primary:** Product Owner  
- **Supporting:** Product Manager, SE Agent

### Guidelines
- Use the Markdown-based template `Customer_Specification_Template.md`.
- Follow requirement writing principles from `FMPS_Requirements_Guideline.md`.
- Maintain traceability to the System Engineering Process.

### Process
1. Identify and formalize customer requirements.  
2. Validate completeness, regulatory alignment, and feasibility.  
3. Commit changes to Git and increment version in CRS file.  
4. Trigger System Engineering Process automatically via CI/CD pipeline.

### Template
- `/specifications/FMPS_AutoTraderApplication_Customer_Specification.md`

---

## 6.2 System Engineering

### Description
Derives and structures **System Requirements**, **System Architecture**, and **System Development Plan**  
from the approved Customer Specification. Ensures traceability and process integrity.

### Responsible Role
- **Primary:** System Engineer  
- **Supporting:** System Architect, Product Owner, SE Agent, Architecture Agent

### Guidelines
- YAML process: `FMPS_System_Engineering_Process_v3.1.yaml`  
- Use modeling tools: PlantUML, Draw.io, MkDocs  
- Ensure one-to-one mapping CRS → SYS → SWE IDs

### Process
1. Parse CRS Markdown file (`customer_spec`)  
2. Derive system-level requirements and architecture  
3. Generate system specification (`system_spec.md`), architecture (`system_arch.md`), and development plan (`dev_plan.md`)  
4. Product Owner reviews & approves SE outputs  
5. Version increment triggers the Software Engineering Process  

### Template
- `/process/FMPS_System_Engineering_Process_v3.1.yaml`  
- `/specifications/FMPS_AutoTraderApplication_System_Specification.md`  
- `/plans/FMPS_AutoTraderApplication_System_Development_Plan.md`

---

## 6.3 Software Engineering

### Description
Implements the **System Requirements** into executable code and verifies functionality through automated testing.  
Ensures compliance with architecture, security, and performance standards.

### Responsible Role
- **Primary:** Software Engineer  
- **Supporting:** QA Engineer, DevOps Engineer, SWE Agent

### Guidelines
- Follow principles of TDD (Test Driven Development).  
- Code standards: PEP8 (Python), ESLint (TypeScript).  
- All commits must link to SYS IDs in the commit message.  
- Ensure >85 % automated test coverage.

### Process
1. Convert approved system requirements into design and code modules.  
2. Implement automated unit and integration tests.  
3. Execute build pipelines with CI validation.  
4. Deploy to demo environment for evaluation.  
5. Provide release candidate for approval by Product Owner.

### Template
- `/specifications/FMPS_AutoTraderApplication_System_Specification.md`  
- `/templates/SWE_Implementation_Template.md`  
- `/ci/gitlab-ci.yml`

---

## 6.4 Release Management

### Description
Controls demo evaluation, release readiness, and customer delivery.  
Ensures every release is evaluated for one week in demo mode before customer distribution.

### Responsible Role
- **Primary:** DevOps Engineer  
- **Supporting:** Product Owner, QA Engineer, SWE Agent

### Guidelines
- All releases must have official `Release Notes`.  
- Demo evaluation results must be documented.  
- Follow CI/CD pipeline rules for artifact signing and traceability.

### Process
1. Build and deploy demo version (`vX.Y-demo`) to evaluation environment.  
2. Conduct 1-week demo testing and collect metrics.  
3. Product Owner reviews metrics and approves release.  
4. Build official release package and generate release notes.  
5. Deploy to customer environment and archive release in `/releases`.

### Template
- `/templates/Release_Notes_Template.md`  
- `/ci/release_pipeline.yml`  
- `/releases/`

---

## 6.5 Configuration and DevOps

### Description
Ensures continuous integration, configuration control, and process integrity validation.  
Manages automated checks and process enforcement (modification policies, hash validation).

### Responsible Role
- **Primary:** DevOps Engineer  
- **Supporting:** Product Owner, Process Agents

### Guidelines
- YAML and Markdown files are configuration-controlled.  
- Every file must contain `sha256` integrity check.  
- No AI Agent may auto-edit configuration files (`modification_policy: no-ai-autoedit`).

### Process
1. Monitor all repository commits for compliance.  
2. Validate YAML and Markdown artifacts via CI scripts.  
3. Generate process integrity reports in `/ci/reports`.  
4. Trigger downstream pipelines (SWE, Release) based on tags and approvals.  
5. Maintain daily backup in secure cloud repository.

### Template
- `/ci/FMPS_CI_Validation_Pipeline.yml`  
- `/process/Process_Integrity_Checklist.md`  
- `/reports/Process_Compliance_Report.md`

