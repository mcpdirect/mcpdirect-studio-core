## 2025-10-10

**Summary:** Enhanced permission system with agentId support and tool inheritance.

**Detail:**
- Modified AIPortTool to include agentId field
- Added parameterized constructor to AIPortToolAgent for name initialization
- Extended AIPortToolPermission with agentId, makerId, and name fields
- Added copy() method to AIPortToolPermission for creating permission copies
- Made AIPortVirtualToolPermission extend AIPortToolPermission for better inheritance
- Added copy() method to AIPortVirtualToolPermission with originalToolId support
- Updated grantToolPermission method to support both regular and virtual permissions
- Fixed endpoint path in queryToolPermissions from \"tool/virtual/query\" to \"tool/permission/query\"
- Added a test method for permission copying functionality

## 2025-10-09

**Summary:** Added tool permissions and tool agent management functionality.

**Detail:**
- Added AIPortVirtualToolPermission entity with originalToolId field
- Added AIPortToolPermission entity for tool permission management
- Enhanced MCPDirectStudio with queryToolPermissions and queryVirtualToolPermissions methods
- Added queryToolAgents method to MCPDirectStudio for tool agent management
- Modified RequestOfToolMaker to include toolAgentId field
- Updated queryToolMakers method to accept toolAgentId parameter
- Fixed constructor parameter order in RequestOfToolMaker
- Added new API endpoints for permission and agent queries

## 2025-10-08

**Summary:** Enhanced tool maker functionality and virtual tool support.

**Detail:**
- Updated project version from 1.1.4-SNAPSHOT to 1.2.0-SNAPSHOT
- Updated mcp-version from 0.12.1 to 0.14.0
- Added new AIPortVirtualTool entity for virtual tool management
- Implemented ToolMakerNotificationHandler interface with tool maker notifications
- Added virtual tool creation and modification capabilities in MCPDirectStudio
- Enhanced MCPToolProvider with JSON mapper for improved serialization
- Modified AIPortToolMaker to support TYPE_VIRTUAL (0) constant
- Added query and modification methods for tools and virtual tools
- Added test methods and ServerParameters helper for NPX execution
- Improved communication with backend services for tool maker operations

## 2025-09-11

**Summary:** Enhanced machine identification and dependency updates.

**Detail:**
- Updated project version from 1.1.3-SNAPSHOT to 1.1.4-SNAPSHOT
- Updated mcp-version from 0.11.3 to 0.12.1
- Updated hstp-service-engine version from 1.4.0 to 1.4.2
- Enhanced machine identification with improved hostname and model detection
- Added frontend-maven-plugin dependency for testing
- Improved Windows system detection with additional system information
- Enhanced Linux system detection with product name retrieval
- Improved macOS system detection with better model name and UUID extraction
- Added test methods for machine name identification

## 2025-09-11

**Summary:** Fixed machine ID generation for Windows systems.

**Detail:**
- Updated project version from 1.1.2-SNAPSHOT to 1.1.3-SNAPSHOT
- Fixed machine ID generation on Windows systems by correcting array index access for MachineGuid

## 2025-09-10

**Summary:** Enhanced configuration loading with system property support.

**Detail:**
- Updated project version from 1.1.1-SNAPSHOT to 1.1.2-SNAPSHOT
- Enhanced configuration loading to support system properties in addition to properties file and environment variables
- Added support for reading ai.mcpdirect.hstp.webport and ai.mcpdirect.hstp.service.gateway from system properties

## 2025-09-10

**Summary:** Configuration enhancements and dependency updates for improved connectivity.

**Detail:**
- Updated project version from 1.1.0-SNAPSHOT to 1.1.1-SNAPSHOT
- Updated hstp-service-engine version from 1.3.0 to 1.4.0
- Added mcpdirect-studio.properties configuration file for better environment configuration
- Enhanced configuration loading to support both properties file and environment variables
- Improved HSTP service engine configuration with dynamic gateway detection
- Added SSL context factory for secure connections
- Removed unused MAC address based machine ID generation code
- Updated appnet-hstp-engine.json to support dynamic gateway configuration
- Added test methods for tool listing and tool call functionality

## 2025-08-30

**Summary:** Enhance device identification, improve machine ID generation, and add status tracking.

**Detail:**
- Updated project version from 1.0.0-SNAPSHOT to 1.1.0-SNAPSHOT
- Updated mcp-version from 0.11.2 to 0.11.3
- Updated hstp-service-engine version from 1.3.0.0-SNAPSHOT to 1.3.0
- Enhanced device identification by adding deviceId field to AIPortToolAgent
- Improved machine ID generation using a more robust approach incorporating user name, home directory, and creation time
- Added statusMessage field to MCPServer and MCPToolProvider for better error tracking
- Modified login flows to use serviceEngine.getEngineId() instead of ServiceEngineFactory.getEngineId()
- Added proper null checking in logout method
- Enhanced tool agent initialization with deviceId parameter
- Fixed macOS detection in machine ID generation
- Added accountKeySeed to AccountDetails for better session management
- Clear mcpServerConfigs on logout