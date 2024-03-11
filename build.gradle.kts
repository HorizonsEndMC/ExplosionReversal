import org.gradle.jvm.toolchain.JavaLanguageVersion.of

plugins {
	java
	id("io.papermc.paperweight.userdev") version "1.5.11"
}

repositories {
	mavenCentral()

	maven("https://papermc.io/repo/repository/maven-public/") // Paper
}

dependencies {
	paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")
}

java.toolchain.languageVersion.set(of(17))