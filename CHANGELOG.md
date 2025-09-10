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