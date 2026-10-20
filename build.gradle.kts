import org.gradle.language.jvm.tasks.ProcessResources
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.mapstruct:mapstruct:${property("mapstruct.version")}")
    implementation("org.hibernate.validator:hibernate-validator-annotation-processor:${property("hibernate.validator.version")}")
    compileOnly(project(":BusinessLogic:Operation"))
    testImplementation("com.microsoft.playwright:playwright:${property("playwright.version")}")
    testImplementation("org.springframework.security:spring-security-test")
    annotationProcessor("org.mapstruct:mapstruct-processor:${property("mapstruct.version")}")
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
