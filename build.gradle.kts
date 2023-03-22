import org.gradle.jvm.toolchain.JavaLanguageVersion.of

plugins {
	java
	id("io.papermc.paperweight.userdev") version "1.5.3"
}

repositories {
	mavenCentral()

	maven("https://papermc.io/repo/repository/maven-public/") // Paper
}

dependencies {
	paperDevBundle("1.19.4-R0.1-SNAPSHOT")
}

java.toolchain.languageVersion.set(of(17))