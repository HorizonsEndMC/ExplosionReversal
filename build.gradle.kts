import org.gradle.jvm.toolchain.JavaLanguageVersion.of

plugins {
	java
	id("io.papermc.paperweight.userdev") version "1.3.8"
}

repositories {
	mavenCentral()

	maven("https://papermc.io/repo/repository/maven-public/") // Paper
}

dependencies {
	paperDevBundle("1.19.1-R0.1-SNAPSHOT")
}

java.toolchain.languageVersion.set(of(17))