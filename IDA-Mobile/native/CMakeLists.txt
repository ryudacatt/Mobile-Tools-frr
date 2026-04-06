cmake_minimum_required(VERSION 3.22.1)
project(ida_mobile_core LANGUAGES C CXX)

add_library(
    ida_mobile_core
    SHARED
    core/core_version.cpp
    jni/jni_bridge.cpp
    loader/file_loader.cpp
    memory/memory_map.cpp
    disassembler/disassembler_stub.cpp
    analysis/analysis_stub.cpp
    cfg/cfg_stub.cpp
)

target_include_directories(
    ida_mobile_core
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
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
target_link_libraries(ida_mobile_core PRIVATE ${log-lib})

