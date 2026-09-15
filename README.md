# WOB Companion

[![Build and Release](https://github.com/freausan/wob-companion/actions/workflows/build-and-release.yml/badge.svg)](https://github.com/freausan/wob-companion/actions/workflows/build-and-release.yml)

A lightweight Fabric mod for Minecraft 26.1 acting as an in-game accessory/companion to the [Wise Old Block](https://wiseoldblock.xyz) API on Hypixel Skyblock.

## Features

- **Fabric 26.1 / 26.1.2** compatible (Java 25)
- **Wise Old Block API Integration**: Connects seamlessly with the wiseoldblock.xyz backend
- **Client Commands**:
  - /wob status or /companion status - View connection and configuration status
  - /wob ping or /companion ping - Test API connectivity
  - /wob toggle or /companion toggle - Quickly enable/disable mod functionality
  - /wob help or /companion help - Display available commands
- **Automated CI/CD**: Automatic builds on push/PR and automated GitHub Releases with packaged JARs on * tags.

## Building from Source

Ensure you have **Java 25** installed:

\\\ash
# Build mod JAR
./gradlew build
\\\

The built JAR will be located in uild/libs/.

## License

[MIT](LICENSE)
