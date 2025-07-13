package com.bingchat4urapp_server.bingchat4urapp_server.Models;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.*;

public interface TaskRepo extends JpaRepository<TaskModel, Integer> {
    @Query("SELECT t FROM TaskModel t WHERE t.isFinished = false ORDER BY t.id ASC LIMIT 1")
    TaskModel findFirstUnfinishedTask();

    @Query("SELECT t FROM TaskModel t WHERE t.taskType = :taskType ORDER BY t.id DESC LIMIT 1")
    TaskModel findLastTaskByType(TaskType taskType);

    @Query("SELECT t FROM TaskModel t WHERE t.taskType = :taskType ORDER BY t.id ASC LIMIT 1")
    TaskModel findFirstTaskByType(TaskType taskType);

    @Query("SELECT t FROM TaskModel t WHERE t.isFinished = true ORDER BY t.id DESC LIMIT 1")
    TaskModel findLastFinishedTask();

    @Query("SELECT t FROM TaskModel t WHERE t.isFinished = true AND t.gotError = false AND t.taskType = :taskType ORDER BY t.id DESC LIMIT :limit")
    List<TaskModel> findLatestFinishedTasksByType(TaskType taskType, int limit);

    default TaskModel findLastAuthTask() {
        return findLastTaskByType(TaskType.AUTH);
    }

    default TaskModel findCreateChatTask() {
        return findFirstTaskByType(TaskType.CREATE_CHAT);
    }

    default List<TaskModel> findLatestFinishedPromptTasks() {
        return findLatestFinishedTasksByType(TaskType.PROMPT, 5);
    }
}
