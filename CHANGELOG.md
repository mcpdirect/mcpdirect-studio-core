## 2025-10-17

**Summary:** Removed hash and tools fields from AIPortToolMaker entity.

**Detail:**
- Removed hash and tools fields from AIPortToolMaker entity

## 2025-10-16

**Summary:** Renamed tool maker team entity and enhanced tool querying capabilities.

**Detail:**
- Renamed AIPortToolMakerTeam to AIPortTeamToolMaker for better naming consistency
- Added teamId field to AIPortToolMaker entity and removed userName/userAccount fields
- Changed agentId and makerId types from Long to long in AIPortToolPermission
- Modified queryToolMakers method to accept teamId parameter
- Modified queryTools method to accept userId instead of toolId
- Added getTool method to retrieve a single tool by ID
- Simplified callback handling in query methods by removing redundant checks
- Renamed modifyToolMakerTeams to modifyTeamToolMakers method
- Renamed queryToolMakerTeams to queryTeamToolMakers method
- Updated modifyTeamToolMakers and queryTeamToolMakers to accept team parameter instead of just teamId
- Changed endpoint from \"tool_maker/team/modify\" to \"tool_maker/team/query\" for queryTeamToolMakers
- Updated parameter handling in team and tool maker management methods

## 2025-10-15

**Summary:** Added tool maker team management functionality and refined HSTP requests.

**Detail:**
- Added AIPortToolMakerTeam entity to manage relationships between tool makers and teams
- Changed status field type from short to Integer in AIPortToolMakerTeam
- Added copy() method to AIPortToolMakerTeam for creating copies of instances
- Modified hstpRequest method to use a more explicit HSTP service call approach
- Added modifyToolMakerTeams method to manage tool maker team associations
- Added queryToolMakerTeams method to retrieve tool maker team associations
- Simplified parameter validation in acceptTeamMember method
- Added builder pattern methods for AIPortToolMakerTeam entity

## 2025-10-14

**Summary:** Enhanced tool maker queries and added tool agent details access.

**Detail:**
- Modified queryToolMakers method to use the new hstpRequest method with Convertor interface
- Commented out the old queryToolMakers implementation
- Added getLocalToolAgentDetails method to MCPDirectStudio for accessing tool agent details
- Added agentStatus, agentName, userName, and userAccount fields to AIPortToolMaker entity

## 2025-10-13

**Summary:** Updated MCP version and enhanced team management features.

**Detail:**
- Updated mcp-version from 0.14.0 to 0.14.1 in pom.xml
- Added ownerName and ownerAccount fields to AIPortTeam entity
- Changed userDevice parameter in login method from serviceEngine.getEngineId() to machineId
- Added error logging in hstpRequest method to print HSTP request errors
- Added acceptTeamMember method to MCPDirectStudio for accepting team member invitations

## 2025-10-12

**Summary:** Added team management functionality with team and team member entities.

**Detail:**
- Added AIPortTeam entity with builder pattern implementation
- Added AIPortTeamMember entity with builder pattern implementation
- Added AccountServiceErrors interface with user and team error codes
- Added new import to include all account entities in MCPDirectStudio
- Removed specific account entity imports (AIPortAccessKeyCredential, AIPortOtp, AIPortUser) from MCPDirectStudio
- Added generic hstpRequest method with Convertor interface for flexible response handling
- Implemented modifyAccessKey method for updating access key properties
- Added team management methods: createTeam, queryTeams, modifyTeam
- Added team member management methods: inviteTeamMember, queryTeamMembers, modifyTeamMember
- Added userId field to AIPortToolMaker entity
- Added lastUpdated field to AIPortTeam and AIPortTeamMember entities
- Added name and account fields to AIPortTeamMember entity
- Removed constructors from AIPortToolMaker, AIPortTeam, and AIPortTeamMember in favor of builder pattern

## 2025-10-11

**Summary:** Added access key management and tool permission summaries.

**Detail:**
- Added AIPortToolPermissionMakerSummary entity with accessKeyId, makerId, and count fields
- Added import for AIPortAnonymousCredential in MCPDirectStudio
- Removed unused imports (Callback and BiFunction) from MCPDirectStudio
- Updated return type from AIPortAccessKeyCredential to AIPortAnonymousCredential in register/anonymous call
- Added generateAccessKey method to MCPDirectStudio for creating new access keys
- Added queryAccessKeys method to MCPDirectStudio for retrieving all access keys
- Added queryToolPermissionMakerSummaries method to MCPDirectStudio for permission summaries
- Modified AIPortAnonymousCredential to remove password field and constructor
- Improved callback handling in queryToolAgents method

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
- Modified RequestOfQueryTools to include userId field
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