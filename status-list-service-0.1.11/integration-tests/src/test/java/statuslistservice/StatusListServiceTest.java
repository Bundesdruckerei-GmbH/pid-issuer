/*
 * Copyright 2024 Bundesdruckerei GmbH
 */
package statuslistservice;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatusListServiceTest {
    @AfterAll
    public static void generateReport() {
        Collection<File> jsonFiles = FileUtils.listFiles(new File("target"),
                WildcardFileFilter.builder().setWildcards("*.json").get(), WildcardFileFilter.builder().setWildcards("karate-reports*").get());
        List<String> jsonPaths = new ArrayList<>(jsonFiles.size());
        jsonFiles.forEach(file -> jsonPaths.add(file.getAbsolutePath()));
        Configuration config = new Configuration(new File("target"), "StatusListService");
        ReportBuilder reportBuilder = new ReportBuilder(jsonPaths, config);
        reportBuilder.generateReports();
    }

    @Test
    void testParallel() {
        Results results = Runner.path("classpath:statuslistservice")
                .outputCucumberJson(true)
                .outputJunitXml(true)
                .parallel(5);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }

}
