# Contributing to RedCheck Backend

Thank you for your interest in contributing to the **RedCheck Backend**! We welcome contributions that improve performance, expand functionality, or refine code quality under the terms of the GNU Affero General Public License v3.0 (AGPLv3).

## Development Standards

To maintain high architectural standards across the platform, please adhere to the following guidelines:

* **Language:** All source code, comments, javadocs, and commit messages must be written in professional English.
* **Commit Messages:** Follow the **Conventional Commits** specification (e.g., `feat(auth): implement JWT refresh token filter`, `fix(tasks): resolve lazy loading exception in task mapping`).
* **Code Style:** Follow standard Java conventions, proper dependency injection via Spring, and clean DTO-entity separation.

## Getting Started Locally

1. **Fork and Clone** the repository to your local machine.
2. **Configure Database:** Ensure MySQL is running and set up your local configuration in `src/main/resources/application-local.yml` or via environment variables.
3. **Run the Application:**
   Using the Maven wrapper:
   ```bash
   ./mvnw spring-boot:run
   ```

## Pull Request Workflow

1. Create a descriptive feature branch from `main` (e.g., `feature/oauth2-integration` or `fix/security-filter-chain`).
2. Verify that your code compiles cleanly and passes all local validations.
3. Submit a Pull Request targeting the `main` branch with a clear description of the modifications introduced.
