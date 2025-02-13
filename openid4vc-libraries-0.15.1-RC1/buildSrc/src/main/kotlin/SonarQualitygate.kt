import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.io.FileReader
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class SonarQualitygate : DefaultTask() {

    @TaskAction
    fun checkQualitygate() {
        val sonarToken =
            project.findProperty("SONAR_TOKEN")?.toString()
                ?: System.getenv("SONAR_TOKEN")
                ?: error("SONAR_TOKEN not set")

        val reportTaskFile = project.layout.buildDirectory.file("sonar/report-task.txt").get()

        val sonarProject = ScannedProjectInfo.fromFile(reportTaskFile.asFile)

        check(sonarProject.qualityGatePassed(sonarToken)) { "Sonar quality gate failed" }
    }

    class ScannedProjectInfo(
        val ceTaskUrl: String,
        val serverUrl: String,
    ) {

        fun qualityGatePassed(sonarToken: String): Boolean {
            val analysisId = waitForAnalysisId(sonarToken)
            return fetchQualityGateStatus(analysisId, sonarToken) == "OK"
        }

        private fun waitForAnalysisId(sonarToken: String): String {
            for (i in 0..10) {
                val analysisId = fetchAnalysisId(sonarToken)
                if (analysisId == null) {
                    Thread.sleep(3000)
                } else {
                    return analysisId
                }
            }
            error("Failed to fetch analysis id after waiting 30s")
        }

        private fun fetchAnalysisId(sonarToken: String): String? {
            val body = readUrl(ceTaskUrl, sonarToken)
            val json = ObjectMapper().readTree(body)
            val task = json["task"] ?: error("Not task object found")
            return if (task["status"]?.asText() == "SUCCESS") {
                task["analysisId"]?.asText()
            } else {
                null
            }
        }

        private fun fetchQualityGateStatus(analysisId: String, sonarToken: String): String {
            val body =
                readUrl(
                    "$serverUrl/api/qualitygates/project_status?analysisId=$analysisId",
                    sonarToken
                )
            val json = ObjectMapper().readTree(body)
            val projectStatus = json["projectStatus"] ?: error("Missing projectStatus")
            return projectStatus["status"].asText() ?: error("No project status found")
        }

        private fun readUrl(url: String, sonarToken: String): String {
            val connection = URI(url).toURL().openConnection()
            connection.connectTimeout = 1000
            connection.readTimeout = 1000
            connection.setRequestProperty("Authorization", "Bearer $sonarToken")
            return connection.getInputStream().reader(UTF_8).readText()
        }

        companion object {
            fun fromFile(file: File): ScannedProjectInfo {
                val properties = Properties().apply { load(FileReader(file)) }
                return ScannedProjectInfo(
                    ceTaskUrl =
                        properties["ceTaskUrl"]?.toString()
                            ?: error("No ceTaskUrl in report-task.txt"),
                    serverUrl =
                        properties["serverUrl"]?.toString()
                            ?: error("No serverUrl in report-task.txt")
                )
            }
        }
    }
}
