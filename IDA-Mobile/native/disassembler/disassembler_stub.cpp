#include "disassembler/disassembler_stub.h"

namespace ida_mobile::disassembler {

bool CanDecodeBuffer(const std::uint8_t* data, std::size_t size) {
    return data != nullptr && size > 0;
}

}  // namespace ida_mobile::disassembler

