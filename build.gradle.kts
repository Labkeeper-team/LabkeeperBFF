import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.springframework.boot")
}

// Versions and shared libraries come from the server build. A checkout of this folder brings its own.
if (parent == null) {
    group = property("group").toString()

    repositories {
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        compilerOptions.javaParameters.set(true)
        compilerOptions.freeCompilerArgs.add("-Xjsr305=warn")
        compilerOptions.freeCompilerArgs.add("-Xjspecify-annotations=ignore")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    dependencies {
        val bootBom = "org.springframework.boot:spring-boot-dependencies:3.5.10"
        implementation(platform(bootBom))
        testImplementation(platform(bootBom))
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
        testImplementation("io.kotest:kotest-assertions-core-jvm:6.1.10")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    testImplementation("org.springframework.security:spring-security-test")
    // bootRun uses the test classpath. The published jar does not pull the gateway in.
    testImplementation(platform("org.springframework.cloud:spring-cloud-dependencies:2025.0.3"))
    testImplementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
}

val editorFrontendDir = layout.projectDirectory.dir("src/main/editor")
val wikiFrontendDir = layout.projectDirectory.dir("src/main/wiki")
val libsDir = layout.projectDirectory.dir("src/main/libs")
val npmCommand = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
    "npm.cmd"
} else {
    "npm"
}

tasks.register<Exec>("npmInstall") {
    workingDir = editorFrontendDir.asFile
    commandLine(npmCommand, "install")
}

tasks.register<Exec>("npmBuild") {
    workingDir = editorFrontendDir.asFile
    commandLine(npmCommand, "run", "build", "--", "--build-version=${rootProject.version}")
    dependsOn("npmInstall")
}

tasks.register<Exec>("wikiNpmInstall") {
    workingDir = wikiFrontendDir.asFile
    commandLine(npmCommand, "ci")
}

tasks.register<Exec>("wikiNpmBuild") {
    workingDir = wikiFrontendDir.asFile
    commandLine(npmCommand, "run", "build")
    dependsOn("wikiNpmInstall")
}

tasks.register<Exec>("libsNpmInstall") {
    workingDir = libsDir.asFile
    commandLine(npmCommand, "ci")
    inputs.file(libsDir.file("package.json"))
    inputs.file(libsDir.file("package-lock.json"))
    outputs.dir(libsDir.dir("node_modules"))
}

tasks.register<Exec>("libsNpmBuild") {
    workingDir = libsDir.asFile
    commandLine(npmCommand, "run", "build")
    dependsOn("libsNpmInstall")
}

val frontendBootRunOnly = gradle.startParameter.taskNames
    .filter { !it.startsWith("-") }
    .map { it.substringAfterLast(':') }
    .let { names -> names.isNotEmpty() && names.all { it == "bootRun" } }

tasks.named<ProcessResources>("processResources") {
    dependsOn("libsNpmBuild")
    // The editor and the wiki take long to build, so bootRun alone serves whatever was built before.
    if (!frontendBootRunOnly) {
        dependsOn("npmBuild", "wikiNpmBuild")
    }
    from(layout.buildDirectory.dir("generated/libs")) { into("static/assets") }
    from(editorFrontendDir.dir("dist")) { into("editor") }
    from(wikiFrontendDir.dir("_site")) { into("wiki") }
}

tasks.named<Jar>("jar") {
    enabled = true
    archiveClassifier.set("")
}

tasks.named<BootJar>("bootJar") {
    enabled = false
}

tasks.named<BootRun>("bootRun") {
    mainClass.set("io.labkeeper.web.frontend.local.LocalFrontendApplicationKt")
    // Sources come first, so edited templates and static files show up without a restart.
    classpath = files("src/main/resources") + sourceSets["test"].runtimeClasspath
}

tasks.named("clean") {
    doFirst {
        delete(editorFrontendDir.dir("dist"))
        delete(wikiFrontendDir.dir("_site"))
    }
}
