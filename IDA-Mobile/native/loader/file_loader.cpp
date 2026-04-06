#include "loader/file_loader.h"

namespace ida_mobile::loader {

bool IsElfHeader(const std::uint8_t* data, std::size_t size) {
    if (data == nullptr || size < 4) {
        return false;
    }
    return data[0] == 0x7f && data[1] == 'E' && data[2] == 'L' && data[3] == 'F';
}

}  // namespace ida_mobile::loader

