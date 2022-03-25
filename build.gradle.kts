import org.gradle.jvm.toolchain.JavaLanguageVersion.of

plugins {
	java
	id("io.papermc.paperweight.userdev") version "1.3.5"
}

repositories {
	maven("https://papermc.io/repo/repository/maven-public/") // Paper

	mavenCentral()
}

dependencies {
	paperDevBundle("1.18.2-R0.1-SNAPSHOT")
}

java.toolchain.languageVersion.set(of(17))