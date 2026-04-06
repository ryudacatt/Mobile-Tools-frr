package com.geeks.idamobile.nativebridge

object NativeBridge {
    init {
        // Loads the C++ shared library built from /native/CMakeLists.txt.
        System.loadLibrary("ida_mobile_core")
    }

    external fun getCoreVersion(): String
}

