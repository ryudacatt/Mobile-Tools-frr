cmake_minimum_required(VERSION 3.22.1)
project(ida_mobile_core LANGUAGES C CXX)

set(CAPSTONE_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../third_party/capstone)
if(NOT EXISTS ${CAPSTONE_SOURCE_DIR}/CMakeLists.txt)
    message(FATAL_ERROR "Capstone source not found at ${CAPSTONE_SOURCE_DIR}. Run scripts/setup_third_party.ps1.")
endif()

# Keep Capstone build lean and deterministic for mobile APK usage.
set(CAPSTONE_BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(CAPSTONE_BUILD_STATIC_LIBS ON CACHE BOOL "" FORCE)
set(CAPSTONE_BUILD_CSTOOL OFF CACHE BOOL "" FORCE)
set(CAPSTONE_BUILD_CSTEST OFF CACHE BOOL "" FORCE)
set(CAPSTONE_BUILD_LEGACY_TESTS OFF CACHE BOOL "" FORCE)
set(CAPSTONE_ARCHITECTURE_DEFAULT OFF CACHE BOOL "" FORCE)
set(CAPSTONE_ARM_SUPPORT ON CACHE BOOL "" FORCE)
set(CAPSTONE_AARCH64_SUPPORT ON CACHE BOOL "" FORCE)
set(CAPSTONE_X86_SUPPORT ON CACHE BOOL "" FORCE)
add_subdirectory(${CAPSTONE_SOURCE_DIR} ${CMAKE_BINARY_DIR}/capstone-build EXCLUDE_FROM_ALL)

add_library(
    ida_mobile_core
    SHARED
    core/core_version.cpp
    jni/jni_bridge.cpp
    loader/file_loader.cpp
    loader/apk_inspector.cpp
    memory/memory_map.cpp
    disassembler/disassembler_stub.cpp
    analysis/analysis_stub.cpp
    cfg/cfg_stub.cpp
)

target_include_directories(
    ida_mobile_core
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CAPSTONE_SOURCE_DIR}/include
)

target_compile_features(ida_mobile_core PRIVATE cxx_std_20)
target_compile_options(
    ida_mobile_core
    PRIVATE
    -Wall
    -Wextra
    -Werror
)

find_library(log-lib log)
target_link_libraries(
    ida_mobile_core
    PRIVATE
    capstone_static
    ${log-lib}
)
