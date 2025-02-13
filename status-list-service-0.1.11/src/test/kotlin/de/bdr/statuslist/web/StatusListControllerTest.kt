/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.web

import com.ninjasquad.springmockk.MockkBean
import de.bdr.statuslist.config.AppConfiguration
import de.bdr.statuslist.service.StatusListService
import de.bdr.statuslist.service.StatusListTokenSource
import de.bdr.statuslist.service.TokenData
import de.bdr.statuslist.service.TokenFormat
import de.bdr.statuslist.stats.StatsService
import io.mockk.every
import java.time.Instant
import java.util.UUID
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.startsWith
import org.hamcrest.text.IsEmptyString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(StatusListController::class)
class StatusListControllerTest(@Autowired val mockMvc: MockMvc) {
    @MockkBean lateinit var appConfiguration: AppConfiguration
    @MockkBean(relaxed = true) lateinit var statusListTokenSource: StatusListTokenSource
    @MockkBean(relaxed = true) lateinit var statusListService: StatusListService
    @MockkBean(relaxed = true) lateinit var statsService: StatsService

    private val listId = UUID.randomUUID()
    private val appUrl = "http://localhost:8080"

    private val statusListJson =
        """
        {
          "bits": 1,
          "lst": "eNpjYEAFAAAQAAE",
          "aggregation_uri": "$appUrl/aggregation/test-pool"
        }"""
            .trimIndent()
    private val statusListJwt =
        """
            eyJ4NWMiOlsiTUlJQ1pqQ0NBZzJnQXdJQkFnSUJDakFLQmdncWhrak9QUVFEQWpCak1Rc3dDUVlEVlFRR0V3SkVSVEVQTUEwR0ExVUVCd3dHUW1WeWJHbHVNUjB3R3dZRFZRUUtEQlJDZFc1a1pYTmtjblZqYTJWeVpXa2dSMjFpU0RFS01BZ0dBMVVFQ3d3QlNURVlNQllHQTFVRUF3d1BTVVIxYm1sdmJpQlVaWE4wSUVOQk1CNFhEVEkwTURVeU9UQTJOVEUwTWxvWERUSTFNRGN3TXpBMk5URTBNbG93V1RFTE1Ba0dBMVVFQmhNQ1JFVXhIVEFiQmdOVkJBb01GRUoxYm1SbGMyUnlkV05yWlhKbGFTQkhiV0pJTVFvd0NBWURWUVFMREFGSk1SOHdIUVlEVlFRRERCWldaWEpwWm1sbFpDQkZMVTFoYVd3Z1NYTnpkV1Z5TUZrd0V3WUhLb1pJemowQ0FRWUlLb1pJemowREFRY0RRZ0FFVFVEWC9USG14MnVhdVRrUmhTSERCd0xMSVNyUmgyUTU4WmpHV0lKY3JGVWFWUEF2QkRUSER3ekVYVHpRbDRrTFRCSml6V2d1UkYrVmU3aWo3VkY3cnFPQnV6Q0J1REFkQmdOVkhRNEVGZ1FVT2doRVpnb3E4WDhOVXlTVnJtRUREcWZPSDdNd0RBWURWUjBUQVFIL0JBSXdBREFPQmdOVkhROEJBZjhFQkFNQ0I0QXdXQVlEVlIwUkJGRXdUNEloYVhOemRXVnlMVzl3Wlc1cFpEUjJZeTV6YzJrdWRHbHlMbUoxWkhKMUxtUmxoaXBvZEhSd2N6b3ZMMmx6YzNWbGNpMXZjR1Z1YVdRMGRtTXVjM05wTG5ScGNpNWlkV1J5ZFM1a1pTOHdId1lEVlIwakJCZ3dGb0FVVDViclB0cU5PNlo3SDVod1Z1ZzJ0VjRMYTFFd0NnWUlLb1pJemowRUF3SURSd0F3UkFJZ05tWFpxVUdXSGFMUkN5SXVWNThDS0QyRFF6NFhseEVOQTRvNVhyR1NQQlFDSUM5NXpOMjAva21PQ0lkUEVHQlJHNE9FTGIvNFM3QmZqTzZ4aWhpd0I1STEiLCJNSUlDTFRDQ0FkU2dBd0lCQWdJVU1ZVUhoR0Q5aFUvYzBFbzZtVzhyamplSit0MHdDZ1lJS29aSXpqMEVBd0l3WXpFTE1Ba0dBMVVFQmhNQ1JFVXhEekFOQmdOVkJBY01Ca0psY214cGJqRWRNQnNHQTFVRUNnd1VRblZ1WkdWelpISjFZMnRsY21WcElFZHRZa2d4Q2pBSUJnTlZCQXNNQVVreEdEQVdCZ05WQkFNTUQwbEVkVzVwYjI0Z1ZHVnpkQ0JEUVRBZUZ3MHlNekEzTVRNd09USTFNamhhRncwek16QTNNVEF3T1RJMU1qaGFNR014Q3pBSkJnTlZCQVlUQWtSRk1ROHdEUVlEVlFRSERBWkNaWEpzYVc0eEhUQWJCZ05WQkFvTUZFSjFibVJsYzJSeWRXTnJaWEpsYVNCSGJXSklNUW93Q0FZRFZRUUxEQUZKTVJnd0ZnWURWUVFEREE5SlJIVnVhVzl1SUZSbGMzUWdRMEV3V1RBVEJnY3Foa2pPUFFJQkJnZ3Foa2pPUFFNQkJ3TkNBQVNFSHo4WWpyRnlUTkhHTHZPMTRFQXhtOXloOGJLT2drVXpZV2NDMWN2ckpuNUpnSFlITXhaYk5NTzEzRWgwRXIyNzM4UVFPZ2VSb1pNSVRhb2RrZk5TbzJZd1pEQWRCZ05WSFE0RUZnUVVUNWJyUHRxTk82WjdINWh3VnVnMnRWNExhMUV3SHdZRFZSMGpCQmd3Rm9BVVQ1YnJQdHFOTzZaN0g1aHdWdWcydFY0TGExRXdFZ1lEVlIwVEFRSC9CQWd3QmdFQi93SUJBREFPQmdOVkhROEJBZjhFQkFNQ0FZWXdDZ1lJS29aSXpqMEVBd0lEUndBd1JBSWdZMERlcmRDeHQ0ekdQWW44eU5yRHhJV0NKSHB6cTRCZGpkc1ZOMm8xR1JVQ0lCMEtBN2JHMUZWQjFJaUs4ZDU3UUFMK1BHOVg1bGRLRzdFa29BbWhXVktlIl0sImtpZCI6Ik1Hd3daNlJsTUdNeEN6QUpCZ05WQkFZVEFrUkZNUTh3RFFZRFZRUUhEQVpDWlhKc2FXNHhIVEFiQmdOVkJBb01GRUoxYm1SbGMyUnlkV05yWlhKbGFTQkhiV0pJTVFvd0NBWURWUVFMREFGSk1SZ3dGZ1lEVlFRRERBOUpSSFZ1YVc5dUlGUmxjM1FnUTBFQ0FRbz0iLCJ0eXAiOiJzdGF0dXNsaXN0K2p3dCIsImFsZyI6IkVTMjU2In0
            .eyJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAiLCJzdWIiOiJodHRwOi8vbG9jYWxob3N0OjgwODUvYjdmNDFjZjMtYTM0OC00Zjk2LWI0MzAtYmM3YmJiYmMwMzMxIiwiZXhwIjoxNzI2MTUzNDQ2LCJpYXQiOjE3MjYxNTM0MzEsInR0bCI6MTAsInN0YXR1c19saXN0Ijp7ImJpdHMiOjEsImxzdCI6ImVOcGpZRUFGQUFBUUFBRSIsImFnZ3JlZ2F0aW9uX3VyaSI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4NS9hZ2dyZWdhdGlvbi92ZXJpZmllZC1lbWFpbCJ9fQ
            .cY1NVWYiI3FwDyZ8WqMC6JpwaKc42cQmbvGseahfUoQ_63LyNxhFKvB3pagmvJaTm3mCU0KctRwozVvBFEFJbQ
        """

    private val contentTypeJson = "application/statuslist+json"
    private val contentTypeJwt = "application/statuslist+jwt"

    @Test
    fun `should return status list as json`() {
        val lastModified = Instant.now()
        every { appConfiguration.publicUrl } returns appUrl
        every { statusListTokenSource.load(listId, TokenFormat.JSON) } returns
            TokenData(statusListJson.toByteArray(), lastModified)

        mockMvc
            .perform(MockMvcRequestBuilders.get("/$listId").accept(contentTypeJson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(contentTypeJson))
            .andExpect(jsonPath("$.bits").value(1))
            .andExpect(jsonPath("$.lst").isNotEmpty())
            .andExpect(jsonPath("$.aggregation_uri").value(endsWith("/aggregation/test-pool")))
            .andExpect(header().exists(HttpHeaders.LAST_MODIFIED))
            .andExpect(header().string(HttpHeaders.ETAG, "\"${lastModified.toEpochMilli()}\""))
            .andExpect(header().string(HttpHeaders.VARY, "Accept"))
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"))
        //  .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should not return status list if not modified`() {
        val lastModified = Instant.now()
        every { appConfiguration.publicUrl } returns appUrl
        every { statusListTokenSource.load(listId, TokenFormat.JSON) } returns
            TokenData(statusListJson.toByteArray(), lastModified)

        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/$listId")
                    .accept(contentTypeJson)
                    .header(
                        HttpHeaders.IF_MODIFIED_SINCE,
                        lastModified.plusSeconds(10).toEpochMilli(),
                    )
            )
            .andExpect(status().isNotModified)
            .andExpect(content().contentType(contentTypeJson))
            .andExpect(content().string(IsEmptyString.emptyOrNullString()))
            .andExpect(header().exists(HttpHeaders.LAST_MODIFIED))
            .andExpect(header().string(HttpHeaders.ETAG, """"${lastModified.toEpochMilli()}""""))
            .andExpect(header().string(HttpHeaders.VARY, "Accept"))
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"))
        //  .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should return status list as jwt`() {
        val lastModified = Instant.now()
        every { appConfiguration.publicUrl } returns appUrl
        every { statusListTokenSource.load(listId, TokenFormat.JWT) } returns
            TokenData(statusListJwt.toByteArray(), lastModified)

        mockMvc
            .perform(MockMvcRequestBuilders.get("/$listId").accept(contentTypeJwt))
            .andExpect(status().isOk)
            .andExpect(content().contentType(contentTypeJwt))
            .andExpect(content().string(statusListJwt))
            .andExpect(header().exists(HttpHeaders.LAST_MODIFIED))
            .andExpect(header().string(HttpHeaders.ETAG, """"${lastModified.toEpochMilli()}""""))
            .andExpect(header().string(HttpHeaders.VARY, "Accept"))
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"))
        //  .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should return aggregation`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/aggregation/test-pool")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isNotFound)

        every { statusListService.listsForAggregationId("test-pool") } returns
            listOf(
                "$appUrl/da3ed290-72f3-4c1c-9d11-9b1fa75902a0",
                "$appUrl/b7f41cf3-a348-4f96-b430-bc7bbbbc0331",
            )

        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/aggregation/test-pool")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status_lists[0]").value(startsWith(appUrl)))
            .andExpect(jsonPath("$.status_lists[1]").value(startsWith(appUrl)))
    }

    @Test
    fun `should get error on id that is not an UUID`() {
        mockMvc
            .perform(MockMvcRequestBuilders.get("/not-a-uuid").accept(contentTypeJson))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should get error on unknown id`() {
        val unknownId = UUID.randomUUID()
        every { appConfiguration.publicUrl } returns appUrl
        every { statusListTokenSource.load(unknownId, TokenFormat.JSON) } returns null
        mockMvc
            .perform(MockMvcRequestBuilders.get("/$unknownId").accept(contentTypeJson))
            .andExpect(status().isNotFound)
    }
}
