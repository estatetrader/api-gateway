package com.estatetrader.apigw.core.utils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.converter.Converter;

import java.util.*;

@Configuration
public class Configurations {
    @Bean
    public ConversionServiceFactoryBean conversionService(List<Converter<?, ?>> converters) {
        ConversionServiceFactoryBean cfb = new ConversionServiceFactoryBean();
        cfb.setConverters(new HashSet<>(converters));
        return cfb;
    }
}
