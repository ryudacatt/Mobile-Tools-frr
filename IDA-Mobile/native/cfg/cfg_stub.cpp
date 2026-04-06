#include "cfg/cfg_stub.h"

namespace ida_mobile::cfg {

bool IsForwardEdge(std::uint64_t from, std::uint64_t to) {
    return to >= from;
}

}  // namespace ida_mobile::cfg

