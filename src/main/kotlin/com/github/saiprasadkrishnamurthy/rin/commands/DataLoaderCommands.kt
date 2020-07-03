package com.github.saiprasadkrishnamurthy.rin.commands

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.saiprasadkrishnamurthy.rin.config.AppConfig
import com.github.saiprasadkrishnamurthy.rin.model.DependenciesInfo
import com.github.saiprasadkrishnamurthy.rin.model.VersionMetadata
import org.apache.commons.io.IOUtils
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import java.io.File
import java.nio.charset.Charset
import java.sql.PreparedStatement
import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * @author Sai.
 */
@ShellComponent("Database Loader Commands")
class DataLoaderCommands(val appConfig: AppConfig, val jdbcTemplate: JdbcTemplate) {

    @ShellMethod("Loads all the artifact versions into the database")
    fun load(@ShellOption(value = ["-d", "--dir"], help = "The base directory from which the 'jars' should be scanned recursively") baseDir: String): String {
        File(baseDir).walk()
                .filter { it.extension == "jar" || it.extension == "war" }
                .forEach {
                    val jarFile = JarFile(it)
                    val entries = jarFile.entries()
                    while (entries.hasMoreElements()) {
                        val element = entries.nextElement()
                        extractVersionInfo(element, jarFile)
                        extractDependencies(element, jarFile)
                    }
                }
        return baseDir
    }

    private fun extractVersionInfo(element: JarEntry, jarFile: JarFile) {
        if (element.name.endsWith("versionInfo.json")) {
            val istream = jarFile.getInputStream(element)
            val contents = IOUtils.toString(istream, Charset.defaultCharset())
            val vm = jacksonObjectMapper().readValue(contents.toByteArray(Charset.defaultCharset()), object : TypeReference<List<VersionMetadata>>() {})
            loadVersionInfo(vm)
        }
    }

    private fun extractDependencies(element: JarEntry, jarFile: JarFile) {
        if (element.name.endsWith("dependencies.json")) {
            val istream = jarFile.getInputStream(element)
            val contents = IOUtils.toString(istream, Charset.defaultCharset())
            val di = jacksonObjectMapper().readValue(contents.toByteArray(Charset.defaultCharset()), object : TypeReference<List<DependenciesInfo>>() {})
            loadDependencies(di)
        }
    }

    private fun loadDependencies(di: List<DependenciesInfo>?) {
        if (di != null) {
            val insert = "INSERT INTO DEPENDENCIES_INFO(parentArtifactId, parentVersion, dependencyArtifactId, dependencyVersion, name, description, url) VALUES(?,?,?,?,?,?,?)"

            val bps = object : BatchPreparedStatementSetter {
                override fun setValues(ps: PreparedStatement, i: Int) {
                    ps.setString(1, di[i].parentArtifactId)
                    ps.setString(2, di[i].parentVersion)
                    ps.setString(3, di[i].dependencyArtifactId)
                    ps.setString(4, di[i].dependencyVersion)
                    ps.setString(5, di[i].name)
                    ps.setString(6, di[i].description)
                    ps.setString(7, di[i].url)
                }

                override fun getBatchSize(): Int {
                    return di.size
                }
            }
            jdbcTemplate.batchUpdate(insert, bps)
        }
    }

    private fun loadVersionInfo(versionMetadata: List<VersionMetadata>) {
        val insert = "INSERT INTO VERSION_INFO(gitSha, artifactId, mavenVersion, timestamp, author, commitMessage, tickets, entries, day) VALUES(?,?,?,?,?,?,?,?,?)"

        val bps = object : BatchPreparedStatementSetter {
            override fun setValues(ps: PreparedStatement, i: Int) {
                ps.setString(1, versionMetadata[i].gitSha)
                ps.setString(2, versionMetadata[i].artifactId)
                ps.setString(3, versionMetadata[i].mavenVersion)
                ps.setLong(4, versionMetadata[i].timestamp)
                ps.setString(5, versionMetadata[i].author)
                ps.setString(6, versionMetadata[i].commitMessage)
                ps.setString(7, versionMetadata[i].tickets.joinToString("\n"))
                ps.setString(8, versionMetadata[i].entries.joinToString("\n"))
                ps.setString(9, versionMetadata[i].day)

            }

            override fun getBatchSize(): Int {
                return versionMetadata.size
            }
        }
        jdbcTemplate.batchUpdate(insert, bps)
    }

    @ShellMethod("Clear all the artifact versions from the database")
    fun cleardb(): String {
        appConfig.dropTables(jdbcTemplate)
        appConfig.createTables(jdbcTemplate)
        return "OK"
    }
}