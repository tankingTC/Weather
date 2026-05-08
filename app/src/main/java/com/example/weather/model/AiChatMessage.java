package com.example.weather.model;

public class AiChatMessage {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    private final String role;
    private final String content;
    private final boolean loading;

    public AiChatMessage(String role, String content) {
        this(role, content, false);
    }

    public AiChatMessage(String role, String content, boolean loading) {
        this.role = role;
        this.content = content;
        this.loading = loading;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public boolean isLoading() {
        return loading;
    }

    public boolean isUser() {
        return ROLE_USER.equals(role);
    }
}
