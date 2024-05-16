package com.gboard.nsfw.xposed

import android.net.Uri
import com.gboard.nsfw.utils.UriUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.Unhook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.util.concurrent.Callable


class NSFW : Callable<Void?> {
    private fun preHookProcess(className: String) {
        try {
            findingTenorServer(XposedHelpers.findClass(className, mainClassLoader))
        } catch (ignored: Throwable) {
        }
    }

    private fun hookMethod(className: String) {
        try {
            val findClass = XposedHelpers.findClass(className, mainClassLoader)
            val methods = XposedHelpers.findMethodsByExactParameters(
                findClass, Void.TYPE, Uri::class.java
            )
            for (method in methods) {
                val unhook = XposedBridge.hookMethod(method, HookMethodClass(findClass))
                hookedMethods.add(unhook)
            }
        } catch (ignored: Throwable) {

        }
    }

    override fun call(): Void? {
        var className: String
        var c = 'a'
        while (c <= 'z') {
            var c2 = 'a'
            while (c2 <= 'z') {
                var c3 = 'a'
                while (c3 <= 'z') {
                    className = c.toString() + c2 + c3
                    if (!tenorServerFounded) {
                        preHookProcess(className)
                    }
                    hookMethod(className)
                    c3 = (c3.code + 1).toChar()
                }
                c2 = (c2.code + 1).toChar()
            }
            c = (c.code + 1).toChar()
        }
//        XposedBridge.log("(" + hookedMethods.size + ") Method(s) are Hooked.")
        return null
    }

    class HookMethodClass internal constructor(private val hookClass: Class<*>) : XC_MethodHook() {
        override fun afterHookedMethod(methodHookParam: MethodHookParam) {
            try {
                val findFirstFieldByExactType = XposedHelpers.findFirstFieldByExactType(
                    hookClass, Uri::class.java
                )
                val obj = findFirstFieldByExactType?.get(methodHookParam.thisObject).toString()
                if (obj.contains("https://www.google.com/search")) {
                    val buildUpon: Uri.Builder = UriUtils.removeUriParameters(
                        Uri.parse(obj), mutableListOf("client", "safe")
                    ).appendQueryParameter("safe", "off").appendQueryParameter("client", "chrome")
                    buildUpon.scheme("https").authority((methodHookParam.args[0] as Uri).authority)
                    findFirstFieldByExactType[methodHookParam.thisObject] = buildUpon.build()
                }
                for (field in hookClass.declaredFields) {
                    if (field.type != String::class.java) {
                        continue
                    }
                    if (!(field[methodHookParam.thisObject] as String).contains("Mozilla/5.0 (Linux; ")) {
                        continue
                    }
                    if (!field.isAccessible) {
                        field.isAccessible = true
                    }
                    field[methodHookParam.thisObject] =
                        "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 7 Build/MOB30X) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Mobile Safari/537.36"
                    break
                }
            } catch (_: Throwable) {
                var unhook: Unhook? = null
                for (method in hookedMethods) {
                    if (method.hookedMethod != methodHookParam.method) {
                        continue
                    }
                    method.unhook()
                    unhook = method
                }
                if (unhook != null) {
                    hookedMethods.remove(unhook)
//                    XposedBridge.log("${unhook.hookedMethod.declaringClass}#${unhook.hookedMethod.name} unhooked")
                }
            }
        }
    }

    companion object {
        private val hookedMethods: MutableList<Unhook> = ArrayList()
        private var mainClassLoader: ClassLoader? = null
        private var tenorServerFounded = false
        private fun findingTenorServer(cls: Class<*>) {
            for (field in cls.declaredFields) {
                val inField: Any = try {
                    field[null]
                } catch (ignored: Throwable) {
                    continue
                } ?: continue
                if (!inField.toString().contains("tenor_server_url_search_v2")) {
                    continue
                }
                val preField = findEmptyField(cls)
                if (preField == null) {
                    XposedBridge.log("Failed to start due to preField is null")
                    break
                }
                field.isAccessible = true
                try {
                    field[null] = preField
                    tenorServerFounded = true
                    XposedBridge.log("NSFW Gboard Started... at $cls")
                    break
                } catch (e: Throwable) {
                    XposedBridge.log("Unable to find tenor server url! due to " + e.message)
                }
            }
        }

        private fun findEmptyField(cls: Class<*>): Any? {
            var inField: Any?
            for (field in cls.declaredFields) {
                inField = try {
                    field[null]
                } catch (unused: Throwable) {
                    continue
                }
                if (inField == null || !inField.toString()
                        .contains("enable_tenor_category_v2_for_language_tags")
                ) {
                    continue
                }

                return inField
            }
            return null
        }

        fun setClassLoader(classLoader: ClassLoader?) {
            mainClassLoader = classLoader
        }
    }
}