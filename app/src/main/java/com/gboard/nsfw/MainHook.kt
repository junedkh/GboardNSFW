package com.gboard.nsfw

import com.gboard.nsfw.xposed.NSFW
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.util.concurrent.FutureTask


class MainHook : IXposedHookLoadPackage {
    @Throws(ClassNotFoundException::class)
    override fun handleLoadPackage(loadPackageParam: LoadPackageParam) {
        if (loadPackageParam.packageName != PACKAGE_NAME) {
            return
        }
        val classLoader = loadPackageParam.classLoader
        XposedBridge.log("Initializing... NSFW Gboard...")
        try {
            val task = NSFW()
            NSFW.setClassLoader(classLoader)
            Thread(FutureTask<Void>(task)).start()
        } catch (ignored: Throwable) {
            XposedBridge.log("Failed to start NSFW Gboard...")
        }
    }

    companion object {
        private const val PACKAGE_NAME = "com.google.android.inputmethod.latin"

    }
}