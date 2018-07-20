package com.optimizely.ab.event

enum class ClientEngine private constructor(val clientEngineValue: String) {
    JAVA_SDK("java-sdk"),
    ANDROID_SDK("android-sdk"),
    ANDROID_TV_SDK("android-tv-sdk")
}