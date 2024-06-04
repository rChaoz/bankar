group = "ro.bankar"
version = "1.0"

plugins {
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.kotlin.multiplatform) apply false
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.kotlin.android) apply false
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.android.application) apply false
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.android.library) apply false
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.kotlin.plugin.serialization) apply false
}
