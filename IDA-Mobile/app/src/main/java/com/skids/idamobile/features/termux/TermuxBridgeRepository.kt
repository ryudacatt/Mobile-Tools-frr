package com.skids.idamobile.features.termux

import android.content.Context
import android.content.Intent

/**
 * Practical Termux bridge:
 * - Detects installation state.
 * - Launches Termux UI.
 * - Attempts command dispatch through Termux RUN_COMMAND integration.
 */
class TermuxBridgeRepository {
    fun status(context: Context): TermuxBridgeStatus {
        val installed = isInstalled(context)
        return if (installed) {
            TermuxBridgeStatus(
                installed = true,
                detail = "Termux detected. You can launch Termux or send run-command intents."
            )
        } else {
            TermuxBridgeStatus(
                installed = false,
                detail = "Termux is not installed. Install package 'com.termux' to enable shell bridge."
            )
        }
    }

    fun launch(context: Context): Result<Unit> = runCatching {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
            ?: error("Termux launcher intent not found.")
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }

    fun runCommand(context: Context, command: String): Result<Unit> = runCatching {
        require(command.isNotBlank()) { "Command cannot be blank." }
        check(isInstalled(context)) { "Termux is not installed." }

        val intent = Intent(TERMUX_RUN_COMMAND_ACTION).apply {
            setClassName(TERMUX_PACKAGE, TERMUX_RUN_COMMAND_SERVICE)
            putExtra(TERMUX_EXTRA_PATH, "/data/data/com.termux/files/usr/bin/sh")
            putExtra(TERMUX_EXTRA_ARGUMENTS, arrayOf("-lc", command))
            putExtra(TERMUX_EXTRA_WORKDIR, "/data/data/com.termux/files/home")
            putExtra(TERMUX_EXTRA_BACKGROUND, false)
        }

        context.startService(intent) ?: error("Termux command service did not start.")
    }

    @Suppress("DEPRECATION")
    private fun isInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
        true
    } catch (_: Exception) {
        false
    }

    private companion object {
        private const val TERMUX_PACKAGE = "com.termux"
        private const val TERMUX_RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND"
        private const val TERMUX_RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
        private const val TERMUX_EXTRA_PATH = "com.termux.RUN_COMMAND_PATH"
        private const val TERMUX_EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        private const val TERMUX_EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
        private const val TERMUX_EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    }
}
