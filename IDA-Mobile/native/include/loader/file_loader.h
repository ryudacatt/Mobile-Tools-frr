#pragma once

#include <cstddef>
#include <cstdint>

namespace ida_mobile::loader {

bool IsElfHeader(const std::uint8_t* data, std::size_t size);

}  // namespace ida_mobile::loader

