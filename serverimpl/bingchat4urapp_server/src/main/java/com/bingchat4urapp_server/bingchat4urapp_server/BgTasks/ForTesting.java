package com.bingchat4urapp_server.bingchat4urapp_server.BgTasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.bingchat4urapp_server.bingchat4urapp_server.Controlers.Utils;
import com.bingchat4urapp_server.bingchat4urapp_server.Models.TaskRepo;
import com.bingchat4urapp_server.bingchat4urapp_server.Models.TaskModel;
import com.bingchat4urapp_server.bingchat4urapp_server.Models.TaskType;

@Component
public class ForTesting implements CommandLineRunner{

    private final Boolean isDebug = true;

    @Autowired
    private TaskRepo context;

    @Autowired
    private Utils utils;

    @Override
    public void run(String... args) throws Exception {
        if (isDebug){
            for (int i=0; i<2; i++){
                TaskModel model = new TaskModel();
                model.result = String.format("Task #%s", String.valueOf(i));
                model.gotError = false;
                model.isFinished = true;
                model.taskType = TaskType.PROMPT;

                context.save(model);
            }

            var model = new TaskModel();
            model.result = utils.readFile("forTest/answerWithMath.txt");
            model.htmlResult = utils.readFile("forTest/answerWithMath.html");
            model.taskType = TaskType.PROMPT;
            model.isFinished = true;
            model.gotError = false;
            context.save(model);
        }
    }
    
}
