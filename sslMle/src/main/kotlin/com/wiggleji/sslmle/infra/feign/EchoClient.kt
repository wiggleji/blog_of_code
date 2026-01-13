package com.wiggleji.sslmle.infra.feign

import com.wiggleji.sslmle.config.FeignConfig
import com.wiggleji.sslmle.config.HttpClientConfig
import com.wiggleji.sslmle.config.mle.MleConfig
import com.wiggleji.sslmle.dto.EchoRequest
import com.wiggleji.sslmle.dto.EchoResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(
    name = "echoClient",
    url = "\${feign.client.echo.url}",
    configuration = [FeignConfig::class, HttpClientConfig::class, MleConfig::class]
)
interface EchoClient {

    @PostMapping(
        value = ["/api/echo"],
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    fun echo(@RequestBody request: EchoRequest): EchoResponse
}
