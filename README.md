# Expense Splitter API  
A simple RESTful API to manage expenses and calculate balances between users.  
The project is built with Scala, Play Framework, Async / Futures, and SQL Database.

---

## 📚 Table of Contents

- [Architecture](#architecture)  
- [Endpoints](#endpoints)  
- [Example Payloads](#example-payloads)   
- [Tech Stack](#tech-stack)  
- [Prerequisites](#prerequisites)  
- [Installation & Setup](#installation--setup)  
- [Notes](#notes)   
- [Postman Testing](#postman-testing) 
---

## 🏗️ Architecture

The application follows a layered structure: 
- **Controllers** handle HTTP requests.  
- **Services** implement business logic.  
- **Repositories** manage database operations.  
- **Models** define data structures (Expense, Balance, User, Notification, Settlement).  
- **gRPC Layer** handles notifications.  
- **Configuration** is in `conf/app.conf`.  
- **Tests** validate functionality.
---

## 📦 Endpoints

| Method | Endpoint   | Description                                     |
|--------|------------|-------------------------------------------------|
| POST   | /expenses  | Add a new expense (with who paid, amount, participants) |
| GET    | /balances  | Get calculated balances showing who owes what |

---

## 🧪 Example Payloads

#### Create Expense (POST /expenses)
```
{
  "paidBy": "John",
  "amount": 500,
  "participants": ["Alice", "Bob"]
} 
```
## Example Response for GET /balances 
```
[
  {
    "from": "Alice",
    "to": "John",
    "amount": 250
  },
  {
    "from": "Bob",
    "to": "John",
    "amount": 250
  }
]
```
---

## 🛠️ Tech Stack

Scala (Play Framework)

Async / Futures

SQL Database

Play JSON for serialization

---

## ⚙️ Prerequisites

Git

Java Development Kit (JDK 11+)

sbt (Scala Build Tool)

SQL Database

---

## 🚀 Installation & Setup

Clone the repository:
```
git clone https://github.com/yourusername/expense-splitter.git
cd expense-splitter
```
Configure your environment (e.g., database connection settings in application.conf).

Build and run the application:
```
sbt run
```
The API will be available at:
http://localhost:9000/

---

## 🚧 Notes

Notifications are logged to console and saved in the database when a new expense is created.

No specific validation is applied to user identifiers (can be names, IDs, etc.).

Future improvements:

Add endpoints for managing users.

Add settlement endpoint to explicitly record payments between users.

Add endpoint to view notifications.

---

## 🧪 Postman Testing

Example:

POST /expenses → Valid Expense JSON → 200 OK response

GET /balances → List of balances as JSON array
