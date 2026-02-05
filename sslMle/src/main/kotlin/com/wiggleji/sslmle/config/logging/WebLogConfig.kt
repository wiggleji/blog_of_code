package com.wiggleji.sslmle.config.logging

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebLogConfig(
    private val logIdInterceptor: LogIdInterceptor
): WebMvcConfigurer {

    /**
     * Spring Web MVC 요청 속 LogId 주입을 위한
     * LogIdInterceptor 등록
     */
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(logIdInterceptor)
    }
}
