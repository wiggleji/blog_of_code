package com.wiggleji.sslmle.dto

data class EchoResponse(
    val message: String,
    val echoedAt: String,
    val serverInfo: String
)
