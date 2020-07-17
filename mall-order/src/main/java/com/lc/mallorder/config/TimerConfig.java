package com.lc.mallorder.config;

import com.lc.mallorder.common.timer.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimerConfig {
    @Autowired
    private Parameters parameters;
    @Bean
    public Timer timer(){
        return new Timer(parameters.getTick(),parameters.getWheelsize());
    }
}
