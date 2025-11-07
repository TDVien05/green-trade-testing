# View this tutorial with code mode to easy to read

# ⚡ Green Trade Platform (Backend)

## 📘 Overview

This is the **backend service** for the *Green Trade Platform* project, developed using **Spring Boot**.  
It implements the core business logic for user management, subscription handling, transactions, and product posting.  
Comprehensive **unit tests** are written using **JUnit 5** and **Mockito** to ensure reliability and maintainability.

---

## ⚙️ System Requirements
------------------------------------------------
| Tool / Technology      | Recommended Version |
|------------------------|---------------------|
| **Java**               |    17 or higher     |
| **Spring Boot**        |        3.2.x        |
| **Maven**              |        3.8+         |
| **IDE**                |    IntelliJ IDEA    |
| **Database**           |        MySQL        |
| **Test Frameworks**    | JUnit 5 + Mockito   |
| **Build Tool**         |        Maven        |
------------------------------------------------


## 🧩 Project Structure
green-trade-platform/

├── .mvn/

├── database/

├── target/

├── pom.xml

├── README.md

└── src/

├── main/

│ ├── java/Green_trade/green_trade_platform/

│ │ ├── advisor/

│ │ ├── config/

│ │ ├── controller/

│ │ ├── enumerate/

│ │ ├── exception/

│ │ ├── filter/

│ │ ├── mapper/

│ │ ├── model/

│ │ ├── repository/

│ │ ├── service/

│ │ ├── util/

│ │ └── request/response/

│ └── resources/

│ ├── application.yml

│ └── static/

└── test/

└── java/Green_trade/green_trade_platform/

├── service/


🧪 Unit Testing
🧠 Frameworks Used
JUnit 5 (Jupiter) → for test structure and assertions
Mockito → for mocking dependencies
AssertJ / Hamcrest → for fluent assertions
Spring Boot Test → for integration-style unit tests

🧩 Scope
All main service flows are covered, including:
Buyer operations (BuyerService)
Subscription management (SubscriptionPackageService)
Product posting and verification (PostProductService)
External shipping integration (GhnService)
And so on 

🧱 Coverage Goal
Minimum coverage: 80%

## 🧪 How to Run Unit Tests (Spring Boot + JUnit + Mockito)
### 🧰 Prerequisites
Make sure you have the following installed:
- **Java 17** (or compatible version used in `pom.xml`)
- **Maven 3.8+**
- **Spring Boot 3.x**
- **IntelliJ IDEA** 
- Internet connection (for Maven dependency resolution on first build)
### ▶️ Run Tests Using IntelliJ IDEA (Recommended)
1. **Open the project** in IntelliJ IDEA.  
   Wait for Maven to finish importing dependencies.
2. In the **Project Explorer**, navigate to:
   --> src/test/java/Green_trade/green_trade_platform/service
3. **Right-click** the `service` package (or any individual test class you want to run).
4. Select:
   --> Run 'Tests in Green_trade.green_trade_platform.service'
    → IntelliJ will automatically detect JUnit tests and execute them.
5. To check coverage, right-click again and select 'More Run/Debug':
   --> Run 'Tests in Green_trade.green_trade_platform.service' with Coverage
6. At coverage window --> Select icon ↗️ --> Save
7. See the result
============================================================
   
