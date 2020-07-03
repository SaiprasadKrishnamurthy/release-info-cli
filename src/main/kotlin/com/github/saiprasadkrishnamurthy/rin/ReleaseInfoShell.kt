package com.github.saiprasadkrishnamurthy.rin

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication


@SpringBootApplication
open class ReleaseInfoShell

fun main(args: Array<String>) {
    SpringApplication.run(ReleaseInfoShell::class.java, *args)
}

