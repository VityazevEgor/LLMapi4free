package com.bingchat4urapp_server.bingchat4urapp_server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bingchat4urapp_server.bingchat4urapp_server.Filters.AntiSoftLockFilter;

@Configuration
public class FiltersCfg {
    @Bean
    @Autowired
    public FilterRegistrationBean<AntiSoftLockFilter> registerAntiSoftLockFilter(AntiSoftLockFilter filter) {
        FilterRegistrationBean<AntiSoftLockFilter> registrationBean = new FilterRegistrationBean<AntiSoftLockFilter>();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(0);
        return registrationBean;
    }
}