import org.gradle.jvm.toolchain.JavaLanguageVersion.of

plugins {
	java
	id("io.papermc.paperweight.userdev") version "1.3.7"
}

repositories {
	maven("https://papermc.io/repo/repository/maven-public/") // Paper

	mavenCentral()
}

dependencies {
	paperDevBundle("1.19-R0.1-SNAPSHOT")
}

java.toolchain.languageVersion.set(of(17))