package com.github.saiprasadkrishnamurthy.rin.commands

import com.github.saiprasadkrishnamurthy.rin.config.AppConfig
import com.jakewharton.fliptables.FlipTableConverters
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.JdbcUtils
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import org.tomlj.Toml
import org.tomlj.TomlTable
import java.nio.file.Paths
import java.util.regex.Pattern
import javax.sql.DataSource


/**
 * @author Sai.
 */
@ShellComponent("Database Query Commands")
class QueryCommands(val appConfig: AppConfig, val jdbcTemplate: JdbcTemplate, val dataSource: DataSource) {

    @ShellMethod("sql")
    fun sql(@ShellOption(value = ["-q", "--query"], help = "Runs SQL Query") query: String): String {
        val rows = jdbcTemplate.queryForList(query)
        return execQuery(rows)
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

    @ShellMethod("tables")
    fun tables(): Any {
        return JdbcUtils.extractDatabaseMetaData(dataSource) {
            val rs = it.getTables(it.userName, null, null, arrayOf("TABLE"))
            val l = mutableListOf<String>()
            while (rs.next()) {
                l.add(rs.getString(3))
            }
            l
        }
    }

    @ShellMethod("named_queries")
    fun named_queries(): Any {
        val path = Paths.get("queries.toml")
        if (path.toFile().exists()) {
            val toml = Toml.parse(path);
            val l = mutableListOf<MutableMap<String, String>>()
            toml.toMap().map {
                if (it.value is TomlTable) {
                    val tt = it.value as TomlTable
                    val map = mutableMapOf("name" to it.key)
                    map["description"] = tt["description"].toString()
                    l.add(map)
                }
            }
            val headers = l[0].keys.toTypedArray()
            val data = l.map { it.values.toTypedArray() }.toTypedArray()
            return FlipTableConverters.fromObjects(headers, data)
        } else {
            return " NO FILE WITH THE NAME 'queries.toml' EXISTS IN THIS DIRECTORY"
        }
    }

    @ShellMethod("nq")
    fun nq(@ShellOption(value = ["-n", "--name"], help = "Runs a named Query") name: String): Any {
        val path = Paths.get("queries.toml")
        if (path.toFile().exists()) {
            val toml = Toml.parse(path);
            val l = mutableListOf<MutableMap<String, String>>()
            toml.toMap().map {
                if (it.value is TomlTable) {
                    val tt = it.value as TomlTable
                    val map = mutableMapOf("name" to it.key)
                    map["sql"] = tt["sql"].toString()
                    l.add(map)
                }
            }
            val q = l.filter { it.containsValue(name) }.map { it["sql"] }.firstOrNull()
            return return if (q == null) {
                "NO NAMED QUERY $name EXISTS"
            } else {
                val pattern = Pattern.compile(":\\w+")
                val vars = mutableListOf<String>()
                val matchPattern = pattern.matcher(q)
                while (matchPattern.find()) {
                    vars.add(matchPattern.group(0))
                }
                if (vars.isEmpty()) {
                    execQuery(jdbcTemplate.queryForList(q))
                } else {
                    var out = q!!
                    vars.forEach {
                        print(" Enter Value for $it: ")
                        val v = readLine()
                        out = out.replace(it, "'$v'")
                    }
                    execQuery(jdbcTemplate.queryForList(out))
                }
            }
        } else {
            return " NO FILE WITH THE NAME 'queries.toml' EXISTS IN THIS DIRECTORY "
        }
    }
}