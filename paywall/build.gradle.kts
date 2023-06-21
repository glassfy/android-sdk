import io.glassfy.paywall.Configuration

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

android {
    compileSdk = Configuration.compileSdk

    defaultConfig {
        minSdk = Configuration.minSdk
        targetSdk = Configuration.targetSdk

        consumerProguardFiles("consumer-rules.pro")

        sourceSets.getByName("main") {
            java.srcDir("src/main/java")
            res.srcDir("src/main/res")
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
    namespace = "io.glassfy.paywall"

    publishing {
        // To publish just one variant, use singleVariant to publish only release
        singleVariant("release")
    }
}

dependencies {
    api(project(":glassfy"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.20")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-process:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    implementation("androidx.fragment:fragment-ktx:1.5.7")
    implementation("com.google.android.material:material:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("androidx.fragment:fragment:1.5.7")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

// Sources
val androidSourcesJar = tasks.register<Jar>("androidSourcesJar") {
    archiveClassifier.set("sources")

    if (project.plugins.findPlugin("com.android.library") != null) {
        from(android.sourceSets["main"].java.srcDirs)
        from(android.sourceSets["main"].res.srcDirs)
    } else {
        from(sourceSets["main"].allSource)
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

                // Generated docs and Sources
                artifact(javadocJar)
                artifact(androidSourcesJar)

                val vcs = "https://github.com/glassfy/android-SDK"
                pom {
                    name.set(Configuration.artifactId)
                    description.set("Glassfy Paywall SDK for android platform")
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
