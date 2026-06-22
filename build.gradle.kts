plugins {
    `java-library`
}

group = "com.baymc.tipspro"

val baseVersion = "1.0.0-SNAPSHOT"
val gitCommitShort = providers.gradleProperty("gitCommitShort").orElse("unknown")
val artifactVersion = providers.gradleProperty("artifactVersionOverride").orElse(baseVersion).get()

version = artifactVersion

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.60-stable")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("io.papermc.paper:paper-api:26.1.2.build.60-stable")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.withType<Jar>().configureEach {
    archiveBaseName.set("BayMcTipsPro")
    manifest {
        attributes(
            "Implementation-Title" to "BayMcTipsPro",
            "Implementation-Version" to artifactVersion,
            "Git-Commit-Short" to gitCommitShort.get(),
        )
    }
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to artifactVersion)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("printVersion") {
    group = "versioning"
    description = "输出开发版发布标签使用的基础项目版本"
    doLast {
        println(baseVersion)
    }
}
