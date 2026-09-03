# RedCheck - Backend

![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)

> **RedCheck API** is the robust backend infrastructure that powers the intelligent task prioritization system. It handles complex data relationships, secure authentication, and the integration with external AI models.

This repository contains the **Backend** architecture. You can find the React client and User Interface in [this link](https://github.com/redcheckapp/redcheck-frontend.git). 

This project demonstrates scalable RESTful API design, rigorous security implementation, and seamless integration of artificial intelligence for background processing.

<p align="center">
  <img src="https://github.com/user-attachments/assets/91100413-5db4-4849-9075-c7ba640219eb" alt="Swagger API Documentation" width="800"/>
</p>

## Key Features

The API was engineered with a focus on security, performance, and clear domain segregation.

*   **SmartCheck AI Integration:** Asynchronous communication with external LLMs. The backend constructs complex, context-aware prompts based on the user's workload, enforces strict JSON output formats, and processes the AI response for the frontend.
*   **Robust Authentication:** Secure endpoint protection utilizing Spring Security. It features stateless JWT (JSON Web Token) generation, validation, and custom filter chains for authorization.
*   **Advanced Data Management:** Relational mapping utilizing Spring Data JPA. Includes cascading soft-deletes (Trash functionality) and data isolation between users.
*   **Recurring Routines Engine:** Automated logic to calculate and generate future tasks based on custom periodicities (Daily, Weekly, Biweekly, Monthly).
*   **Internationalization (i18n) Support:** Dynamic prompt engineering that adapts the AI output language based on the client's request (`?lang=` parameters).

## Tech Stack

*   **Core:** Java 17, Spring Boot 3.x.
*   **Security:** Spring Security, JWT (JSON Web Tokens).
*   **Persistence:** Spring Data JPA, Hibernate, MySQL 8.0+.
*   **Build Tool:** Maven.

## Local Installation & Configuration

### Prerequisites
*   Java Development Kit (JDK) 17 or higher
*   Maven 3.8+
*   MySQL Server running locally or via Docker

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/redcheckapp/redcheck-backend.git
   cd redcheck-backend
   ```
2. Database and Environment Configuration:
  Update your `src/main/resources/application.properties` with your local credentials and API keys.
  ```Properties
  # Database Configuration
  spring.datasource.url=jdbc:mysql://localhost:3306/redcheck
  spring.datasource.username=root
  spring.datasource.password=your_db_password
  spring.jpa.hibernate.ddl-auto=update

  # JWT Configuration
  jwt.secret=your_highly_secure_base64_encoded_secret_key
  jwt.expiration=86400000

  # AI Configuration
  ai.api.key=your_llm_api_key
  ```
3. Build and Run:
   ```Bash
   mvn clean install
   mvn spring-boot:run
   ```
4. The server will start on `http://localhost:8080`.

## Architecture Highlights

The codebase strictly adheres to the Controller-Service-Repository pattern, ensuring a clean separation of concerns:

  - `Controllers`: Handle HTTP requests, routing, and response formatting.

  - `Services`: Contain pure business logic and transactional boundaries.

  - `Repositories`: Interface with the MySQL database via Spring Data JPA.

  - `DTOs (Data Transfer Objects)`: Prevent data over-fetching and securely map internal entities to external payloads.

```mermaid
graph TD
    %% Node Styles
    classDef proxy fill:#009639,stroke:#00732c,stroke-width:2px,color:#fff;
    classDef backend fill:#6DB33F,stroke:#4a8229,stroke-width:2px,color:#fff;
    classDef aiengine fill:#3670A0,stroke:#29567c,stroke-width:2px,color:#fff;
    classDef db fill:#00758F,stroke:#005c70,stroke-width:2px,color:#fff;
    classDef vector fill:#FF4F00,stroke:#c43c00,stroke-width:2px,color:#fff;
    classDef external fill:#8E75B2,stroke:#6e5a8a,stroke-width:2px,color:#fff;

    Gateway["API Gateway / Frontend<br>(External Requests)"]:::proxy
    Gemini["Google Gemini 2.5 Flash<br>(External LLM API)"]:::external

    %% Backend Isolation Network
    subgraph DockerNet ["Internal Network: redcheck-net"]
        style DockerNet fill:none,stroke:#0984e3,stroke-width:2px
        
        Spring["Core API Container<br>(Java 17 / Spring Boot)"]:::backend
        MySQL[("Database Container<br>(MySQL 8.0)")]:::db

        %% SmartCheck AI Sub-module
        subgraph AIModule ["SmartCheck AI Module"]
            style AIModule fill:none,stroke:#3670A0,stroke-width:2px,stroke-dasharray: 5 5
            
            SmartCheck["SmartCheck AI Engine<br>(Python / FastAPI)"]:::aiengine
            ChromaDB[("Vector Memory<br>(ChromaDB / SQLite)")]:::vector
        end
    end

    %% Flow
    Gateway -. "REST API Calls<br>(/auth, /tasks, /ai)" .-> Spring
    Spring == "TCP 3306<br>(Spring Data JPA)" ==> MySQL
    Spring -- "POST /api/v1/prioritize<br>(DTO with Tasks & Analytics)" --> SmartCheck
    SmartCheck == "Semantic Similarity Search" ==> ChromaDB
    SmartCheck -- "RAG Augmented Prompt" --> Gemini
    Gemini -. "Strict JSON Schema" .-> SmartCheck
    SmartCheck -. "200 OK (Validated Daily Plan)" .-> Spring
```

## Copyright and License
This project is licensed under the **GNU Affero General Public License v3.0 (AGPLv3)**.
