## 2025-07-20

**Summary:** Refactor `ServiceEngineIdSeed` initialization and `userDevice` assignment.

**Detail:**
- Moved `ServiceEngineFactory.setServiceEngineIdSeed` to the static initializer block for earlier and consistent initialization.
- Changed `userDevice` assignment in `login` and `anonymousLogin` methods to use `ServiceEngineFactory.getEngineId()` for more accurate device identification.

## 2025-07-20

**Summary:** Adjust HSTP webport URL and enhance HTTP client error stream handling.

**Detail:**
- Updated `hstpWebport` in `MCPDirectStudio.java` to include `/hstp/` in the URL.
- Modified `HstpHttpClient.java` to correctly read error streams for HTTP responses with status codes 400 or higher.

## 2025-07-20

**Summary:** Refactor login logic and update `hstp-service-engine` version.

**Detail:**
- Introduced `onLoginHttpResponse` method to centralize login success handling.
- Updated `login` and `anonymousLogin` methods to use the new `onLoginHttpResponse` method.
- Changed `System.setProperty` for `ENGINE_ID_SEED_PROPERTY` to `ServiceEngineFactory.setServiceEngineIdSeed`.
- Updated `hstp-service-engine` dependency version to `1.3.0.0-SNAPSHOT` in `pom.xml`.

## 2025-07-20

**Summary:** Implement anonymous user support and enhance user/tool agent notifications.

**Detail:**
- Added anonymous user registration, login, and transfer functionalities.
- Introduced `UserInfoNotificationHandler` to notify about user information changes.
- Modified `ToolAgentsDetailsNotificationHandler` to include local tool agent details in notifications.
- Refined `login` process to update user information and set engine ID seed.
- Implemented persistence for anonymous keys.

## 2025-07-20

**Summary:** Migrate access key IDs to `long` and refine access key handling.

**Detail:**
- Changed `id` type from `int` to `long` in `AIPortAccessKey` and `AIPortToolPermission` for broader compatibility.
- Updated `AIPortAccessKeyCredential` to use `AIPortAccessKeyValidator.hashCode(secretKey)` for generating access key IDs.
- Modified `MCPDirectStudio` to handle `long` type for access key IDs in various methods (`getAccessKeyCredential`, `modifyAccessKey`, `queryAccessKey`, `getAccessKey`).
- Adjusted the masking of `secretKey` in `MCPDirectStudio` to use `Long.toString(key.id, 36)` for consistency with `long` IDs.
- Updated the `invalidKeys` map in `MCPDirectStudio` to store `Long` values.

## 2025-07-20

**Summary:** Add change password functionality to MCPDirect Studio.

**Detail:**
- Implemented `changePassword` method in `MCPDirectStudio.java` to allow users to change their password.

## 2025-07-20

**Summary:** Implement graceful shutdown and pre-login logout in MCPDirect Studio.

**Detail:**
- Added a shutdown hook to ensure `logout()` is called on application exit.
- Modified the `login` method to perform a `logout()` and potentially exit if a `serviceEngine` is already active, ensuring a clean state before new login.

## 2025-07-20

**Summary:** Refactor handlers and service names, introduce new notification handlers and tool permissions.

**Detail:**
- Renamed `AccessKeyHandler` to `AccessKeyNotificationHandler` and updated its methods to `onAccessKeysNotification` and `onAccessKeyPermissionsNotification`.
- Renamed `MCPServerHandler` to `MCPServerNotificationHandler` and updated its method to `onMCPServersNotification`.
- Renamed `AIToolsServiceHandler` to `AIToolServiceHandler`.
- Renamed `MCPToolsProvider` to `MCPToolProvider`.
- Renamed `AIPortToolsAgent` to `AIPortToolAgent`.
- Renamed `AIPortToolsMaker` to `AIPortToolMaker`.
- Introduced `ToolAgentsDetailsNotificationHandler` for notifying about tool agents, makers, tools, and permissions.
- Added `AIPortToolPermission` entity for managing tool access.
- Implemented `logout` functionality in `MCPDirectStudio`.
- Added `getAllToolAgentsDetails` and `grantToolPermission` methods in `MCPDirectStudio`.
- Updated `hstp-service-engine` version in `pom.xml`.
- Adjusted HSTP URLs in `appnet-hstp-engine-properties.json` and `appnet-hstp-engine.json`.
- Modified `AIPortTool` constructor to include `id`.

## 2025-07-20

**Summary:** Update HSTP endpoints and refactor access key handling.

**Detail:**
- Updated HSTP webport and gateway URLs to `hstp.mcpdirect.ai`.
- Refactored access key management to use `AccessKeyHandler` interface.
- Changed `queryAccessKeys` to update access keys via the handler instead of returning a list.

## 2025-07-20

**Summary:** Refactor project to MCPDirect Studio, remove web UI and SQLite persistence.

**Detail:**
- Renamed project from MCPWings to MCPDirect Studio.
- Removed web UI and related files, including `api-test.html` and icons.
- Replaced SQLite database persistence with file-based storage for access keys.
- Removed `sqlite-jdbc` dependency and all related SQLite helper classes.
- Added custom `JSON` and `HstpHttpClient` utilities to reduce external dependencies.
- Updated all endpoints and provider URLs to point to `mcp.mcpdirect.ai`.
- Enhanced device identification by capturing the machine model name.
- The `tools_agent/init` service now receives the machine model name instead of the OS version.
- Access keys are now stored in a directory based on the user ID for better multi-user support.
- Commented out unused dependencies in `pom.xml` like `swagger-parser`.
- Renamed `MCPWingsWorkshopApplicationTest` to `MCPDirectStudioApplicationTest`.

## 2025-07-19

**Summary:** Refactor MCP server handling and add client information to tools provider.

**Detail:**
- Moved `ToolsLogHandler` to the `handler` package.
- Introduced `MCPServerHandler` for updating MCP servers.
- Added client name and version to `MCPToolsProvider`.
- The `AIToolsServiceHandler` now includes an `X-MCP-Client-Name` header in requests.
- Created the `AIPortTool` entity.
- Added `CHANGELOG.md`.

## 2025-07-19

**Summary:** Initial commit of the MCPDirect Studio project.

**Detail:**
- Added project structure with `pom.xml` for Maven build.
- Included basic source files for the MCPDirect Studio application.
- Added `README.md` and `GEMINI.md` for project documentation.
- Set up `.gitignore` to exclude unnecessary files from version control.