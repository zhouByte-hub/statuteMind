package com.zhoubyte.core.config;

import com.zhoubyte.context.ScorpioContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContextConfig {

    @Bean
    public ScorpioContext scorpioContext(){
        return new ScorpioContext();
    }
}
