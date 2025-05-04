package com.api.utils;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ApiGatewayResponse {

    /**
     * Construye una respuesta para API Gateway con un código de estado y un mensaje
     * @param statusCode código HTTP de respuesta
     * @param body cuerpo de la respuesta
     * @return objeto APIGatewayProxyResponseEvent
     */
    public static APIGatewayProxyResponseEvent build(int statusCode, String body) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(headers)
                .withBody(body)
                .withIsBase64Encoded(false);
    }

    /**
     * Construye una respuesta para API Gateway con un código de estado y un mensaje
     * @param statusCode código HTTP de respuesta
     * @param body cuerpo de la respuesta
     * @param headers cabeceras adicionales
     * @return objeto APIGatewayProxyResponseEvent
     */
    public static APIGatewayProxyResponseEvent build(int statusCode, String body, Map<String, String> headers) {
        Map<String, String> allHeaders = new HashMap<>();
        allHeaders.put("Content-Type", "application/json");
        allHeaders.put("Access-Control-Allow-Origin", "*");
        allHeaders.put("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        allHeaders.put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");

        if (headers != null) {
            allHeaders.putAll(headers);
        }

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(allHeaders)
                .withBody(body)
                .withIsBase64Encoded(false);
    }
}