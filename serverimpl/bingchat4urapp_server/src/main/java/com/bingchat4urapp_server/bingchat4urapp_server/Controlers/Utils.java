package com.bingchat4urapp_server.bingchat4urapp_server.Controlers;

import java.util.Map;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.bingchat4urapp_server.bingchat4urapp_server.Models.TaskModel;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class Utils {
    private ObjectMapper mapper = new ObjectMapper();
    private final Logger logger = org.slf4j.LoggerFactory.getLogger(Utils.class);

    public TaskModel createPromtTask(String promt, String timeOutForAnswer){
        // create String String Map that conatins promt and timeoutForAnswer
        Map<String, String> promtTask = Map.of("promt", promt, "timeOutForAnswer", timeOutForAnswer);
        // convert map to json string
        String jsonString = null;
        try {
            jsonString = mapper.writeValueAsString(promtTask);
        }
        catch (Exception e){
            logger.error("Can't serialize data", e);
            return null;
        }

        var newTask = new TaskModel();
        newTask.type = 2;
        newTask.data = jsonString;

        return newTask;
    }
}
