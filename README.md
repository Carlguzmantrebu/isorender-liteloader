# Isometric Render - LiteLoader 1.12.2

Clean MCP-based implementation with direct Minecraft access.

## Build Instructions

1. Run decompile workspace (first time only):
```bash
./gradlew setupDecompWorkspace
```

2. Build the mod:
```bash
./gradlew build
```

The output JAR will be in `build/libs/`

## Usage

`/isorender area <x1> <y1> <z1> <x2> <y2> <z2>`

Opens GUI with:
- Scale slider
- Rotation slider  
- Resolution slider
- Export PNG button

## Features

- Renders blocks, tile entities (chests, furnaces, etc), and entities
- Clean implementation using MCP mappings
- No reflection required
- Direct Minecraft API access
