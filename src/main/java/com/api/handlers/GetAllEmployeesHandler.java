package com.api.handlers;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.api.models.Employee;
import com.api.utils.ApiGatewayResponse;
import com.api.utils.TokenVerifier;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetAllEmployeesHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final AmazonDynamoDB dynamoDBClient;
    private final DynamoDB dynamoDB;
    private final Gson gson;

    public GetAllEmployeesHandler() {
        this.dynamoDBClient = AmazonDynamoDBClientBuilder.standard().build();
        this.dynamoDB = new DynamoDB(dynamoDBClient);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        context.getLogger().log("Received get all employees request");

        try {
            // Verificar token
            String token = request.getHeaders().get("Authorization");
            if (token == null) {
                token = request.getHeaders().get("authorization");
            }

            Map<String, Object> authResult = TokenVerifier.verifyToken(token);
            if (!(boolean) authResult.get("isValid")) {
                return ApiGatewayResponse.build(401, gson.toJson(Map.of("message", authResult.get("message"))));
            }

            // Obtener todos los empleados
            Table employeesTable = dynamoDB.getTable("Employees");
            ItemCollection<ScanOutcome> items = employeesTable.scan();

            List<Map<String, Object>> employees = new ArrayList<>();

            for (Item item : items) {
                Employee employee = Employee.fromDynamoDBItem(item);
                employees.add(employee.toMap());
            }

            // Construir respuesta
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("message", "Empleados obtenidos correctamente");
            responseBody.put("count", employees.size());
            responseBody.put("employees", employees);

            return ApiGatewayResponse.build(200, gson.toJson(responseBody));

        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return ApiGatewayResponse.build(500, gson.toJson(Map.of("message", "Error interno del servidor", "error", e.getMessage())));
        }
    }
}