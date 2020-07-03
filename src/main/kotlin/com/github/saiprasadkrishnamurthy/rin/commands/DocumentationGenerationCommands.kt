package com.github.saiprasadkrishnamurthy.rin.commands

import com.github.saiprasadkrishnamurthy.rin.config.AppConfig
import com.jakewharton.fliptables.FlipTableConverters
import net.steppschuh.markdowngenerator.table.Table
import net.steppschuh.markdowngenerator.text.emphasis.BoldText
import net.steppschuh.markdowngenerator.text.heading.Heading
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import javax.sql.DataSource


/**
 * @author Sai.
 */
@ShellComponent("Documentation Generation Commands")
class DocumentationGenerationCommands(val appConfig: AppConfig, val jdbcTemplate: JdbcTemplate, val dataSource: DataSource) {

    @ShellMethod("md")
    fun md(): String {
        val artifactsAndCurrentVersionsQuery = "SELECT parentartifactid, max(parentversion) as current_version, name, description, url FROM DEPENDENCIES_INFO group by parentartifactid order by parentartifactid"
        val rows = jdbcTemplate.queryForList(artifactsAndCurrentVersionsQuery)
        val out = StringBuilder()
        out.append(Heading("Artifacts Version Metadata", 1)).append("\n\n")
        rows.forEach { row ->
            val artifactId = row["parentartifactid"].toString()
            val version = row["current_version"].toString()
            val name = row["name"].toString()
            val description = row["description"].toString()
            val url = row["url"].toString()
            out.append(Heading("Artifact Id: $artifactId, Version: $version, Artifact Name: $name", 2)).append("\n")
            out.append(BoldText(description)).append("\n")
            out.append(BoldText(url)).append("\n")

            out.append(Heading("Dependencies", 3)).append("\n")
            var tableBuilder = Table.Builder()
                    .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_LEFT)
                    .addRow("Artifact ID", "Maven Version")

            val depsQuery = "SELECT * FROM DEPENDENCIES_INFO where parentartifactid='$artifactId' and parentversion='$version'"
            val deps = jdbcTemplate.queryForList(depsQuery)
            deps.forEach { d ->
                tableBuilder.addRow(d["dependencyArtifactId"], d["dependencyVersion"])
            }
            out.append(tableBuilder.build()).append("\n")

            out.append(Heading("Git Logs", 3)).append("\n")

            tableBuilder = Table.Builder()
                    .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_LEFT)
                    .addRow("GIT Revision", "Artifact Id", "Maven Version", "Commit Message", "Author", "TIMESTAMP")

            loadGitLogs(artifactId, version, tableBuilder)
            val tickets = getTickets(artifactId, version).toMutableList()
            deps.forEach {
                loadGitLogs(it["dependencyArtifactId"].toString(), it["dependencyVersion"].toString(), tableBuilder)
                tickets.addAll(getTickets(it["dependencyArtifactId"].toString(), it["dependencyVersion"].toString()))
            }
            out.append(tableBuilder.build()).append("\n")

            out.append(Heading("Tickets", 3)).append("\n")
            tableBuilder = Table.Builder()
                    .withAlignments(Table.ALIGN_LEFT)
                    .addRow("Tickets")
            tickets.distinct().forEach { tableBuilder.addRow(it) }
            out.append(tableBuilder.build()).append("\n")
            out.append("\n\n")
        }
        val file = Paths.get("Documentation.md")
        Files.writeString(file, out.toString(), Charset.defaultCharset())
        return file.toFile().absolutePath + " generated successfully."
    }

    private fun loadGitLogs(artifactId: String, version: String, tableBuilder: Table.Builder) {
        val gitLogsQuery = "SELECT gitSha as git_revision ,artifactid, mavenVersion as maven_version, commitMessage as commit_message, author, datetime(timestamp, 'unixepoch') as timestamp, tickets FROM VERSION_INFO where artifactid='$artifactId' and mavenVersion='$version'"
        val logs = jdbcTemplate.queryForList(gitLogsQuery)
        logs.forEach { d ->
            tableBuilder.addRow(d["git_revision"], d["artifactid"], d["maven_version"], d["commit_message"], d["author"], d["timestamp"])
        }
    }

    private fun getTickets(artifactId: String, version: String): List<String> {
        val gitLogsQuery = "SELECT tickets FROM VERSION_INFO where artifactid='$artifactId' and mavenVersion='$version'"
        val logs = jdbcTemplate.queryForList(gitLogsQuery)
        return logs.flatMap { d ->
            d["tickets"].toString().split(",").distinct()
        }.toList()
    }

    private fun execQuery(rows: MutableList<MutableMap<String, Any>>): String {
        return return if (rows.isNotEmpty()) {
            val headers = rows[0].keys.toTypedArray()
            val data = rows.map { it.values.toTypedArray() }.toTypedArray()
            FlipTableConverters.fromObjects(headers, data)
        } else {
            "NO ROWS FOUND"
        }
    }
}