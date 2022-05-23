import io.glassfy.androidsdk.Configuration

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka")
}

android {
    compileSdk = Configuration.compileSdk

    defaultConfig {
        minSdk = Configuration.minSdk
        targetSdk = Configuration.targetSdk

        consumerProguardFiles("consumer-rules.pro")

        sourceSets.getByName("main") {
            java.srcDir("src/main/java")
        }

        buildConfigField("String", "SDK_VERSION", "\"${rootProject.version}\"")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    packagingOptions {
        // exclude file used by debugger
        resources.excludes += "DebugProbesKt.bin"
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    namespace = "io.glassfy.androidsdk"

    publishing {
        // To publish just one variant, use singleVariant to publish only release
        singleVariant("release")
    }
}

dependencies {
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.1")
    implementation("androidx.lifecycle:lifecycle-process:2.4.1")

    // Android Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")

    // BillingClient
    implementation("com.android.billingclient:billing-ktx:4.1.0")

    // Retrofit + OKHttp + Moshi
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.moshi:moshi:1.13.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.13.0")
//    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.13.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.0")

    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}

// Sources
val androidSourcesJar = tasks.register<Jar>("androidSourcesJar") {
    archiveClassifier.set("sources")

    if (project.plugins.findPlugin("com.android.library") != null) {
        from(android.sourceSets["main"].java.srcDirs)
    } else {
        from(sourceSets["main"].java.srcDirs)
    }
}

// Dokka
val dokkaOutputDir = "$buildDir/dokka"
tasks.getByName<org.jetbrains.dokka.gradle.DokkaTask>("dokkaJavadoc") {
    outputDirectory.set(file(dokkaOutputDir))
}
val javadocJar = tasks.register<Jar>("javadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    archiveClassifier.set("javadoc")
    from(dokkaOutputDir)
}

artifacts {
    archives(androidSourcesJar)
    archives(javadocJar)
}

afterEvaluate {
    publishing {
        publications {
            // Creates a Maven publication called "release".
            create<MavenPublication>("release") {
                groupId = Configuration.artifactGroup
                version = rootProject.version as String
                artifactId = Configuration.artifactId

                if (project.plugins.findPlugin("com.android.library") != null) {
                    from(components["release"])
                } else {
                    from(components["java"])
                }

                artifact(javadocJar)    // Add generated docs
                artifact(androidSourcesJar)    // Add sources

                val vcs = "https://github.com/glassfy/android-SDK"
                pom {
                    name.set(Configuration.artifactId)
                    description.set("Glassfy SDK for android platform")
                    url.set(vcs)

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("http://opensource.org/licenses/MIT")
                        }
                    }
                    issueManagement {
                        system.set("Github")
                        url.set("${vcs}/issues")
                    }
                    developers {
                        developer {
                            name.set("Luca Garbolino")
                            email.set("luca@glassfy.io")
                            organization.set("Glassfy")
                            organizationUrl.set("https://glassfy.io")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/glassfy/android-SDK.git")
                        developerConnection.set("scm:git:ssh://github.com:glassfy/android-SDK.git")
                        url.set("${vcs}/tree/main")
                    }
                }

            }
        }
    }
}

signing {
    useInMemoryPgpKeys(
        rootProject.ext["signing.keyId"] as String,
        rootProject.ext["signing.key"] as String,
        rootProject.ext["signing.password"] as String,
    )
    sign(publishing.publications)
}
