#pragma once

#include <cstddef>
#include <cstdint>

namespace ida_mobile::disassembler {

// Phase 1 placeholder: returns true if there is at least one byte to decode.
bool CanDecodeBuffer(const std::uint8_t* data, std::size_t size);

}  // namespace ida_mobile::disassembler

