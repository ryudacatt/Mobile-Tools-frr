#pragma once

#include <cstddef>
#include <cstdint>

namespace ida_mobile::memory {

struct Region {
    std::uint64_t start;
    std::uint64_t end;
};

bool IsValidRegion(const Region& region);

}  // namespace ida_mobile::memory

