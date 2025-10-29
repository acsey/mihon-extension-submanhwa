plugins {
    id("com.android.application") version "8.2.2" apply false
    id("com.android.library") version "8.2.2" apply false
    kotlin("android") version "1.9.23" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        // Tachiyomi/Mihon sources are published under JitPack or Maven Central depending on fork
        maven { url = uri("https://jitpack.io") }
    }
}
