/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.jmolecules.archunit.JMoleculesArchitectureRules;
import org.jmolecules.archunit.JMoleculesDddRules;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

@AnalyzeClasses(packages = "de.bdr.pidi")
class ArchitectureTests {
    @ArchTest
    ArchRule dddRules = JMoleculesDddRules.all();
    @ArchTest
    ArchRule hexagonal = JMoleculesArchitectureRules.ensureHexagonal();

    @Test
    void verifyModules() {
        ApplicationModules.of(PidiApplication.class).verify();
    }

    @Test
    void writeDocumentationSnippets() {

        var modules = ApplicationModules.of(PidiApplication.class);

        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
