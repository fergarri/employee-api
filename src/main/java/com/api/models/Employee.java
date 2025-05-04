package com.api.models;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.document.Item;

public class Employee {
    private String id;
    private String nombre;
    private String email;
    private String supervisor_id;
    private String lastUpdated;
    private Integer directReportsCount;

    public Employee() {
    }

    public Employee(String id, String nombre, String email, String supervisor_id, String lastUpdated) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.supervisor_id = supervisor_id;
        this.lastUpdated = lastUpdated;
    }

    // Constructor a partir de un Item de DynamoDB
    public static Employee fromDynamoDBItem(Item item) {
        if (item == null) {
            return null;
        }

        Employee employee = new Employee();
        employee.setId(item.getString("id"));
        employee.setNombre(item.getString("nombre"));
        employee.setEmail(item.getString("email"));
        employee.setSupervisor_id(item.getString("supervisor_id"));
        employee.setLastUpdated(item.getString("lastUpdated"));

        return employee;
    }

    // Convertir a Item de DynamoDB
    public Item toDynamoDBItem() {
        Item item = new Item()
                .withPrimaryKey("id", this.id)
                .withString("nombre", this.nombre)
                .withString("email", this.email)
                .withString("lastUpdated", Instant.now().toString());

        if (this.supervisor_id != null && !this.supervisor_id.isEmpty()) {
            item.withString("supervisor_id", this.supervisor_id);
        } else {
            item.withNull("supervisor_id");
        }

        return item;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSupervisor_id() {
        return supervisor_id;
    }

    public void setSupervisor_id(String supervisor_id) {
        this.supervisor_id = supervisor_id;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Integer getDirectReportsCount() {
        return directReportsCount;
    }

    public void setDirectReportsCount(Integer directReportsCount) {
        this.directReportsCount = directReportsCount;
    }

    // Convertir a Map para respuesta JSON
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", this.id);
        map.put("nombre", this.nombre);
        map.put("email", this.email);
        map.put("supervisor_id", this.supervisor_id);
        map.put("lastUpdated", this.lastUpdated);

        if (this.directReportsCount != null) {
            map.put("directReportsCount", this.directReportsCount);
        }

        return map;
    }
}