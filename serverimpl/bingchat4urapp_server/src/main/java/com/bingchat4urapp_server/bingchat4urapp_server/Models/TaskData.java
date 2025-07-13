package com.bingchat4urapp_server.bingchat4urapp_server.Models;

import com.vityazev_egor.Wrapper.LLMproviders;
import java.util.Map;
import java.util.Optional;

public sealed interface TaskData permits TaskData.AuthData, TaskData.PromptData, TaskData.CreateChatData, TaskData.ShutdownData {
    
    static TaskData fromMap(TaskType type, Map<String, String> data) {
        return switch (type) {
            case AUTH -> new AuthData(
                LLMproviders.valueOf(data.get("provider"))
            );
            case PROMPT -> new PromptData(
                data.get("prompt"),
                Optional.ofNullable(data.get("timeOutForAnswer"))
                    .map(Integer::parseInt)
                    .orElse(30)
            );
            case CREATE_CHAT -> new CreateChatData();
            case SHUTDOWN -> new ShutdownData();
        };
    }
    
    record AuthData(LLMproviders provider) implements TaskData {
        public AuthData {
            if (provider == null) {
                throw new IllegalArgumentException("Provider cannot be null");
            }
        }
    }
    
    record PromptData(String prompt, int timeoutForAnswer) implements TaskData {
        public PromptData {
            if (prompt == null || prompt.trim().isEmpty()) {
                throw new IllegalArgumentException("Prompt cannot be null or empty");
            }
            if (timeoutForAnswer <= 0) {
                throw new IllegalArgumentException("Timeout must be positive");
            }
        }
    }
    
    record CreateChatData() implements TaskData {}
    
    record ShutdownData() implements TaskData {}
}