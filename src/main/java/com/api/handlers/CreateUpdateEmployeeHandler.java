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
import com.api.models.Employee;
import com.api.utils.ApiGatewayResponse;
import com.api.utils.TokenVerifier;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateUpdateEmployeeHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final AmazonDynamoDB dynamoDBClient;
    private final DynamoDB dynamoDB;
    private final Gson gson;

    public CreateUpdateEmployeeHandler() {
        this.dynamoDBClient = AmazonDynamoDBClientBuilder.standard().build();
        this.dynamoDB = new DynamoDB(dynamoDBClient);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        context.getLogger().log("Received create/update employee request");

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

            // Parsear solicitud
            Employee employee = gson.fromJson(request.getBody(), Employee.class);

            // Validación básica
            if (employee.getNombre() == null || employee.getNombre().trim().isEmpty() ||
                    employee.getEmail() == null || employee.getEmail().trim().isEmpty()) {
                return ApiGatewayResponse.build(400, gson.toJson(Map.of("message", "El nombre y el email son obligatorios")));
            }

            // Si se proporciona un ID, es una actualización; de lo contrario, es una creación
            boolean isUpdate = employee.getId() != null && !employee.getId().trim().isEmpty();
            String id = isUpdate ? employee.getId() : UUID.randomUUID().toString();
            employee.setId(id);

            // Si es una actualización, verificar que el empleado existe
            if (isUpdate) {
                Table employeesTable = dynamoDB.getTable("Employees");
                GetItemSpec spec = new GetItemSpec()
                        .withPrimaryKey("id", id);

                Item existingItem = employeesTable.getItem(spec);
                if (existingItem == null) {
                    return ApiGatewayResponse.build(404, gson.toJson(Map.of("message", "Empleado no encontrado")));
                }
            }

            // Si se proporciona supervisor_id, verificar que existe
            String supervisorId = employee.getSupervisor_id();
            if (supervisorId != null && !supervisorId.trim().isEmpty()) {
                Table employeesTable = dynamoDB.getTable("Employees");
                GetItemSpec spec = new GetItemSpec()
                        .withPrimaryKey("id", supervisorId);

                Item supervisorItem = employeesTable.getItem(spec);
                if (supervisorItem == null) {
                    return ApiGatewayResponse.build(400, gson.toJson(Map.of("message", "El supervisor_id proporcionado no existe")));
                }
            }

            // Establecer la fecha de última actualización
            employee.setLastUpdated(Instant.now().toString());

            // Guardar en DynamoDB
            Table employeesTable = dynamoDB.getTable("Employees");
            employeesTable.putItem(employee.toDynamoDBItem());

            // Construir respuesta
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("message", isUpdate ? "Empleado actualizado correctamente" : "Empleado creado correctamente");
            responseBody.put("employee", employee.toMap());

            return ApiGatewayResponse.build(isUpdate ? 200 : 201, gson.toJson(responseBody));

        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return ApiGatewayResponse.build(500, gson.toJson(Map.of("message", "Error interno del servidor", "error", e.getMessage())));
        }
    }
}