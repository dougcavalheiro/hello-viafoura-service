window.swaggerSpec={
  "openapi" : "3.0.0",
  "info" : {
    "title" : "Hello Viafoura Service",
    "description" : "This is a simple and light example on how Viafoura creates microservice projects",
    "version" : "1"
  },
  "servers" : [ {
    "url" : "http://localhost:8080"
  } ],
  "paths" : {
    "/v1/hello" : {
      "get" : {
        "operationId" : "getMyHello",
        "summary" : "Get the hello message",
        "description" : "This is a simple get request that provides a hello message",
        "parameters" : [ {
          "$ref" : "#/components/parameters/HelloMessageInput"
        } ],
        "responses" : {
          "200" : {
            "description" : "OK",
            "content" : {
              "application/json" : {
                "schema" : {
                  "$ref" : "#/components/schemas/HelloMessageOutput"
                }
              }
            }
          }
        },
        "tags" : [ "Hello_Viafoura_Operation" ]
      }
    }
  },
  "components" : {
    "securitySchemes" : {
      "TokenInCookie" : {
        "type" : "http",
        "scheme" : "basic",
        "description" : "Authentication using a JWT set in a cookie.  Different access levels can be set for each endpoint. Acceptable values are 'optional', 'user', 'mod' (moderator), 'admin', and 'client'. If more than one is specified, the most restrictive level is used.\n"
      },
      "Token" : {
        "type" : "http",
        "scheme" : "bearer",
        "description" : "Authentication using a JWT via a Bearer token.  Different access levels can be set for each endpoint. Acceptable values are 'optional', 'user', 'mod' (moderator), 'admin', and 'client'. If more than one is specified, the most restrictive level is used.\n"
      }
    },
    "parameters" : {
      "HelloMessageInput" : {
        "name" : "name",
        "required" : true,
        "in" : "query",
        "schema" : {
          "type" : "string"
        },
        "example" : "Lucy Skyrunner"
      }
    },
    "schemas" : {
      "HelloMessageOutput" : {
        "type" : "object",
        "description" : "A json object with the hello message content and service status\n",
        "properties" : {
          "message" : {
            "type" : "string"
          }
        }
      }
    }
  }
}