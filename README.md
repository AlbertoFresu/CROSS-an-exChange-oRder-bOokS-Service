# CROSS: an exChange oRder bOokS Service

**CROSS** is a distributed Java-based system simulating a simplified order book service for financial exchanges. The system enables multiple clients to register, authenticate, submit orders, receive real-time notifications, and request historical price data.

This project was developed as part of an academic internship and focuses on concurrency, modularity, and scalable software architecture.

---

## 📦 Features

- ✅ Registration and secure authentication (with credential validation)
- 📩 Client-server communication via TCP and UDP
- 🔄 Asynchronous notifications using non-blocking I/O (NIO)
- 📊 Price history storage and query by month/year
- 💾 Order serialization with JSON (Gson)
- 🔧 Configurable via `.cfg` files (no manual input required)

---

## 🧠 Technologies Used

- **Java 17**
- **Gson** (for JSON parsing)
- **java.nio** (for non-blocking networking)
- **Multithreading** and thread pools
- **RMI** for initial client registration
- **Concurrent collections** (`ConcurrentHashMap`, `BlockingQueue`, etc.)

---

## 🗂 Project Structure

├── src/
│ ├── client/ # Client-side logic and notification listeners
│ ├── server/ # Server-side logic and order processing
│ ├── common/ # Shared classes (e.g. JSON parser)
│ ├── order/ # Order management classes
│ ├── user/ # User account management
│ ├── rmi/ # Remote method invocation for registration
│
├── config/ # Configuration files (.cfg)
├── resources/ # Serialized orders and user data
├── lib/ # External dependencies (Gson)
└── docs/
└── Relazione_Progetto_CROSS_AlbertoFresu_582314.pdf

yaml
Copia
Modifica

---

## 🚀 How to Build and Run

### 📍 Prerequisites
- Java 17 or newer
- Gson 2.8.9 (`lib/gson-2.8.9.jar`)

### 🔧 Windows

1. Compile the project:
   ```bash
   dir /b /s src\*.java > sources.txt
   javac -cp lib\gson-2.8.9.jar -d out @sources.txt
   
Create executable JARs:
jar --create --file client.jar --main-class=client.ClientMain -C out .
jar --create --file server.jar --main-class=server.ServerMain -C out .

Add config and resources:
jar --update --file client.jar -C config client.cfg -C resources .
jar --update --file server.jar -C config server.cfg -C resources .

Run:
java -cp server.jar;lib\gson-2.8.9.jar server.ServerMain
java -cp client.jar;lib\gson-2.8.9.jar client.ClientMain

🐧 Linux/Mac
Same as above, but replace ; with : in classpaths and use:
find src -name "*.java" > sources.txt
javac -cp lib/gson-2.8.9.jar -d out @sources.txt

jar --create --file client.jar --main-class=client.ClientMain -C out .
jar --create --file server.jar --main-class=server.ServerMain -C out .

jar --update --file client.jar -C config client.cfg -C resources .
jar --update --file server.jar -C config server.cfg -C resources .

java -cp server.jar:lib/gson-2.8.9.jar server.ServerMain
java -cp client.jar:lib/gson-2.8.9.jar client.ClientMain
📖 Documentation
📄 Read the full technical report (PDF)

The PDF contains:

Architecture overview

Thread management model

Communication protocol

Data structure design

Order and price history handling

Synchronization and concurrency handling

📬 Author
Alberto Fresu
LinkedIn
Email: albertofresu05@gmail.com

📝 License
This project is intended for educational and demonstrative purposes.
You may adapt and reuse it following the terms of the MIT License (if applicable).
