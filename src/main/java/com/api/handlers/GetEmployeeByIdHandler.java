package com.api.handlers;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.api.models.Employee;
import com.api.utils.ApiGatewayResponse;
import com.api.utils.TokenVerifier;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class GetEmployeeByIdHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final AmazonDynamoDB dynamoDBClient;
    private final DynamoDB dynamoDB;
    private final Gson gson;

    public GetEmployeeByIdHandler() {
        this.dynamoDBClient = AmazonDynamoDBClientBuilder.standard().build();
        this.dynamoDB = new DynamoDB(dynamoDBClient);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        context.getLogger().log("Received get employee by ID request");

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

            // Obtener el ID del empleado
            String employeeId = request.getPathParameters().get("id");
            if (employeeId == null || employeeId.trim().isEmpty()) {
                return ApiGatewayResponse.build(400, gson.toJson(Map.of("message", "ID de empleado no proporcionado")));
            }

            // Obtener detalles del empleado y la cantidad de empleados a su cargo en paralelo
            Table employeesTable = dynamoDB.getTable("Employees");

            // Ejecutar ambas consultas en paralelo usando CompletableFuture
            CompletableFuture<Item> employeeDetailsFuture = CompletableFuture.supplyAsync(() -> {
                GetItemSpec spec = new GetItemSpec()
                        .withPrimaryKey("id", employeeId);
                return employeesTable.getItem(spec);
            });

            CompletableFuture<Integer> directReportsCountFuture = CompletableFuture.supplyAsync(() -> {
                ScanSpec scanSpec = new ScanSpec()
                        .withFilterExpression("supervisor_id = :supervisorId")
                        .withValueMap(Map.of(":supervisorId", employeeId));

                ItemCollection<ScanOutcome> result = employeesTable.scan(scanSpec);
                int count = 0;
                for (Item ignored : result) {
                    count++;
                }
                return count;
            });

            // Esperar a que ambas consultas se completen
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    employeeDetailsFuture,
                    directReportsCountFuture
            );

            // Esperar hasta que se completen ambas consultas
            allFutures.get();

            // Obtener los resultados
            Item employeeItem = employeeDetailsFuture.get();
            int directReportsCount = directReportsCountFuture.get();

            // Si el empleado no existe
            if (employeeItem == null) {
                return ApiGatewayResponse.build(404, gson.toJson(Map.of("message", "Empleado no encontrado")));
            }

            // Convertir el Item a un objeto Employee
            Employee employee = Employee.fromDynamoDBItem(employeeItem);

            // Agregar la cantidad de empleados a cargo
            employee.setDirectReportsCount(directReportsCount);

            // Construir respuesta
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("message", "Empleado obtenido correctamente");
            responseBody.put("employee", employee.toMap());

            return ApiGatewayResponse.build(200, gson.toJson(responseBody));

        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return ApiGatewayResponse.build(500, gson.toJson(Map.of("message", "Error interno del servidor", "error", e.getMessage())));
        }
    }
}