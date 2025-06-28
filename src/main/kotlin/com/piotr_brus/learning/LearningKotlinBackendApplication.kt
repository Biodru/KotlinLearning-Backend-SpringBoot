package com.piotr_brus.learning

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LearningKotlinBackendApplication

fun main(args: Array<String>) {
	print(args)
	runApplication<LearningKotlinBackendApplication>(*args)
}
