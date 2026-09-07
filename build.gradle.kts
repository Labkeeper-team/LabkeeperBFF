import org.gradle.language.jvm.tasks.ProcessResources

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.mapstruct:mapstruct:${property("mapstruct.version")}")
    implementation("org.hibernate.validator:hibernate-validator-annotation-processor:${property("hibernate.validator.version")}")
    implementation(project(":BusinessLogic:Operation"))
    testImplementation("com.microsoft.playwright:playwright:${property("playwright.version")}")
    testImplementation("org.springframework.security:spring-security-test")
    annotationProcessor("org.mapstruct:mapstruct-processor:${property("mapstruct.version")}")
}

val editorFrontendDir = layout.projectDirectory.dir("src/main/editor")
val pagesFrontendDir = layout.projectDirectory.dir("src/main/pages")
val wikiFrontendDir = layout.projectDirectory.dir("src/main/wiki")
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

tasks.named<ProcessResources>("processResources") {
    dependsOn("npmBuild", "wikiNpmBuild")
    from(editorFrontendDir.dir("dist")) { into("editor") }
    from(pagesFrontendDir.dir("src")) { into("pages") }
    from(wikiFrontendDir.dir("_site")) { into("wiki") }
}

tasks.named("clean") {
    doFirst {
        delete(editorFrontendDir.dir("dist"))
        delete(wikiFrontendDir.dir("_site"))
    }
}
