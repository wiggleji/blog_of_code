package com.wiggleji.sslmle.client

import com.wiggleji.sslmle.dto.EchoRequest
import com.wiggleji.sslmle.dto.EchoResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(
    name = "echoClient",
    url = "\${feign.client.echo.url}"
)
interface EchoClient {

    @PostMapping(
        value = ["/api/echo"],
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    fun echo(@RequestBody request: EchoRequest): EchoResponse
}
