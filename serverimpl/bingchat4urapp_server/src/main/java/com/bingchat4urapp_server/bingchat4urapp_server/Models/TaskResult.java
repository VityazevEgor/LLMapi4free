package com.bingchat4urapp_server.bingchat4urapp_server.Models;

import java.util.Optional;

public sealed interface TaskResult permits TaskResult.Success, TaskResult.Failure {
    
    boolean isSuccess();
    
    default boolean isFailure() {
        return !isSuccess();
    }
    
    record Success(
        Optional<String> result,
        Optional<String> htmlResult,
        Optional<String> imageResult
    ) implements TaskResult {
        
        public Success() {
            this(Optional.empty(), Optional.empty(), Optional.empty());
        }
        
        public Success(String result) {
            this(Optional.ofNullable(result), Optional.empty(), Optional.empty());
        }
        
        public Success(String result, String htmlResult, String imageResult) {
            this(Optional.ofNullable(result), 
                 Optional.ofNullable(htmlResult), 
                 Optional.ofNullable(imageResult));
        }
        
        @Override
        public boolean isSuccess() {
            return true;
        }
    }
    
    record Failure(String reason, Optional<Throwable> cause) implements TaskResult {
        
        public Failure(String reason) {
            this(reason, Optional.empty());
        }
        
        public Failure(String reason, Throwable cause) {
            this(reason, Optional.of(cause));
        }
        
        @Override
        public boolean isSuccess() {
            return false;
        }
    }
}