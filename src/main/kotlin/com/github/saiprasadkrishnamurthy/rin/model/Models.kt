package com.github.saiprasadkrishnamurthy.rin.model

data class VersionMetadata(val gitSha: String,
                           val mavenVersion: String,
                           val timestamp: Long,
                           val author: String,
                           val commitMessage: String,
                           val tickets: List<String>,
                           val entries: List<String>,
                           val day: String,
                           val artifactId: String)

data class DependenciesInfo(val parentArtifactId: String,
                            val parentVersion: String,
                            val dependencyArtifactId: String,
                            val dependencyVersion: String,
                            val name: String,
                            val description: String,
                            val url: String)