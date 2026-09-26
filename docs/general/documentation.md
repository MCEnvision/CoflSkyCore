# Core command flow

## Ownership and execution

[SkyModCommands](https://github.com/Coflnet/SkyModCommands) registers public
commands in `Commands/MinecraftSocket.cs`. `HelpCommand.BuildCommandList`
serializes command descriptions and declared argument completions into the
existing flat `commandUpdate` dictionary. Root keys are command names. Keys with
spaces contain a complete suggested argument sequence, such as `tradegui on`.

`WSClient.HandleCommand` stores that dictionary in `LocalConfig.knownCommands`.
Fabric uses `CommandSuggestions.matching` to match the entire greedy argument
prefix. Root completion shows command names; after a space it shows matching full
argument sequences. Suggestions replace the greedy argument range, preserving
`/cofl` or `/cl`. Removing a command from a backend update removes its suggestions.
Core does not register a local `tradegui` handler or seed this command into its
fallback list. Cached commands can remain visible while disconnected, as before.

`CoflSkyCommand.processCommand` forwards unhandled commands through the existing
`sendCommandToServer` path. A trade toggle is sent as:

```json
{"type":"tradegui","data":"\"on\""}
```

The backend validates the argument and sends:

```json
{"type":"tradeGui","data":"{\"enabled\":true}"}
```

`enabled` is `true` for on, `false` for off, or `null` for a status request with no
argument. `TradeGuiSettings.parseEnabled` rejects missing fields, strings, numbers,
and nonobject payloads. Core publishes `OnTradeGui` through EventBus. Fabric
subscribes and schedules the persisted setting change and feedback on the
Minecraft client thread. Status reads the client preference and does not mutate it.
The backend does not invent or store the user's local overlay state.

The separate SkyApi HTTP command catalogue is not the runtime completion owner.
Always trace registration, transport, state ownership, and client consumers across
these repositories before changing command behavior.

## Verification

```sh
./gradlew --no-daemon test build
```

The JUnit 4 suite covers wire parsing, malformed payload rejection, existing
protocol behavior, and command cache initialization. Initialization removes the
retired `vps` and `loadfliphistory` keys and adds missing public defaults without
replacing saved descriptions.

For a live transport test, start the explicit `TradeGuiSocketTests` fixture in the
matching Commands checkout, then run:

```sh
SKYCOFL_PROTOCOL_ENDPOINT=ws://127.0.0.1:18084/modsocket \
  ./gradlew --no-daemon test --tests '*.TradeGuiBackendIntegrationTest'
```

This test receives the actual backend registry's command list, matches argument
suggestions, forwards requests through `CoflSkyCommand`, and receives typed Core
events for on, off, and status. It sends `fixtureShutdown` before teardown. Without
the endpoint the integration test is skipped; ordinary protocol unit tests still run.
The fixture skips the backend login and service bootstrap. It does not verify
production authentication, Hypixel menus, rendering performance, or deployment.

## Compatibility

The matching backend, Core dependency, and Fabric consumer must ship together.
Older clients ignore the new response type. The payload envelope and command
update dictionary retain their existing formats. The command requires an active
backend connection; there is no offline command execution bypass.
