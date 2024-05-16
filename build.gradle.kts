// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
}
var versionName = "1.0.0"
var versionCode = 100

rootProject.ext.set("appVersionName", versionName)
rootProject.ext.set("appVersionCode", versionCode)
rootProject.ext.set("applicationId", "com.gboard.nsfw")
    
tasks.register<Delete>("clean").configure {
    delete(getLayout().buildDirectory)
}
tasks.register("getVersion") {
    doLast {
        val versionFile = File("app/build/version.txt")
        versionFile.parentFile.mkdirs()
        if (!versionFile.exists()) {
            versionFile.createNewFile()
        }
        versionFile.writeText(versionName)
    }
}