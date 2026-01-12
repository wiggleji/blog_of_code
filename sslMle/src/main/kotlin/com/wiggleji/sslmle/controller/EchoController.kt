package com.wiggleji.sslmle.controller

import com.wiggleji.sslmle.dto.EchoRequest
import com.wiggleji.sslmle.dto.EchoResponse
import mu.KotlinLogging
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api")
class EchoController {

    @PostMapping("/echo")
    fun echo(@RequestBody request: EchoRequest): EchoResponse {
        log.info { "[EchoController] Received message: ${request.message}" }

        return EchoResponse(
            message = request.message,
            echoedAt = Instant.now().toString(),
            serverInfo = "sslMle-demo-server"
        )
    }
}
