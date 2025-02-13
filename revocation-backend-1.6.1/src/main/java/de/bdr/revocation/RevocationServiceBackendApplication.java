/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RevocationServiceBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RevocationServiceBackendApplication.class, args);
    }

}
