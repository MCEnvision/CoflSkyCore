# Cofl Core

CoflSkyCore is the shared Java client library used by the Coflnet Minecraft mods.
It owns WebSocket transport, protocol dispatch, command forwarding, cached backend
command descriptions, settings models, and auction information.

This fork carries the backend driven trade overlay command. Build with a JDK
compatible with Gradle 9.4. The compiled library targets Java 21.

```sh
./gradlew --no-daemon test build
```

The published dependency is selected by exact Git commit on JitPack using
`com.github.MCEnvision:CoflSkyCore:<commit>`. The Minecraft mod bundles this
library. The trade command also requires the matching SkyModCommands and Fabric
changes. Forking Core does not change the running Coflnet service.

See [technical documentation](docs/general/documentation.md) for command ownership,
wire formats, local integration tests, and compatibility boundaries.
