## 2025-10-31

**Summary:** Added usage field to AIPortAccessKey and AIPortTool entities.

**Detail:**
- Changed field name from usageAmount to usage in AIPortAccessKey entity
- Added usage field to AIPortTool entity

## 2025-10-30

**Summary:** Enhanced tool call result handling in MCPToolProvider.

**Detail:**
- Added JSON import to MCPToolProvider
- Modified callTool method to return JSON serialized result instead of toString
- Added exception handling for JSON serialization in callTool method

## 2025-10-29

**Summary:** Added new MCPServer constructor and enhanced notification handling.

**Detail:**
- Added new MCPServer constructor that accepts an ID parameter
- Added calls to notificationHandler for server notifications after tool publishing
- Added notificationHandler getter method in MCPDirectStudio

## 2025-10-28

**Summary:** Modified virtual tool structure and updated MCP config creation.

**Detail:**
- Changed AIPortVirtualTool back to have toolId field instead of originalToolId
- Modified createMCPConfigFromKey to use environment variable for gateway host
- Updated createMCPConfigFromKey to use X-MCPdirect-Key header in environment

## 2025-10-27

**Summary:** Added event listeners cleanup and modified virtual tool structure.

**Detail:**
- Added eventListeners and accessKeyCredentials cleanup during logout in MCPDirectStudio
- Renamed listeners variable to eventListeners in MCPDirectStudio for better clarity
- Changed AIPortVirtualTool to have originalToolId field instead of toolId and tags
- Commented out authentication requirement in AIToolServiceHandler callTool method
- Commented out tool logging in AIToolServiceHandler callTool method

## 2025-10-26

**Summary:** Added null check for account details in HTTP request method.

**Detail:**
- Added null check for accountDetails before accessing accessToken in httpRequest method

## 2025-10-25

**Summary:** Added HTTP request method and enhanced HSTP client handling.

**Detail:**
- Added httpRequest method in MCPDirectStudio for making HTTP requests with HSTP authentication
- Modified HstpHttpClient.doPost to handle both String and object bodies properly
- Added a blank line for better code formatting in the Convertor interface

## 2025-10-24

**Summary:** Optimized tool agent initialization and local server connection process.

**Detail:**
- Modified initToolAgent to asynchronously connect to MCP servers and send notifications
- Changed connectLocalMCPServers to accept a configuration map as parameter
- Added status code Integer.MIN_VALUE for new server notifications
- Implemented threading for connecting to servers asynchronously
- Added maker validation check before creating server configurations
- Removed unnecessary writeMCPServerConfigs calls in loop
- Improved notification handling during tool agent initialization

## 2025-10-23

**Summary:** Added NotificationHandler interface and enhanced server management.

**Detail:**
- Added NotificationHandler interface for handling MCP server notifications
- Changed MCPDirectStudio logout method to return Boolean instead of String
- Replaced notifyMCPServer calls with notificationHandler.onMCPServerNotification
- Added connectLocalMCPServers and loadLocalMCPServerConfigs methods
- Renamed reassignMCPServer to remapMCPServer in AIToolServiceHandler
- Modified StudioConsoleServiceHandler to use mcpServerId instead of mcpServiceId
- Added metaData() method to MCPTool for generating metadata
- Updated tool publishing logic to properly handle tool metadata and hashes
- Modified getAIPortTools to return tool duplicates instead of new instances
- Added setNotificationHandler method in MCPDirectStudio
- Removed unused AITool import from MCPDirectStudio
- Commented out several deprecated or replaced methods and sections
- Added proper initialization handling in StudioConsoleServiceHandler

## 2025-10-22

**Summary:** Refactored tool handling with MCPTool and enhanced tool publishing.

**Detail:**
- Added MCPToolSchema class with commented out implementation
- Modified AITool interface to remove inputSchema method
- Refactored MCPServer to use MCPTool instead of AITool in getTools() method
- Added merge method in MCPServer to merge AIPortToolMaker with tools list
- Enhanced MCPTool with provider and tool properties with proper JSON ignore annotations
- Implemented setMCPToolProvider method in MCPTool for initializing tool properties
- Added merge method in MCPTool to update properties from AIPortTool
- Modified MCPToolProvider to reuse existing MCPTool instances instead of creating new ones
- Updated MCPDirectStudio to use MCPTool in getAIPortTools and createPublishingTools methods
- Modified publishTools method to handle empty tool lists and update tool properties
- Added createVirtualToolMaker method for creating virtual tool makers
- Added createToolMaker method with MCPServer parameter for creating MCP tool makers
- Modified publishTools to pass the MCPServer object to createToolMaker
- Added import for MCPTool in MCPDirectStudio

## 2025-10-19

**Summary:** Added MCPDirectStdioClientTransport and StudioConsoleServiceHandler with enhanced server management.

**Detail:**
- Added new MCPDirectStdioClientTransport class extending existing StdioClientTransport
- Added new StudioConsoleServiceHandler for managing console operations
- Renamed type field to transport in several entities (AIPortMCPServerConfig, MCPServerConfig)
- Modified MCPServer to extend AIPortToolMaker instead of MCPServerConfig
- Added connectMCPServer method in AIToolServiceHandler to connect to MCP servers
- Changed getMCPServer to use serverId instead of serverName as parameter
- Added removeMCPServer and reassignMCPServer methods in AIToolServiceHandler
- Modified publishTools in MCPDirectStudio to be asynchronous with callback
- Added login and logout methods with Callback functionality
- Replaced addMCPServer with connectMCPServer in MCPDirectStudio
- Added localMCPServerId method to generate unique IDs for local servers
- Added event listener system with fireEvent mechanism
- Added utility methods for studio engine ID and tool agent ID
- Modified AIPortTool to include a constructor with name parameter
- Added merge method to MCPServer to incorporate AIPortToolMaker properties
- Implemented configMCPServerConfig functionality to modify server configurations
- Added HstpResponseHandler interface for HSTP response handling

## 2025-10-18

**Summary:** Enhanced MCP server configuration with type support and added new API methods.

**Detail:**
- Added type field to AIPortMCPServerConfig entity to support different server types
- Added type field to MCPServerConfig and MCPToolProvider classes with corresponding constructors
- Modified addMCPServer methods to accept serverType parameter
- Added new modifyToolAgent and modifyToolMaker methods for updating entities
- Added accountId() and studioId() utility methods
- Changed name field in MCPServer from final to mutable
- Added tags field to MCPServer entity
- Integrated type parameter in MCPToolProvider for different transport mechanisms
- Added support for HttpClientStreamableHttpTransport for type 2 servers
- Modified query methods to pass type parameter when adding MCP servers
- Added environment variable for test ID during machine ID generation

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