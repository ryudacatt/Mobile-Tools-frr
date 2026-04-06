#include "analysis/analysis_stub.h"

namespace ida_mobile::analysis {

std::uint32_t ByteChecksum(const std::uint8_t* data, std::size_t size) {
    if (data == nullptr || size == 0) {
        return 0;
    }

    std::uint32_t sum = 0;
    for (std::size_t i = 0; i < size; ++i) {
        sum += data[i];
    }
    return sum;
}

}  // namespace ida_mobile::analysis

