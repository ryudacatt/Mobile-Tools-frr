# third_party

Vendored upstream source dependencies used by the mobile reverse-engineering stack.

## Repositories

- `capstone`: disassembly engine (linked from NDK build)
- `radare2`: decompiler/analysis backend (Android binary build + app bridge)
- `lief`: binary parsing and mutation toolkit (reserved for advanced modules)
- `ghidra`: reference backend source (external integration target)
- `ogdf`: graph algorithms/layout source (reserved for CFG graph phase)

## Setup

From project root:

```powershell
.\scripts\setup_third_party.ps1
```

## Version Pinning

Pinned commit heads are documented in [LOCKFILE.md](LOCKFILE.md).

Update lock entries when repository heads are intentionally changed.
