cmake_minimum_required(VERSION 3.22.1)

project("native_enhancer")

add_library(
    native_enhancer
    SHARED
    native_enhancer.cpp
)

find_library(
    log-lib
    log
)

find_library(
    jnigraphics-lib
    jnigraphics
)

target_link_libraries(
    native_enhancer
    ${log-lib}
    ${jnigraphics-lib}
)
