plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    application
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(project(":core"))
}

application {
    mainClass.set("jp.hopkotan.scraper.MainKt")
}

// 相対パスの引数 (data など) は, リポジトリの直下からの位置にする
tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}
