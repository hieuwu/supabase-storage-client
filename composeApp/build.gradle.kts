import com.codingfeline.buildkonfig.compiler.FieldSpec
import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            linkerOpts("-lsqlite3")
        }

    }


    jvm()

    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.androidx.security.crypto)
            implementation(libs.ktor.client.android)   // or latest stable 3.x version
            implementation(libs.sqldelight.android)
            implementation(libs.androidx.core.splashscreen)

        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Supabase
            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.storage)
            implementation(libs.supabase.auth)
            implementation(libs.ktor.client.core)

            // Multiplatform Settings
            implementation(libs.multiplatform.settings)

            // Kotlinx
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.kermit)
            implementation(libs.sqldelight.coroutines)


            implementation(libs.revenuecat.purchases)
            implementation(libs.revenuecat.purchases.ui)
            
            // FileKit
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test) // Use the latest version
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.sqldelight.sqlite)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native)
        }
    }
}

android {
    namespace = "com.hieuwu.supabasestorageclient"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    signingConfigs {
        create("release") {
            storeFile = System.getenv("ANDROID_KEYSTORE_FILE")?.let { file(it) }
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_KEY_ALIAS")
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
        }
    }

    defaultConfig {
        applicationId = "com.hieuwu.supabasestorageclient"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 7
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.hieuwu.supabasestorageclient.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.hieuwu.supabasestorageclient"
            packageVersion = "1.0.0"
        }
    }
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("com.hieuwu.supabasestorageclient.database")
        }
    }
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val revenueCatApiKeyAndroid: String = localProperties.getProperty("revenuecat.api.key.android") ?: ""
val revenueCatApiKeyIos: String = localProperties.getProperty("revenuecat.api.key.ios") ?: ""

buildkonfig {
    packageName = "com.hieuwu.supabasestorageclient"
    objectName = "BuildKonfig"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "REVENUECAT_API_KEY", "")
    }

    targetConfigs {
        create("android") {
            buildConfigField(FieldSpec.Type.STRING, "REVENUECAT_API_KEY", revenueCatApiKeyAndroid)
        }
        create("iosArm64") {
            buildConfigField(FieldSpec.Type.STRING, "REVENUECAT_API_KEY", revenueCatApiKeyIos)
        }
        create("iosSimulatorArm64") {
            buildConfigField(FieldSpec.Type.STRING, "REVENUECAT_API_KEY", revenueCatApiKeyIos)
        }
    }
}
afterEvaluate {
    configurations.matching {
        it.name.contains("jvm", ignoreCase = true) ||
                it.name.contains("macos", ignoreCase = true) ||
                it.name.contains("tvos", ignoreCase = true) ||
                it.name.contains("watchos", ignoreCase = true) ||
                it.name.contains("js", ignoreCase = true)
    }.configureEach {
        exclude(group = "com.revenuecat.purchases", module = "purchases-kmp-core")
        exclude(group = "com.revenuecat.purchases", module = "purchases-kmp-either")
        exclude(group = "com.revenuecat.purchases", module = "purchases-kmp-result")
        exclude(group = "com.revenuecat.purchases", module = "purchases-kmp-ui")
    }

    configurations.matching {
        it.name.contains("tvos", ignoreCase = true) ||
                it.name.contains("watchos", ignoreCase = true)
    }.configureEach {
        exclude(group = "org.jetbrains.compose.ui", module = "ui")
    }

    // Exclude Amazon Appstore SDK globally - we only use Google Play
    configurations.configureEach {
        exclude(group = "com.revenuecat.purchases", module = "purchases-store-amazon")
        exclude(group = "com.amazon.device", module = "amazon-appstore-sdk")
    }
}