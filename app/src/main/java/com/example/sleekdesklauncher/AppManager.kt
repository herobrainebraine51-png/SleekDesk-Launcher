package com.example.sleekdesklauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable
)

data class IconPackInfo(
    val label: String,
    val packageName: String
)

class AppManager(private val context: Context) {
    private val pm: PackageManager = context.packageManager

    fun getInstalledIconPacks(): List<IconPackInfo> {
        val themes = listOf(
            "org.adw.launcher.THEMES",
            "com.gau.go.launcherex.theme",
            "com.novalauncher.THEME"
        )
        val packs = mutableMapOf<String, IconPackInfo>()

        for (action in themes) {
            val intent = Intent(action)
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            for (info in resolveInfos) {
                val pkg = info.activityInfo.packageName
                if (!packs.containsKey(pkg)) {
                    packs[pkg] = IconPackInfo(
                        label = info.loadLabel(pm).toString(),
                        packageName = pkg
                    )
                }
            }
        }
        return packs.values.sortedBy { it.label.lowercase() }
    }

    fun getInstalledApps(iconPackPackage: String? = null): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val iconMap = iconPackPackage?.let { loadIconPackMapping(it) } ?: emptyMap()

        return resolveInfos.map { resolveInfo ->
            val pkgName = resolveInfo.activityInfo.packageName
            val className = resolveInfo.activityInfo.name
            val componentName = "ComponentInfo{$pkgName/$className}"

            val customIcon = iconPackPackage?.let { pkg ->
                iconMap[componentName]?.let { drawableName ->
                    getDrawableFromPackage(pkg, drawableName)
                }
            } ?: resolveInfo.loadIcon(pm)

            AppInfo(
                label = resolveInfo.loadLabel(pm).toString(),
                packageName = pkgName,
                icon = customIcon
            )
        }.sortedBy { it.label.lowercase() }
    }

    private fun loadIconPackMapping(iconPackPkg: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val iconPackRes = pm.getResourcesForApplication(iconPackPkg)
            val xmlId = iconPackRes.getIdentifier("appfilter", "xml", iconPackPkg)
            if (xmlId != 0) {
                val parser = iconPackRes.getXml(xmlId)
                while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        val component = parser.getAttributeValue(null, "component")
                        val drawable = parser.getAttributeValue(null, "drawable")
                        if (component != null && drawable != null) {
                            map[component] = drawable
                        }
                    }
                    parser.next()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    private fun getDrawableFromPackage(pkg: String, drawableName: String): Drawable? {
        return try {
            val res = pm.getResourcesForApplication(pkg)
            val id = res.getIdentifier(drawableName, "drawable", pkg)
            if (id != 0) res.getDrawable(id, null) else null
        } catch (e: Exception) {
            null
        }
    }
}
