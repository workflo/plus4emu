# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Plus4Emu** is a Commodore Plus/4 computer emulator written in Java. It emulates the complete system including:
- 6510 CPU (6502-compatible)
- TED video chip (320x200 pixels, 128-color palette)
- 64KB RAM + 32KB system ROM + 32KB expansion ROM
- Keyboard matrix and joystick input
- Timer system and interrupt handling

**License**: GNU GPL v2
**Author**: Florian Wolff (2009)

## Build and Run Commands

### Using Gradle (Recommended)
```bash
# Build the project
./gradlew build

# Compile classes
./gradlew classes

# Clean build artifacts
./gradlew clean
```

### Using Ant
```bash
# Build JAR file (compiles and packages)
ant dist

# Run the emulator
ant run

# Compile only
ant compile

# Clean build artifacts
ant clean

# Generate API documentation
ant doc

# Clean API docs
ant docclean
```

The built JAR file is created as `plus4.jar` at the project root.

## Architecture Overview

### Core Emulation Loop
The emulator follows a classic fetch-decode-execute cycle:

1. **Plus4Emu.java** - Entry point that creates:
   - JFrame window (960x600, 3x scaled)
   - Screen canvas for rendering
   - Plus4 emulator instance
   - Daemon thread running the emulation loop

2. **Plus4.java** - Main emulator engine:
   - **CPU Execution**: `befehlAusfuehren()` implements 200+ 6510/6502 opcodes
   - **Memory Management**:
     - `Peek(addr)` / `Poke(addr, value)` with ROM/RAM banking
     - Memory map: 0x0000-0xFFFF RAM, 0x8000+ ROM (bankable)
     - I/O registers: 0xFD00-0xFF3F (TED chip, keyboard, joystick)
   - **Timing**: 885 kHz clock, 114 ticks per raster line
   - **Interrupts**: Timer A/B/C and raster interrupts
   - **Video Rendering**: `rasterzeileDarstellen()` generates pixel data for each of 312 raster lines

3. **CpuState.java** - CPU registers and operations:
   - Registers: PC (program counter), A (accumulator), X, Y, SP (stack pointer)
   - Status flags: Carry, Zero, Negative, Overflow, Break, Decimal, Interrupt
   - CPU operations: ADC, SBC, AND, EOR, ROL, ROR, ASL, LSR, CMP, etc.

4. **Screen.java** - Video output:
   - Canvas-based rendering (AWT)
   - 320x200 pixel buffer, scaled 3x
   - 128-color palette loaded from constants
   - `updateLine(line, pixels)` draws one raster line per call

### Memory-Mapped I/O
Key registers in the 0xFF00-0xFF3F range:
- **0xFF00**: Interrupt status/control
- **0xFF06**: Screen control register (bitmap/text mode)
- **0xFF08**: Keyboard/joystick data
- **0xFF09**: Interrupt enable mask
- **0xFF12-0xFF1E**: Character set, color, and screen memory pointers
- **0xFD30**: Keyboard matrix row select
- **0xFExx/0xFFxx**: ROM banking control

### Video Modes
The emulator supports three display modes:
- **Text Mode** (40x25 characters): Uses character ROM and color RAM
- **Hi-Res Bitmap**: Direct pixel manipulation (1 bit per pixel)
- **Multi-Color Mode**: 4-color mode (partially implemented)

Mode selection via bit 5 of register 0xFF06.

### ROM Files
ROM binaries are loaded from classpath resources:
- `src/main/java/de/donuz/plus4/rom/rom.bin` - 32KB system ROM
- `src/main/java/de/donuz/plus4/rom/3plus1.bin` - 32KB 3Plus1 expansion ROM

These are embedded into the JAR during build.

### Keyboard Input
Keyboard matrix is 8x8, accessed via:
- Write row selector to 0xFD30
- Read column data from 0xFF08
- Joystick ports selected via 0xFA (port 1) or 0xFD (port 2)

Current implementation is mostly placeholder code; full keyboard integration requires AWT KeyListener implementation.

## Code Organization

```
src/main/java/de/donuz/plus4/
├── Plus4Emu.java          # Entry point and main window
├── Plus4.java             # Core emulator (CPU, memory, I/O, rendering)
├── CpuState.java          # CPU registers and state operations
├── Screen.java            # Video output canvas
├── Opcode.java            # Instruction lookup table
├── AddressMode.java       # CPU addressing mode enum
├── RomListing.java        # ROM constants
└── helper/
    └── IOHelper.java      # Binary file reading utilities
```

## Key Implementation Details

### Timing Constants
- Clock frequency: 885 kHz
- Refresh rate: 57 Hz (312 raster lines)
- Ticks per raster line: 114
- Visible screen: 200 lines starting at line 3

### CPU Emulation
All 6510 opcodes are implemented in large switch statements in `Plus4.java`:
- Legal opcodes (200+ variants)
- Addressing modes: Immediate, Zero Page, Absolute, Indexed, Indirect
- Each instruction updates CPU state (registers, flags) and consumes clock cycles

### Rendering Pipeline
1. Main loop executes CPU instructions
2. After 114 clock ticks, `rasterzeileDarstellen()` is called
3. Pixel data for current raster line is generated based on video mode
4. `screen.updateLine()` draws the line to canvas
5. Process repeats for all 312 lines (60 times per second)

## Common Development Tasks

When modifying the emulator:
- CPU changes: Edit `Plus4.befehlAusfuehren()` and `CpuState.java`
- Video changes: Edit `Plus4.rasterzeileDarstellen()` and `Screen.java`
- I/O changes: Edit `Peek()` and `Poke()` methods in `Plus4.java`
- Build changes: Edit `build.gradle` or `build.xml`
