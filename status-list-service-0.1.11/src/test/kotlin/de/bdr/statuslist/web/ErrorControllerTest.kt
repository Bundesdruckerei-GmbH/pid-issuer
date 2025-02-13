/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.web

import jakarta.servlet.RequestDispatcher
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@WebMvcTest(ErrorController::class)
class ErrorControllerTest(@Autowired val mockMvc: MockMvc) {
    @Test
    fun `should return error status`() {
        arrayOf(HttpStatus.OK, HttpStatus.BAD_REQUEST).forEach {
            mockMvc
                .perform(
                    MockMvcRequestBuilders.get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, it.value())
                )
                .andExpect(MockMvcResultMatchers.status().`is`(it.value()))
                .andExpect(MockMvcResultMatchers.content().string(it.reasonPhrase))
        }
    }
}
