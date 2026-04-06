#pragma once

#include <cstddef>
#include <cstdint>

namespace ida_mobile::analysis {

std::uint32_t ByteChecksum(const std::uint8_t* data, std::size_t size);

}  // namespace ida_mobile::analysis

