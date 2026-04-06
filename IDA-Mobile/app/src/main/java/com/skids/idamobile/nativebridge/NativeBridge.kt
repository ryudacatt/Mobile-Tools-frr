package com.skids.idamobile.nativebridge

object NativeBridge {
    init {
        // Loads the C++ shared library built from /native/CMakeLists.txt
        System.loadLibrary("ida_mobile_core")
    }

    external fun getCoreVersion(): String
    // Returns linked Capstone runtime version from native core.
    external fun getCapstoneVersion(): String
    // Runs the native mmap APK parser and returns JSON with summary stats
    external fun inspectApk(fd: Int, declaredSize: Long): String
}
