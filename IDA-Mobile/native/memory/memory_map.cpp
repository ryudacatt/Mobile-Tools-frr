#include "memory/memory_map.h"

namespace ida_mobile::memory {

bool IsValidRegion(const Region& region) {
    return region.start < region.end;
}

}  // namespace ida_mobile::memory

