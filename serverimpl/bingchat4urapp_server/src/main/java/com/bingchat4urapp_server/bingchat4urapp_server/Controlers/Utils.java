package com.bingchat4urapp_server.bingchat4urapp_server.Controlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.bingchat4urapp_server.bingchat4urapp_server.Models.RequestsModels;
import com.bingchat4urapp_server.bingchat4urapp_server.Models.TaskModel;
import com.bingchat4urapp_server.bingchat4urapp_server.Models.TaskType;

@Component
public class Utils {
    @SuppressWarnings("unused")
    private final Logger logger = org.slf4j.LoggerFactory.getLogger(Utils.class);

    @Autowired
    private ResourceLoader resourceLoader;

    public String readFile(String filePath) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + filePath);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }

    public TaskModel createPromptTask(String prompt, String timeOutForAnswer){
        Map<String, String> promptTask = Map.of("prompt", prompt, "timeOutForAnswer", timeOutForAnswer);

        var newTask = new TaskModel();
        newTask.taskType = TaskType.PROMPT;
        newTask.data = promptTask;

        return newTask;
    }

    public TaskModel createAuthTask(String provider){
        Map<String, String> authTask = Map.of("provider", provider);
        var newTask = new TaskModel();
        newTask.taskType = TaskType.AUTH;
        newTask.data = authTask;

        return newTask;
    }

    public TaskModel createNewChatTask(){
        var newTask = new TaskModel();
        newTask.taskType = TaskType.CREATE_CHAT;
        return newTask;
    }

    public TaskModel createPromptTask(RequestsModels.PromptRequest promptRequest) {
        return createPromptTask(promptRequest.getPrompt(), promptRequest.getTimeOutForAnswer().toString());
    }

    public TaskModel createAuthTask(RequestsModels.AuthRequest authRequest){
        return createAuthTask(authRequest.getProvider().toString());
    }
}
