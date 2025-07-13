package com.bingchat4urapp_server.bingchat4urapp_server.Models;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.validation.constraints.NotNull;

@Entity
public class TaskModel {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Integer id;

    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    public TaskType taskType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_data", joinColumns = @JoinColumn(name = "task_id"))
    @MapKeyColumn(name = "data_key")
    @Column(name = "data_value")
    public Map<String, String> data = new HashMap<>();

    @Column(nullable = false)
    public boolean isFinished = false;

    @Column(nullable = false)
    public boolean gotError = false;

    public String result;
    public String htmlResult;
    public String imageResult;

    public TaskData getTypedData() {
        if (taskType == null) {
            throw new IllegalStateException("Task type is not set");
        }
        return TaskData.fromMap(taskType, data);
    }

    public void applyResult(TaskResult result) {
        this.isFinished = true;
        switch (result) {
            case TaskResult.Success success -> {
                this.gotError = false;
                this.result = success.result().orElse(null);
                this.htmlResult = success.htmlResult().orElse(null);
                this.imageResult = success.imageResult().orElse(null);
            }
            case TaskResult.Failure failure -> {
                this.gotError = true;
                this.result = failure.reason();
                this.htmlResult = null;
                this.imageResult = null;
            }
        }
    }
}
