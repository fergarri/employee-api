package com.api.handlers;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.api.models.LoginRequest;
import com.api.utils.ApiGatewayResponse;
import com.api.utils.TokenGenerator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LoginHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final AmazonDynamoDB dynamoDBClient;
    private final DynamoDB dynamoDB;
    private final Gson gson;

    public LoginHandler() {
        this.dynamoDBClient = AmazonDynamoDBClientBuilder.standard().build();
        this.dynamoDB = new DynamoDB(dynamoDBClient);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        context.getLogger().log("Received login request");

        try {
            // Parse request body
            LoginRequest loginRequest = gson.fromJson(request.getBody(), LoginRequest.class);

            // Validate request
            if (loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
                return ApiGatewayResponse.build(400, "Se requiere nombre de usuario y contraseña");
            }

            // Get expiration time (default 5 minutes)
            int expirationMinutes = loginRequest.getExpirationMinutes() != null ? loginRequest.getExpirationMinutes() : 5;

            // Check if user exists
            Table usersTable = dynamoDB.getTable("Users");
            GetItemSpec spec = new GetItemSpec()
                    .withPrimaryKey("username", loginRequest.getUsername());

            Item userItem = usersTable.getItem(spec);

            // If user doesn't exist or password doesn't match
            if (userItem == null || !userItem.getString("password").equals(loginRequest.getPassword())) {
                return ApiGatewayResponse.build(401, "Credenciales inválidas");
            }

            // Generate token
            String token = TokenGenerator.generateToken();
            long expirationTime = TokenGenerator.calculateExpirationTime(expirationMinutes);
            String userId = userItem.getString("id");

            // Save token to DynamoDB
            Table tokensTable = dynamoDB.getTable("Tokens");
            Item tokenItem = new Item()
                    .withPrimaryKey("token", token)
                    .withString("username", loginRequest.getUsername())
                    .withString("userId", userId)
                    .withNumber("expiresAt", expirationTime);

            tokensTable.putItem(tokenItem);

            // Build response
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("token", token);
            responseBody.put("expiresAt", expirationTime);
            responseBody.put("expirationMinutes", expirationMinutes);

            return ApiGatewayResponse.build(200, gson.toJson(responseBody));

        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return ApiGatewayResponse.build(500, "Error interno del servidor: " + e.getMessage());
        }
    }
}