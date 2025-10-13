# Expense Splitter API  
A RESTful API built with Scala (Play Framework) for managing shared expenses, balances, and user authentication using JWT-based auth.
It enables users to register, log in, add shared expenses, and view who owes whom — just like real-world group expense apps.

---

## 📚 Table of Contents

- [Architecture](#architecture)  
- [Endpoints](#endpoints)  
- [Example Payloads](#example-payloads)   
- [Tech Stack](#tech-stack) 
- [Prerequisites](#prerequisites)  
- [Installation & Setup](#installation--setup)  
- [Authentication-flow](#authentication-flow) 
- [Notes](#notes)   
- [Postman Testing](#postman-testing) 
---

## 🏗️ Architecture

The project follows a layered architecture:

 - ***Controllers*** Handle HTTP requests & responses.
 - ***Services*** Contain business logic (auth, expenses, balances).
 - ***Repositories*** Handle database interactions.
 - ***Models*** Represent entities (User, Expense, Balance, Notification).
 - ***Auth Layer*** Manages JWT generation, validation, and refresh.
 - ***gRPC Layer*** Sends notifications asynchronously.
 - ***Config*** Stored in conf/application.conf.
 - ***Tests*** Validate core functionality.

---

## 📦 Endpoints
## 🔐 Auth APIs

| Method | Endpoint         | Description                                    |
| ------ | ---------------- | ---------------------------------------------- |
| POST   | `/auth/register` | Register a new user                            |
| POST   | `/auth/login`    | Log in and receive access + refresh tokens     |
| POST   | `/auth/refresh`  | Refresh access token using valid refresh token |
| POST   | `/auth/logout`   | Revoke refresh token                           |

## 👥 User APIs

| Method | Endpoint     | Description                           |
| ------ | ------------ | ------------------------------------- |
| POST   | `/users`     | Create a user (admin use or internal) |
| GET    | `/users`     | Get all users                         |
| GET    | `/users/:id` | Get user details by ID                |

## 💰 Expense APIs (Protected)

| Method | Endpoint                 | Description                          |
| ------ | ------------------------ | ------------------------------------ |
| POST   | `/expenses`              | Create a new expense                 |
| GET    | `/expenses`              | Get all expenses                     |
| GET    | `/expenses/:id`          | Get a specific expense by ID         |
| GET    | `/expenses/user/:userId` | Get all expenses for a specific user |
| DELETE | `/expenses/:id`          | Delete an expense                    |

## ⚖️ Balance APIs (Protected)

| Method | Endpoint                       | Description                                |
| ------ | ------------------------------ | ------------------------------------------ |
| GET    | `/balances`                    | Get all balances                           |
| GET    | `/balances/user/:userId`       | Get all balances for a user                |
| GET    | `/balances/user/:userId/owes`  | Get balances where the user owes others    |
| GET    | `/balances/user/:userId/owed`  | Get balances where others owe the user     |
| GET    | `/balances/expense/:expenseId` | Get balances related to a specific expense |

---

## 🧪 Example Payloads

#### ➕ Register (POST /auth/register)
``` {
  "name": "John",
  "email": "john@example.com",
  "password": "password123"
}
```
## Response
```
{
  "message": "User registered successfully",
  "userId": 1
}
```
#### 🔑 Login (POST /auth/login)
```
{
  "email": "john@example.com",
  "password": "password123"
}
```
## Response
```
{
  "accessToken": "<jwt-access-token>",
  "refreshToken": "<refresh-token>"
}
```
#### 💵 Create Expense (POST /expenses)
```
{
  "description": "Dinner",
  "amount": 600,
  "paidBy": 1,
  "participants": [2, 3]
}
```
## Response
```
{
  "message": "Expense created successfully",
  "expenseId": 10
}
```
#### ⚖️ Get Balances (GET /balances)
## Response
```
[
  {
    "from": "Alice",
    "to": "John",
    "amount": 200
  },
  {
    "from": "Bob",
    "to": "John",
    "amount": 200
  }
]
```
---

## 🛠️ Tech Stack

Language: Scala

Framework: Play Framework

Database: SQL (Relational)

Async Operations: Futures

Authentication: JWT + Refresh Tokens

Password Hashing: Bcrypt

Serialization: Play JSON

Build Tool: sbt

---

## ⚙️ Prerequisites

Git

Java JDK 11+

sbt (Scala Build Tool)

SQL Database

---

## 🚀 Installation & Setup

Clone the repository:
```
git clone https://github.com/Praveena-g2005/ExpenseSplitter.git
cd expenseservice
```
Configure the database connection in:
```
conf/application.conf
```
Run the project:
```
sbt run
```
Access the API:
```
http://localhost:9000/
```
---
## 🔐 Authentication Flow

1.User registers → credentials stored with bcrypt hash.

2.User logs in → receives JWT access token + refresh token.

3.Protected routes (/expenses, /balances) require the Authorization: Bearer <token> header.

4.When the access token expires, user can use /auth/refresh to get a new one.

5.Logout revokes the refresh token in the database.

## 🚧 Notes

Notifications are sent via gRPC and logged to console.

All /expenses and /balances routes are protected by JWT.

No email verification implemented yet.

Future improvements:

Add settlements endpoint for payments between users.

Add notification listing endpoint.

Add role-based access control (RBAC).

---

## 🧪 Postman Testing

Example test flow:

POST /auth/register → Create a user

POST /auth/login → Obtain tokens

POST /expenses (with Authorization header) → Add expense

GET /balances → Verify balances