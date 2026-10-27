import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Monotonic version code: number of commits in the history (1 when built outside of git).
val commitCount: Int = runCatching {
    providers.exec { commandLine("git", "rev-list", "--count", "HEAD") }
        .standardOutput.asText.get().trim().toInt()
}.getOrDefault(1)

// A private keystore can be provided through environment variables (see README);
// otherwise the project keystore is used so that every build can update the installed app.
val externalKeystore: String? = providers.environmentVariable("MUSCUAPP_KEYSTORE_FILE").orNull

android {
    namespace = "com.lehyanflouriot.muscuapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lehyanflouriot.muscuapp"
        minSdk = 26
        targetSdk = 36
        versionCode = commitCount
        versionName = "1.2.0"
    }

    signingConfigs {
        create("muscuapp") {
            if (externalKeystore != null) {
                storeFile = file(externalKeystore)
                storePassword = providers.environmentVariable("MUSCUAPP_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("MUSCUAPP_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("MUSCUAPP_KEY_PASSWORD").get()
            } else {
                storeFile = rootProject.file("signing/muscuapp.jks")
                storePassword = "muscuapp"
                keyAlias = "muscuapp"
                keyPassword = "muscuapp"
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("muscuapp")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("muscuapp")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { test ->
                // Screenshot tests write their images to app/build/outputs/roborazzi.
                test.systemProperty("roborazzi.test.record", "true")
                test.maxHeapSize = "2g"
                test.testLogging {
                    events("failed", "standard_out")
                    exceptionFormat = TestExceptionFormat.FULL
                }
            }
        }
    }

    lint {
        abortOnError = false
        textReport = true
        // Special value understood by lint: print the report in the build log.
        textOutput = File("stdout")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
