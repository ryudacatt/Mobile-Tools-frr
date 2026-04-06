#include "loader/apk_inspector.h"

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <limits>
#include <string>
#include <string_view>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

namespace ida_mobile::loader {
namespace {

constexpr std::uint32_t kLocalFileHeaderSignature = 0x04034b50;
constexpr std::uint32_t kCentralDirectorySignature = 0x02014b50;
constexpr std::uint32_t kEndOfCentralDirectorySignature = 0x06054b50;
constexpr std::size_t kEndOfCentralDirectoryMinSize = 22;
constexpr std::size_t kMaxCommentAndEocdWindow = 0x10000 + kEndOfCentralDirectoryMinSize;

std::uint16_t ReadU16(const std::uint8_t* data) {
    return static_cast<std::uint16_t>(data[0] | (data[1] << 8));
}

std::uint32_t ReadU32(const std::uint8_t* data) {
    return static_cast<std::uint32_t>(
        data[0] |
        (data[1] << 8) |
        (data[2] << 16) |
        (data[3] << 24)
    );
}

std::string JsonEscape(std::string_view value) {
    std::string out;
    out.reserve(value.size() + 8);
    for (char ch : value) {
        switch (ch) {
            case '\\':
                out += "\\\\";
                break;
            case '"':
                out += "\\\"";
                break;
            case '\n':
                out += "\\n";
                break;
            case '\r':
                out += "\\r";
                break;
            case '\t':
                out += "\\t";
                break;
            default:
                out += ch;
                break;
        }
    }
    return out;
}

std::string ErrorJson(std::string_view message) {
    return std::string("{\"ok\":false,\"error\":\"") + JsonEscape(message) + "\"}";
}

bool IsDexName(std::string_view name) {
    if (name.size() < 11) {
        return false;
    }
    if (!name.starts_with("classes") || !name.ends_with(".dex")) {
        return false;
    }

    for (std::size_t i = 7; i + 4 < name.size(); ++i) {
        if (name[i] < '0' || name[i] > '9') {
            return false;
        }
    }
    return true;
}

std::int64_t ResolveFileSize(int fd, std::int64_t declared_size) {
    if (declared_size > 0) {
        return declared_size;
    }

    struct stat st {};
    if (fstat(fd, &st) != 0) {
        return -1;
    }
    return static_cast<std::int64_t>(st.st_size);
}

}  // namespace

std::string InspectApkFromFd(int fd, std::int64_t declared_size) {
    if (fd < 0) {
        return ErrorJson("Invalid file descriptor.");
    }

    const std::int64_t resolved_size = ResolveFileSize(fd, declared_size);
    if (resolved_size <= 0) {
        return ErrorJson("Could not determine APK size.");
    }
    if (resolved_size > static_cast<std::int64_t>(std::numeric_limits<std::size_t>::max())) {
        return ErrorJson("APK is too large to map on this device.");
    }

    const std::size_t size = static_cast<std::size_t>(resolved_size);
    if (size < 4) {
        return ErrorJson("File is too small to be an APK.");
    }
    if (size < kEndOfCentralDirectoryMinSize) {
        return ErrorJson("File is too small to contain ZIP metadata.");
    }

    void* mapped = mmap(nullptr, size, PROT_READ, MAP_PRIVATE, fd, 0);
    if (mapped == MAP_FAILED) {
        return ErrorJson("mmap failed while opening APK.");
    }

    const auto* bytes = static_cast<const std::uint8_t*>(mapped);
    auto unmap = [&mapped, size]() {
        munmap(mapped, size);
    };

    if (ReadU32(bytes) != kLocalFileHeaderSignature) {
        unmap();
        return ErrorJson("Not a ZIP/APK local file header.");
    }

    const std::size_t search_window = std::min(size, kMaxCommentAndEocdWindow);
    const std::size_t search_start = size - search_window;
    std::size_t eocd_offset = size;

    for (std::size_t i = size - kEndOfCentralDirectoryMinSize + 1; i-- > search_start;) {
        if (ReadU32(bytes + i) == kEndOfCentralDirectorySignature) {
            eocd_offset = i;
            break;
        }
    }

    if (eocd_offset == size) {
        unmap();
        return ErrorJson("EOCD record not found in APK.");
    }

    const std::uint16_t total_entries = ReadU16(bytes + eocd_offset + 10);
    const std::uint32_t central_size = ReadU32(bytes + eocd_offset + 12);
    const std::uint32_t central_offset = ReadU32(bytes + eocd_offset + 16);

    if (static_cast<std::size_t>(central_offset) + static_cast<std::size_t>(central_size) > size) {
        unmap();
        return ErrorJson("Corrupt central directory bounds.");
    }

    std::size_t cursor = central_offset;
    std::uint32_t parsed_entries = 0;
    std::uint32_t dex_files = 0;
    bool has_manifest = false;

    while (cursor + 46 <= size && parsed_entries < total_entries) {
        if (ReadU32(bytes + cursor) != kCentralDirectorySignature) {
            unmap();
            return ErrorJson("Invalid central directory entry signature.");
        }

        const std::uint16_t name_len = ReadU16(bytes + cursor + 28);
        const std::uint16_t extra_len = ReadU16(bytes + cursor + 30);
        const std::uint16_t comment_len = ReadU16(bytes + cursor + 32);

        const std::size_t name_start = cursor + 46;
        const std::size_t name_end = name_start + static_cast<std::size_t>(name_len);
        const std::size_t next = name_end + static_cast<std::size_t>(extra_len) + static_cast<std::size_t>(comment_len);

        if (name_end > size || next > size || next <= cursor) {
            unmap();
            return ErrorJson("Corrupt central directory entry lengths.");
        }

        const std::string_view name(reinterpret_cast<const char*>(bytes + name_start), name_len);
        if (name == "AndroidManifest.xml") {
            has_manifest = true;
        }
        if (IsDexName(name)) {
            ++dex_files;
        }

        cursor = next;
        ++parsed_entries;
    }

    unmap();

    return std::string("{\"ok\":true,\"fileSize\":") + std::to_string(size) +
           ",\"entries\":" + std::to_string(parsed_entries) +
           ",\"dexFiles\":" + std::to_string(dex_files) +
           ",\"hasManifest\":" + (has_manifest ? "true" : "false") + "}";
}

}  // namespace ida_mobile::loader
