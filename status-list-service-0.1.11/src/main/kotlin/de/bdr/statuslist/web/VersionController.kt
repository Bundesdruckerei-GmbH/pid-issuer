/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.web

import de.bdr.statuslist.ProjectVersion
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class VersionController {
    @GetMapping("/version", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun version() = ProjectVersion.value
}
