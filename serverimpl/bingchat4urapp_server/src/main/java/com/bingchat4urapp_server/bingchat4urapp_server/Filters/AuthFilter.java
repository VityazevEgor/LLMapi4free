package com.bingchat4urapp_server.bingchat4urapp_server.Filters;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.bingchat4urapp_server.bingchat4urapp_server.BgTasks.CommandsExecutor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthFilter extends OncePerRequestFilter {
    @Autowired
    CommandsExecutor executor;

    private final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        antiSoftLock();
        if (executor.getWrapper().getWorkingLLM().isPresent()){
            filterChain.doFilter(request, response);
            logger.info("Passed request");
        }
        else{
            logger.info("Request filtered cuz there is no working LLM");
            response.sendRedirect("/auth");
        }
    }

    // to avoid situation when all providers in error state
    private void antiSoftLock(){
        if (executor.getWrapper().getLlms().stream().allMatch(llm->llm.getGotError() == true)){
            logger.warn("Detected soft lock! Resetting all LLM error states.");
            executor.getWrapper().getLlms().forEach(llm ->{
                llm.setGotError(false);
            });
        }
    }
    
}
