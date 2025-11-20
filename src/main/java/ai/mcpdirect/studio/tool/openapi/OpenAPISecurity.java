package ai.mcpdirect.studio.tool.openapi;

import io.swagger.v3.oas.models.security.SecurityScheme;

public record OpenAPISecurity(SecurityScheme scheme, String security){};
