package com.api.utils;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class TokenVerifier {

    private static final AmazonDynamoDB dynamoDBClient = AmazonDynamoDBClientBuilder.standard().build();
    private static final DynamoDB dynamoDB = new DynamoDB(dynamoDBClient);

    /**
     * Verifica si un token es válido
     * @param token el token a verificar
     * @return un Map con el resultado de la verificación
     */
    public static Map<String, Object> verifyToken(String token) {
        Map<String, Object> result = new HashMap<>();

        if (token == null || token.trim().isEmpty()) {
            result.put("isValid", false);
            result.put("message", "Token no proporcionado");
            return result;
        }

        try {
            // Buscar el token en DynamoDB
            Table tokensTable = dynamoDB.getTable("Tokens");
            GetItemSpec spec = new GetItemSpec()
                    .withPrimaryKey("token", token);

            Item tokenItem = tokensTable.getItem(spec);

            // Si el token no existe
            if (tokenItem == null) {
                result.put("isValid", false);
                result.put("message", "Token inválido");
                return result;
            }

            // Verificar si el token ha expirado
            long expiresAt = tokenItem.getNumber("expiresAt").longValue();
            long currentTime = Instant.now().getEpochSecond();

            if (expiresAt < currentTime) {
                result.put("isValid", false);
                result.put("message", "Token expirado");
                return result;
            }

            // Token válido, devolver información del usuario
            result.put("isValid", true);
            result.put("userId", tokenItem.getString("userId"));
            result.put("username", tokenItem.getString("username"));

            return result;

        } catch (Exception e) {
            result.put("isValid", false);
            result.put("message", "Error al verificar token: " + e.getMessage());
            return result;
        }
    }
}