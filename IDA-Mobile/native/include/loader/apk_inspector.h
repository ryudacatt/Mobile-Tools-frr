#pragma once

#include <cstdint>
#include <string>

namespace ida_mobile::loader {

// Returns a JSON payload describing parsed APK metadata.
std::string InspectApkFromFd(int fd, std::int64_t declared_size);

}  // namespace ida_mobile::loader

