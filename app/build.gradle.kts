import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.example.weather"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.weather"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "QWEATHER_API_KEY",
            "\"${localProperties.getProperty("QWEATHER_API_KEY", "")}\""
        )
        buildConfigField(
            "String",
            "QWEATHER_API_HOST",
            "\"${localProperties.getProperty("QWEATHER_API_HOST", "https://devapi.qweather.com/")}\""
        )
        buildConfigField(
            "String",
            "ALIYUN_BAILIAN_API_KEY",
            "\"${localProperties.getProperty("ALIYUN_BAILIAN_API_KEY", "")}\""
        )
        buildConfigField(
            "String",
            "ALIYUN_BAILIAN_APP_ID",
            "\"${localProperties.getProperty("ALIYUN_BAILIAN_APP_ID", "")}\""
        )
        buildConfigField(
            "String",
            "ALIYUN_BAILIAN_BASE_URL",
            "\"${localProperties.getProperty("ALIYUN_BAILIAN_BASE_URL", "https://dashscope.aliyuncs.com")}\""
        )
        buildConfigField(
            "String",
            "ALIYUN_BAILIAN_MODEL",
            "\"${localProperties.getProperty("ALIYUN_BAILIAN_MODEL", "qwen-plus")}\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.swiperefreshlayout)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation("io.coil-kt:coil:2.7.0")
    implementation(libs.room.runtime)
    implementation(libs.glide)
    annotationProcessor(libs.room.compiler)
    annotationProcessor(libs.glide.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
