package com.comp5619.libraryreservationreviewhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextListener;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/books/**")
                .addResourceLocations("classpath:/static/books/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // only acts on handlers annotated with @AuthCheck
        registry.addInterceptor(authInterceptor).addPathPatterns("/**");
    }

}