##Lost & Found - Greater Boston Network

A comprehensive multi-enterprise lost and found management system designed to connect universities, public transit (MBTA), airports (Logan), and law enforcement across the Greater Boston area.

## 📋 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [System Architecture](#system-architecture)
- [User Roles](#user-roles)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Running the Application](#running-the-application)
- [Project Structure](#project-structure)
- [Core Components](#core-components)
- [Database Schema](#database-schema)
- [Work Request Workflow](#work-request-workflow)
- [Trust Score System](#trust-score-system)

---

## Overview

The **Campus Lost & Found System** is a Java Swing desktop application that facilitates the management and recovery of lost items across multiple enterprises in the Greater Boston area. The system creates a collaborative network connecting:

- **Higher Education Institutions** (Universities and Colleges)
- **Public Transit** (MBTA Stations and Transit Network)
- **Airports** (Logan International Airport)
- **Law Enforcement** (Boston Police Department)

The platform enables seamless item tracking, cross-enterprise transfers, claim verification, and dispute resolution through a sophisticated work request approval system.

---

## Key Features

### 🔍 Item Management
- Report lost and found items with detailed descriptions
- Image upload support for item identification
- Category-based organization (Electronics, Books, Clothing, IDs, Keys, etc.)
- Keyword-based smart matching algorithm
- Status tracking (Open, Pending Claim, Verified, Claimed, Cancelled, Expired)

### 🔄 Cross-Enterprise Transfers
- **Transit to University Transfer**: Items found on MBTA transferred to campus
- **Airport to University Transfer**: Items from Logan Airport to universities
- **Cross-Campus Transfer**: Items moved between university campuses
- **Emergency MBTA to Airport Transfer**: Urgent transfers for travelers

### ✅ Work Request System
- Multi-step approval workflows
- Role-based approval chains
- SLA monitoring and priority management
- Automated routing to appropriate approvers
- Real-time status tracking

### 🛡️ Trust Score System
- Dynamic user reputation scoring (0-100)
- Event-based score adjustments
- Fraud detection and flagging
- Investigation management
- Claim eligibility based on trust level

---

## System Architecture

```
Network (Greater Boston Lost & Found Ecosystem)
    │
    ├── Enterprise: Higher Education
    │   ├── Organization: Campus Operations
    │   ├── Organization: Student Services
    │   └── Organization: Campus Security
    │
    ├── Enterprise: Public Transit (MBTA)
    │   ├── Organization: Station Operations
    │   ├── Organization: Transit Police
    │   └── Organization: Central Lost & Found
    │
    ├── Enterprise: Airport (Logan)
    │   ├── Organization: Airport Operations
    │   ├── Organization: TSA Security
    │   └── Organization: Airline Services
    │
    └── Enterprise: Law Enforcement
        ├── Organization: Police Department
        ├── Organization: Evidence Management
        └── Organization: Detective Bureau
```

---

## User Roles

### Higher Education Roles
| Role | Description |
|------|-------------|
| **Student** | Report lost/found items, submit claims, confirm pickups |
| **Campus Coordinator** | Approve claims, initiate transfers, manage inventory |
| **Campus Security** | Handle security-related items, investigations |
| **University Admin** | Full administrative access |

### MBTA Roles
| Role | Description |
|------|-------------|
| **Station Manager** | Manage station items, approve transfers |
| **Transit Security Inspector** | Fraud detection, security verification |

### Airport Roles
| Role | Description |
|------|-------------|
| **Airport Lost & Found Specialist** | Process airport items, coordinate transfers |
| **TSA Security Coordinator** | Security screening items, verification |

### Law Enforcement Roles
| Role | Description |
|------|-------------|
| **Police Evidence Custodian** | Manage evidence chain, custody tracking |
| **Detective** | Access investigation records, fraud cases |

---

## Technology Stack

| Component | Technology |
|-----------|------------|
| **Language** | Java 17+ |
| **UI Framework** | Java Swing |
| **Database** | MongoDB 4.11+ |
| **Build System** | Apache Ant (NetBeans) |
| **PDF Generation** | Apache PDFBox 2.0.27 |

---

## Installation & Setup

### 1. Prerequisites
- Java Development Kit (JDK) 17+
- MongoDB 4.4+ running on `localhost:27017`
- NetBeans IDE (recommended)

### 2. Configure Database
Edit `Final_Project/resources/mongodb.properties`:
```properties
mongodb.connection.string=mongodb://localhost:27017
mongodb.database=campus_lostfound
```

### 3. Run the Application
```bash
# From NetBeans: Press F6 or Run Project
# From command line:
java -jar dist/Final_Project.jar
```

---

## Work Request Workflow

### Request Types & Approval Chains

| Request Type | Approval Chain |
|--------------|----------------|
| Item Claim | Student → Campus Coordinator |
| Cross-Campus Transfer | Source Coordinator → Destination Coordinator → Student |
| Transit to University | MBTA Manager → Campus Coordinator → Student |
| Airport to University | Airport Specialist → Campus Coordinator → Student |
| Police Evidence | Campus Coordinator → Police Custodian |

---

## Trust Score System

| Level | Score | Capabilities |
|-------|-------|--------------|
| **Excellent** | 90-100 | All claims, skip verification |
| **Good** | 70-89 | High-value claims allowed |
| **Fair** | 50-69 | Standard claims only |
| **Low** | 30-49 | Limited, needs verification |
| **Probation** | 0-29 | Restricted access |

---

## License

INFO 5100 - Application Engineering and Development  
Northeastern University - Fall 2025
