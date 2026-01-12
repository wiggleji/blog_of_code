package com.wiggleji.sslmle.controller

import com.wiggleji.sslmle.client.EchoClient
import com.wiggleji.sslmle.dto.EchoRequest
import com.wiggleji.sslmle.dto.EchoResponse
import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/test")
class TestController(
    private val echoClient: EchoClient
) {

    @GetMapping("/feign")
    fun testFeign(@RequestParam(defaultValue = "Hello from Feign!") message: String): EchoResponse {
        log.info { "[TestController] Testing Feign client with message: $message" }

        val request = EchoRequest(message = message)
        val response = echoClient.echo(request)

        log.info { "[TestController] Received response: $response" }
        return response
    }
}
