package com.bingchat4urapp_server.bingchat4urapp_server.Models;

public enum TaskType {
    SHUTDOWN(0, "Завершение работы"),
    AUTH(1, "Авторизация"),
    PROMPT(2, "Отправка запроса"),
    CREATE_CHAT(3, "Создание чата");

    private final int value;
    private final String description;

    TaskType(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static TaskType fromValue(int value) {
        for (TaskType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown task type: " + value);
    }
}