package com.skids.idamobile.fileloader

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * Builds a debugger-oriented metadata report from a standalone APK file.
 */
class ApkDebuggerRepository {
    fun inspect(context: Context, apkFile: File, displayName: String): Result<ApkDebugReport> = runCatching {
        val packageInfo = readArchivePackageInfo(context.packageManager, apkFile.absolutePath)
            ?: error("PackageManager could not parse this APK archive.")

        val applicationInfo = packageInfo.applicationInfo
            ?: error("ApplicationInfo is missing for archive package info.")

        // Point the archive-backed ApplicationInfo to the selected file
        applicationInfo.sourceDir = apkFile.absolutePath
        applicationInfo.publicSourceDir = apkFile.absolutePath

        ApkDebugReport(
            displayName = displayName,
            packageName = packageInfo.packageName.orEmpty(),
            versionName = packageInfo.versionName,
            versionCode = packageInfo.longVersionCodeCompat(),
            minSdkVersion = applicationInfo.minSdkVersionCompat(),
            targetSdkVersion = applicationInfo.targetSdkVersion,
            isDebuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0,
            permissions = packageInfo.requestedPermissions.orEmpty().toList().sorted(),
            activities = packageInfo.activities.orEmpty().mapNotNull { it.name }.sorted(),
            services = packageInfo.services.orEmpty().mapNotNull { it.name }.sorted(),
            receivers = packageInfo.receivers.orEmpty().mapNotNull { it.name }.sorted(),
            providers = packageInfo.providers.orEmpty().mapNotNull { it.name }.sorted(),
            nativeLibraries = collectNativeLibraries(apkFile),
            certificateSha256 = collectCertificateDigests(packageInfo)
        )
    }

    private fun readArchivePackageInfo(packageManager: PackageManager, path: String): PackageInfo? {
        val flags = (
            PackageManager.GET_ACTIVITIES or
                PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS or
                PackageManager.GET_PROVIDERS or
                PackageManager.GET_PERMISSIONS or
                PackageManager.GET_SIGNING_CERTIFICATES
            )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(path, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(path, flags)
        }
    }

    private fun collectNativeLibraries(apkFile: File): List<String> {
        ZipFile(apkFile).use { zip ->
            val nativeLibs = mutableSetOf<String>()
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name
                if (!entry.isDirectory && name.startsWith("lib/") && name.endsWith(".so")) {
                    nativeLibs += name
                }
            }
            return nativeLibs.sorted()
        }
    }

    private fun collectCertificateDigests(packageInfo: PackageInfo): List<String> {
        val signatures: List<ByteArray> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners?.map { it.toByteArray() }.orEmpty()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures?.map { it.toByteArray() }.orEmpty()
        }

        return signatures
            .map { bytes -> sha256(bytes) }
            .distinct()
            .sorted()
    }

    private fun sha256(content: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(content)
        return buildString(digest.size * 2) {
            digest.forEach { byte -> append("%02x".format(byte.toInt() and 0xff)) }
        }
    }

    private fun PackageInfo.longVersionCodeCompat(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode.toLong()
        }
    }

    private fun android.content.pm.ApplicationInfo.minSdkVersionCompat(): Int? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            minSdkVersion
        } else {
            null
        }
    }
}

