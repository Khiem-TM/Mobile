package com.vitalai.core.error

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertTrue
import org.junit.Test

class AppErrorMapperTest {

    @Test
    fun `maps unauthorized http code`() {
        val error = AppErrorMapper.fromHttpCode(401)

        assertTrue(error is AppError.Unauthorized)
    }

    @Test
    fun `maps network exceptions`() {
        val unknownHost = AppErrorMapper.fromThrowable(UnknownHostException())
        val timeout = AppErrorMapper.fromThrowable(SocketTimeoutException())

        assertTrue(unknownHost is AppError.Network)
        assertTrue(timeout is AppError.Network)
    }
}
