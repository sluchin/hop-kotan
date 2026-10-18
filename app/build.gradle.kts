import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

// Kotlin の警告が出たら, ビルドを失敗させる (core と同じ)
kotlin {
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

// 忽布古丹醸造のマーク (private のサブモジュール). あるときだけ, それを使う APK (marked) を作れる.
// マークの権利は, 忽布古丹醸造にある. リポジトリにコミットせず, その APK を配布しない (.claude/CLAUDE.md)
val markFile = rootProject.file("private/kotan-mark.svg")

/** SVG のマーク (path が 1 つ) から, アダプティブアイコンの絵 (Android のベクター) を作る. */
abstract class GenerateMarkIcon : DefaultTask() {
    /** マークの SVG. */
    @get:InputFile
    abstract val svg: RegularFileProperty

    /** 絵を書き出す, res ディレクトリ. */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    /** 前景 (マーク) と, 背景を書く. */
    @TaskAction
    fun generate() {
        val text = svg.get().asFile.readText()
        val box =
            Regex(
                """viewBox="([\d.\s-]+)"""",
            ).find(text)!!.groupValues[1].trim().split(Regex("\\s+")).map {
                it.toFloat()
            }
        val path = Regex("""\sd="([^"]+)"""").find(text)!!.groupValues[1]
        // 108 の画面の中央に, 幅 56 (安全領域 66 の内側) で置く
        val scale = MARK_WIDTH / box[2]
        val tx = (CANVAS - box[2] * scale) / 2 - box[0] * scale
        val ty = (CANVAS - box[3] * scale) / 2 - box[1] * scale
        val dir =
            outputDir
                .get()
                .asFile
                .resolve("drawable")
                .apply { mkdirs() }
        dir.resolve("ic_launcher_foreground.xml").writeText(
            """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group
        android:scaleX="$scale"
        android:scaleY="$scale"
        android:translateX="$tx"
        android:translateY="$ty">
        <path
            android:fillColor="#FF231815"
            android:pathData="$path" />
    </group>
</vector>
""",
        )
        dir.resolve("ic_launcher_background.xml").writeText(
            """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFE6DAC3"
        android:pathData="M0,0h108v108h-108z" />
</vector>
""",
        )
    }

    private companion object {
        const val CANVAS = 108f
        const val MARK_WIDTH = 56f
    }
}

android {
    namespace = "jp.hopkotan.app"
    // build-tools は, Docker のイメージに入っている版に固定する (SDK は読み取り専用)
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "jp.hopkotan.app"
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        // データ (JSON) を取りにいく URL (GitHub Actions が, 毎日更新する)
        buildConfigField(
            "String",
            "DATA_URL",
            "\"https://raw.githubusercontent.com/sluchin/hop-kotan/main/data/\"",
        )
    }

    buildFeatures {
        buildConfig = true
    }

    // plain: オリジナルのアイコン. marked: 忽布古丹醸造のマークのアイコン (private/ があるときだけ)
    flavorDimensions += "icon"
    productFlavors {
        create("plain") { dimension = "icon" }
        if (markFile.exists()) {
            create("marked") { dimension = "icon" }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // 警告も, エラーにする
    lint {
        warningsAsErrors = true
        abortOnError = true
    }
}

androidComponents {
    if (markFile.exists()) {
        onVariants(selector().withFlavor("icon" to "marked")) { variant ->
            val task =
                tasks.register<GenerateMarkIcon>(
                    "generate${variant.name.replaceFirstChar { it.uppercase() }}Icon",
                ) {
                    svg.set(markFile)
                }
            variant.sources.res?.addGeneratedSourceDirectory(task, GenerateMarkIcon::outputDir)
        }
    }
}

// APK の名前を, リポジトリの名前と版にする (例: hop-kotan-0.1.0-debug.apk. 後ろに, ビルドの種類が付く)
base {
    archivesName = "hop-kotan-${android.defaultConfig.versionName}"
}

dependencies {
    implementation(project(":core"))
    implementation(libs.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.swiperefresh)
    implementation(libs.androidx.work)
    implementation(libs.play.code.scanner)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}
