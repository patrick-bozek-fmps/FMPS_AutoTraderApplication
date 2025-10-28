# Development Handbook

**Version**: 1.0  
**Last Updated**: October 28, 2025  
**Purpose**: Central repository for development guidelines, templates, and processes

---

## 📚 **Overview**

This folder contains all the essential documentation and templates needed for developing the FMPS AutoTrader Application. All team members should be familiar with these documents before starting development work.

---

## 📋 **Table of Contents**

### **1. Setup & Configuration**
- **[SETUP_GUIDE.md](./SETUP_GUIDE.md)** - Initial project setup and environment configuration
- **[PIPELINE_SETUP_GUIDE.md](./PIPELINE_SETUP_GUIDE.md)** - CI/CD pipeline setup instructions

### **2. Development Process**
- **[DEVELOPMENT_WORKFLOW.md](./DEVELOPMENT_WORKFLOW.md)** - Standard development workflow and Git practices
- **[ISSUE_TEMPLATE.md](./ISSUE_TEMPLATE.md)** ⭐ **NEW** - Template for creating new Issue files
- **[TESTING_GUIDE.md](./TESTING_GUIDE.md)** - Testing standards and practices
- **[LOGGING_GUIDE.md](./LOGGING_GUIDE.md)** - Logging standards and best practices

### **3. Project Structure**
- **[GITHUB_PROJECT_STRUCTURE.md](./GITHUB_PROJECT_STRUCTURE.md)** - Repository organization and structure

### **4. Tools & Automation**
- **[CI_AUTOMATION_README.md](./CI_AUTOMATION_README.md)** - CI/CD automation tools and scripts
- **[CONVERTER_README.md](./CONVERTER_README.md)** - Excel to Markdown conversion tools

---

## 🚀 **Quick Start**

### **For New Developers**
1. Read **[SETUP_GUIDE.md](./SETUP_GUIDE.md)** to set up your environment
2. Review **[DEVELOPMENT_WORKFLOW.md](./DEVELOPMENT_WORKFLOW.md)** to understand our Git workflow
3. Familiarize yourself with **[GITHUB_PROJECT_STRUCTURE.md](./GITHUB_PROJECT_STRUCTURE.md)**
4. Review **[TESTING_GUIDE.md](./TESTING_GUIDE.md)** and **[LOGGING_GUIDE.md](./LOGGING_GUIDE.md)**

### **For Creating New Issues**
1. Copy **[ISSUE_TEMPLATE.md](./ISSUE_TEMPLATE.md)** to `Development_Plan/Issue_[NUMBER]_[Name].md`
2. Fill in all placeholders (marked with [brackets])
3. Remove optional sections if not needed
4. Update **Development_Plan_v2.md** to reference the new issue

### **For Testing**
1. Follow guidelines in **[TESTING_GUIDE.md](./TESTING_GUIDE.md)**
2. Ensure all tests pass before committing: `./gradlew test`
3. Maintain >80% code coverage

### **For Logging**
1. Follow standards in **[LOGGING_GUIDE.md](./LOGGING_GUIDE.md)**
2. Use appropriate log levels (TRACE, DEBUG, INFO, WARN, ERROR)
3. Never log sensitive information (API keys, passwords, etc.)

---

## 📄 **Document Descriptions**

### **SETUP_GUIDE.md**
Complete guide for setting up the development environment, including:
- Prerequisites (JDK, Gradle, Git, IDE)
- Cloning the repository
- Building the project
- Running tests
- IDE configuration

### **DEVELOPMENT_WORKFLOW.md**
Standard workflow for all development work:
- Git branching strategy
- Commit message conventions
- Pull request process
- Code review guidelines
- CI/CD integration

### **ISSUE_TEMPLATE.md** ⭐ **NEW**
Comprehensive template for creating new Issue files with:
- Standardized header format (Status, Assigned, Created, Started, Completed, Duration, Epic, Priority, Dependencies)
- Task breakdown structure
- Deliverables checklist
- Success criteria table
- Testing and documentation sections
- Commit strategy
- Definition of Done

**Use this template for all new issues to maintain consistency!**

### **TESTING_GUIDE.md**
Testing standards and practices:
- Unit testing guidelines
- Integration testing approach
- Test coverage requirements
- Mocking strategies
- Test organization

### **LOGGING_GUIDE.md**
Logging standards and best practices:
- Log levels and when to use them
- Structured logging with MDC
- Performance considerations
- Security (what NOT to log)
- Configuration examples

### **GITHUB_PROJECT_STRUCTURE.md**
Repository organization:
- Directory structure
- Module purposes
- File naming conventions
- Documentation organization

### **PIPELINE_SETUP_GUIDE.md**
CI/CD pipeline configuration:
- GitHub Actions setup
- Build automation
- Test automation
- Deployment process

### **CI_AUTOMATION_README.md**
CI/CD automation tools:
- Status checking scripts
- Annotation analysis
- Failure diagnosis
- Automated testing

### **CONVERTER_README.md**
Excel to Markdown conversion tools:
- Usage instructions
- Batch conversion
- Watch mode for automatic conversion

---

## ✅ **Best Practices**

### **Documentation**
- ✅ Keep all documentation up-to-date
- ✅ Use clear, concise language
- ✅ Include code examples where applicable
- ✅ Add diagrams for complex concepts
- ✅ Cross-reference related documents

### **Issue Management**
- ✅ Use **ISSUE_TEMPLATE.md** for all new issues
- ✅ Update issue status as work progresses
- ✅ Keep task checklists current
- ✅ Document blockers and risks
- ✅ Update Development_Plan_v2.md when issues complete

### **Code Quality**
- ✅ Write tests before code (TDD when possible)
- ✅ Maintain >80% code coverage
- ✅ Follow Kotlin coding conventions
- ✅ Add KDoc comments to public APIs
- ✅ Use meaningful variable and function names

### **Git & Version Control**
- ✅ Write clear, descriptive commit messages
- ✅ Commit logical units of work
- ✅ Run tests before committing
- ✅ Keep commits focused and atomic
- ✅ Reference issue numbers in commits

### **Logging**
- ✅ Use appropriate log levels
- ✅ Include context in log messages
- ✅ Never log sensitive data
- ✅ Use structured logging (MDC)
- ✅ Log entry/exit of critical operations

---

## 🆕 **What's New**

### **October 28, 2025**
- ✅ **ISSUE_TEMPLATE.md** created with standardized format
- ✅ All Issue files (#1-#6) harmonized with consistent headers
- ✅ Development_Handbook README created (this file)

---

## 📞 **Support & Questions**

If you have questions about any of these documents or need clarification:

1. Check the relevant guide first
2. Search existing issues and discussions
3. Ask in the team chat
4. Contact the project lead

---

## 📝 **Contributing to Documentation**

When updating documentation:

1. Follow the same format as existing docs
2. Use clear headings and sections
3. Include a table of contents for long documents
4. Add examples and code snippets
5. Update this README if adding new documents
6. Commit with descriptive message: `docs: Update [DocumentName] - [Brief description]`

---

## 📊 **Documentation Status**

| Document | Status | Last Updated | Next Review |
|----------|--------|--------------|-------------|
| SETUP_GUIDE.md | ✅ Current | Oct 2025 | Nov 2025 |
| DEVELOPMENT_WORKFLOW.md | ✅ Current | Oct 2025 | Nov 2025 |
| ISSUE_TEMPLATE.md | ✅ Current | Oct 28, 2025 | Dec 2025 |
| TESTING_GUIDE.md | ✅ Current | Oct 2025 | Nov 2025 |
| LOGGING_GUIDE.md | ✅ Current | Oct 2025 | Nov 2025 |
| GITHUB_PROJECT_STRUCTURE.md | ✅ Current | Oct 2025 | Nov 2025 |
| PIPELINE_SETUP_GUIDE.md | ✅ Current | Oct 2025 | Nov 2025 |
| CI_AUTOMATION_README.md | ✅ Current | Oct 2025 | Nov 2025 |
| CONVERTER_README.md | ✅ Current | Oct 2025 | Nov 2025 |

---

**Maintained By**: Development Team  
**Version**: 1.0  
**Last Updated**: October 28, 2025

