package com.github.saiprasadkrishnamurthy.rin.config

import org.jline.utils.AttributedString
import org.jline.utils.AttributedStyle
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.shell.jline.PromptProvider
import javax.sql.DataSource


@Configuration
open class AppConfig(private val env: Environment) {

    @Bean
    open fun promptProvider(): PromptProvider {
        return PromptProvider {
            AttributedString("release-info$:> ", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
        }
    }

    @Bean
    open fun jdbcTemplate(dataSource: DataSource): JdbcTemplate {
        val jdbcTemplate = JdbcTemplate(dataSource)
        createTables(jdbcTemplate)
        return jdbcTemplate
    }

    fun createTables(jdbcTemplate: JdbcTemplate) {
        val sqls = listOf("CREATE TABLE IF NOT EXISTS VERSION_INFO (gitSha string, artifactId, mavenVersion string, timestamp long, author string, commitMessage string, tickets text, entries text, day string)",
                "CREATE INDEX IF NOT EXISTS artifactId ON VERSION_INFO (artifactId)",
                "CREATE INDEX IF NOT EXISTS mavenVersion ON VERSION_INFO (mavenVersion)",
                "CREATE INDEX IF NOT EXISTS author ON VERSION_INFO (author)",
                "CREATE INDEX IF NOT EXISTS gitSha ON VERSION_INFO (gitSha)",
                "CREATE TABLE IF NOT EXISTS DEPENDENCIES_INFO (parentArtifactId string, parentVersion string, dependencyArtifactId string, dependencyVersion string, name string, description string, url string)",
                "CREATE INDEX IF NOT EXISTS parentArtifactId ON DEPENDENCIES_INFO (parentArtifactId)",
                "CREATE INDEX IF NOT EXISTS parentVersion ON DEPENDENCIES_INFO (parentVersion)"
        )
        sqls.forEach { jdbcTemplate.update(it) }
    }

    fun dropTables(jdbcTemplate: JdbcTemplate) {
        val sqls = listOf("DROP table IF EXISTS VERSION_INFO",
                "DROP table IF EXISTS DEPENDENCIES_INFO",
                "DROP INDEX IF EXISTS artifactId",
                "DROP INDEX IF EXISTS mavenVersion ",
                "DROP INDEX IF EXISTS author",
                "DROP INDEX IF EXISTS gitSha ",
                "DROP INDEX IF EXISTS parentArtifactId ",
                "DROP INDEX IF EXISTS parentVersion ")
        sqls.forEach { println(jdbcTemplate.update(it)) }
    }

    @Bean
    open fun dataSource(): DataSource {
        val dataSource = DriverManagerDataSource()
        dataSource.setDriverClassName(env.getProperty("driverClassName"))
        dataSource.url = env.getProperty("url")
        dataSource.username = env.getProperty("user")
        dataSource.password = env.getProperty("password")
        return dataSource
    }
}