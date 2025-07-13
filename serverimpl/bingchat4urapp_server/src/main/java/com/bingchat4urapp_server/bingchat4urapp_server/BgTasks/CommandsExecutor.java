package com.bingchat4urapp_server.bingchat4urapp_server.BgTasks;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bingchat4urapp_server.bingchat4urapp_server.Shared;
import com.bingchat4urapp_server.bingchat4urapp_server.Models.TaskRepo;
import com.bingchat4urapp_server.bingchat4urapp_server.Models.TaskModel;
import com.bingchat4urapp_server.bingchat4urapp_server.Models.TaskType;
import com.bingchat4urapp_server.bingchat4urapp_server.Models.TaskData;
import com.bingchat4urapp_server.bingchat4urapp_server.Models.TaskResult;
import com.vityazev_egor.Wrapper;
import com.vityazev_egor.Wrapper.LLMproviders;
import com.vityazev_egor.Wrapper.WrapperMode;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;

@Component
public class CommandsExecutor {
    @Getter
    private Wrapper wrapper;
    private boolean doJob = true;
    private final Logger logger = LoggerFactory.getLogger(CommandsExecutor.class);
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    
    @Autowired
    private TaskRepo context;

    public CommandsExecutor(){
        try{
            wrapper = new Wrapper(Shared.proxy, LLMproviders.Copilot, Shared.examMode ? WrapperMode.ExamMode : WrapperMode.Normal);
            logger.info("Created Wrapper object with proxy = {}", Shared.proxy);
            if (Shared.emulateBingErros){
                logger.warn("emulateErrors mode is enabled. Copilot will always return errors");
                Wrapper.emulateError = true;
            }
            if (Shared.examMode)
                logger.warn("examMode mode is enabled. Server will try to get answer from other LLM provider is current one failed!");
        } catch (Exception e){
            logger.error("Could not create Wrapper object", e);
            System.exit(1);
        }
    }

    private static class CommandsProcessor implements Runnable {
        private final Wrapper wrapper;
        private final TaskRepo context;
        private final Logger logger;

        public CommandsProcessor(Wrapper wrapper, TaskRepo context, Logger logger) {
            this.wrapper = wrapper;
            this.context = context;
            this.logger = logger;
        }

        @Override
        public void run() {
            TaskModel task = context.findFirstUnfinishedTask();
            if (task == null) return;
            
            logger.info("Processing task of type: {}", task.taskType);
            
            try {
                TaskResult result = switch (task.taskType) {
                    case SHUTDOWN -> {
                        logger.info("Shutting down application");
                        System.exit(0);
                        yield null;
                    }
                    case AUTH -> processAuthTask(task);
                    case PROMPT -> processPromptTask(task);
                    case CREATE_CHAT -> processCreateChatTask(task);
                };
                
                if (result != null) {
                    task.applyResult(result);
                    context.save(task);
                }
            } catch (Exception ex) {
                logger.error("Error processing task", ex);
                task.applyResult(new TaskResult.Failure("Internal error", ex));
                context.save(task);
            }
        }

        private TaskResult processCreateChatTask(TaskModel task) {
            logger.info("Processing create chat task");
            try {
                var typedData = (TaskData.CreateChatData) task.getTypedData();
                
                return wrapper.getWorkingLLM()
                    .map(workingLLM -> {
                        boolean result = wrapper.createChat(workingLLM.getProvider());
                        logger.info("Create chat task completed with result: {}", result);
                        return result ? new TaskResult.Success() : new TaskResult.Failure("Failed to create chat");
                    })
                    .orElse(new TaskResult.Failure("No working LLM available"));
            } catch (Exception e) {
                logger.error("Error in create chat task", e);
                return new TaskResult.Failure("Error creating chat", e);
            }
        }

        private TaskResult processAuthTask(TaskModel task) {
            logger.info("Processing auth task");
            try {
                var typedData = (TaskData.AuthData) task.getTypedData();
                boolean result = wrapper.auth(typedData.provider());
                logger.info("Auth task completed for provider {} with result: {}", typedData.provider(), result);
                return result ? new TaskResult.Success() : new TaskResult.Failure("Authentication failed");
            } catch (Exception e) {
                logger.error("Error in auth task", e);
                return new TaskResult.Failure("Authentication error", e);
            }
        }

        private TaskResult processPromptTask(TaskModel task) {
            logger.info("Processing prompt task");
            try {
                var typedData = (TaskData.PromptData) task.getTypedData();
                var chatAnswer = wrapper.askLLM(typedData.prompt(), typedData.timeoutForAnswer());
                
                if (chatAnswer.getCleanAnswer().isEmpty()) {
                    return new TaskResult.Failure("No answer received from LLM");
                }
                
                String imageName = null;
                if (chatAnswer.getAnswerImage().isPresent()) {
                    try {
                        imageName = UUID.randomUUID() + ".png";
                        ImageIO.write(chatAnswer.getAnswerImage().get(), "png", 
                                    Paths.get(Shared.imagesPath.toString(), imageName).toFile());
                    } catch (IOException ex) {
                        logger.warn("Failed to save answer image, continuing without image", ex);
                    }
                }
                
                logger.info("Prompt task completed successfully");
                return new TaskResult.Success(
                    chatAnswer.getCleanAnswer().get(),
                    chatAnswer.getHtmlAnswer().orElse(null),
                    imageName
                );
            } catch (Exception e) {
                logger.error("Error in prompt task", e);
                return new TaskResult.Failure("Error processing prompt", e);
            }
        }
    }

    @PostConstruct
    public void startTask(){
        if (doJob) {
            scheduledExecutorService.scheduleWithFixedDelay(
                new CommandsProcessor(wrapper, context, logger), 
                2, 
                2, 
                TimeUnit.SECONDS
            );
        }
    }

    @PreDestroy
    public void stopTask() {
        doJob = false;
        scheduledExecutorService.shutdown();
        try {
            if (!scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        wrapper.exit();
    }

 
}
