package ai.mcpdirect.studio.openapi.util;

import ai.mcpdirect.studio.dao.entity.OpenAPIServerDoc;
import ai.mcpdirect.studio.service.AIPortServiceResponse;
import ai.mcpdirect.studio.service.ConsoleServiceHandler;
import appnet.hstp.engine.util.JSON;
import junit.framework.TestCase;

public class OpenApiToJsonSchemaConverterTest extends TestCase {
    static String json = """
            {
              "openapi": "3.1.0",
              "info": {
                "title": "OpenWeatherMap One Call API",
                "description": "Provides access to current weather, hourly forecast, and daily forecast data.",
                "version": "v1.0.0"
              },
              "servers": [
                {
                  "url": "https://api.openweathermap.org"
                }
              ],
              "security":[
              {"apiKeyAuth":[]}
              ],
              "paths": {
                "/data/2.5/weather": {
                  "get": {
                    "description": "Retrieve current weather, hourly forecast, and daily forecast based on latitude and longitude.",
                    "operationId": "getWeatherData",
                    "parameters": [
                      {
                        "name": "lat",
                        "in": "query",
                        "required": true,
                        "description": "Latitude of the location.",
                        "schema": {
                          "type": "number"
                        }
                      },
                      {
                        "name": "lon",
                        "in": "query",
                        "required": true,
                        "description": "Longitude of the location.",
                        "schema": {
                          "type": "number"
                        }
                      },
                      {
                        "name": "appid",
                        "in": "query",
                        "required": true,
                        "description": "API key for authentication.",
                        "schema": {
                          "type": "string"
                        }
                      }
                    ],
                    "responses": {
                      "200": {
                        "description": "Successful response with weather data.",
                        "content": {
                          "application/json": {
                            "schema": {
                              "$ref": "#/components/schemas/WeatherResponse"
                            }
                          }
                        }
                      },
                      "401": {
                        "description": "Unauthorized due to missing or invalid API key."
                      }
                    }
                  }
                }
              },
              "components": {
                "schemas": {
                  "WeatherResponse": {
                    "type": "object",
                    "properties": {
                      "current": {
                        "type": "object",
                        "properties": {
                          "lat": {
                            "type": "number"
                          },
                          "lon": {
                            "type": "number"
                          },
                          "tz": {
                            "type": "string"
                          },
                          "date": {
                            "type": "string",
                            "format": "date"
                          },
                          "units": {
                            "type": "string"
                          },
                          "cloud_cover": {
                            "type": "object",
                            "properties": {
                              "afternoon": {
                                "type": "integer"
                              }
                            }
                          },
                          "humidity": {
                            "type": "object",
                            "properties": {
                              "afternoon": {
                                "type": "integer"
                              }
                            }
                          },
                          "precipitation": {
                            "type": "object",
                            "properties": {
                              "total": {
                                "type": "integer"
                              }
                            }
                          },
                          "temperature": {
                            "type": "object",
                            "properties": {
                              "min": { "type": "number" },
                              "max": { "type": "number" },
                              "afternoon": { "type": "number" },
                              "night": { "type": "number" },
                              "evening": { "type": "number" },
                              "morning": { "type": "number" }
                            }
                          },
                          "pressure": {
                            "type": "object",
                            "properties": {
                              "afternoon": {
                                "type": "integer"
                              }
                            }
                          },
                          "wind": {
                            "type": "object",
                            "properties": {
                              "max": {
                                "type": "object",
                                "properties": {
                                  "speed": { "type": "number" },
                                  "direction": { "type": "integer" }
                                }
                              }
                            }
                          }
                        }
                      },
                      "hourly": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "properties": {
                            "lat": {
                              "type": "number"
                            },
                            "lon": {
                              "type": "number"
                            },
                            "tz": {
                              "type": "string"
                            },
                            "date": {
                              "type": "string",
                              "format": "date"
                            },
                            "units": {
                              "type": "string"
                            },
                            "cloud_cover": {
                              "type": "object",
                              "properties": {
                                "afternoon": {
                                  "type": "integer"
                                }
                              }
                            },
                            "humidity": {
                              "type": "object",
                              "properties": {
                                "afternoon": {
                                  "type": "integer"
                                }
                              }
                            },
                            "precipitation": {
                              "type": "object",
                              "properties": {
                                "total": {
                                  "type": "integer"
                                }
                              }
                            },
                            "temperature": {
                              "type": "object",
                              "properties": {
                                "min": { "type": "number" },
                                "max": { "type": "number" },
                                "afternoon": { "type": "number" },
                                "night": { "type": "number" },
                                "evening": { "type": "number" },
                                "morning": { "type": "number" }
                              }
                            },
                            "pressure": {
                              "type": "object",
                              "properties": {
                                "afternoon": {
                                  "type": "integer"
                                }
                              }
                            },
                            "wind": {
                              "type": "object",
                              "properties": {
                                "max": {
                                  "type": "object",
                                  "properties": {
                                    "speed": { "type": "number" },
                                    "direction": { "type": "integer" }
                                  }
                                }
                              }
                            }
                          }
                        }
                      },
                      "daily": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "properties": {
                            "lat": {
                              "type": "number"
                            },
                            "lon": {
                              "type": "number"
                            },
                            "tz": {
                              "type": "string"
                            },
                            "date": {
                              "type": "string",
                              "format": "date"
                            },
                            "units": {
                              "type": "string"
                            },
                            "cloud_cover": {
                              "type": "object",
                              "properties": {
                                "afternoon": {
                                  "type": "integer"
                                }
                              }
                            },
                            "humidity": {
                              "type": "object",
                              "properties": {
                                "afternoon": {
                                  "type": "integer"
                                }
                              }
                            },
                            "precipitation": {
                              "type": "object",
                              "properties": {
                                "total": {
                                  "type": "integer"
                                }
                              }
                            },
                            "temperature": {
                              "type": "object",
                              "properties": {
                                "min": { "type": "number" },
                                "max": { "type": "number" },
                                "afternoon": { "type": "number" },
                                "night": { "type": "number" },
                                "evening": { "type": "number" },
                                "morning": { "type": "number" }
                              }
                            },
                            "pressure": {
                              "type": "object",
                              "properties": {
                                "afternoon": {
                                  "type": "integer"
                                }
                              }
                            },
                            "wind": {
                              "type": "object",
                              "properties": {
                                "max": {
                                  "type": "object",
                                  "properties": {
                                    "speed": { "type": "number" },
                                    "direction": { "type": "integer" }
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                },
                              "securitySchemes": {
                "apiKeyAuth": {
                  "type": "apiKey",
                  "in": "query",
                  "name": "appid"
                }
              }
              }
            }""";

    public static void main(String[] args) throws Exception {
        ConsoleServiceHandler consoleServiceHandler = new ConsoleServiceHandler();
        ConsoleServiceHandler.RequestOfParseOpenAPIDoc req = new ConsoleServiceHandler.RequestOfParseOpenAPIDoc();
        req.doc = json;
        AIPortServiceResponse<OpenAPIServerDoc> resp = new AIPortServiceResponse<>();
        consoleServiceHandler.parseOpenAPIDoc(req,resp);

        System.out.println(JSON.toJson(resp.data));
    }

}