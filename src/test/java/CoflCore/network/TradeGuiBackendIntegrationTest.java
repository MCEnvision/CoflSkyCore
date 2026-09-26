package CoflCore.network;

import CoflCore.CoflCore;
import CoflCore.CoflSkyCommand;
import CoflCore.commands.CommandSuggestions;
import CoflCore.commands.RawCommand;
import CoflCore.configuration.LocalConfig;
import CoflCore.events.OnTradeGui;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.junit.Assume;
import org.junit.Test;

import java.net.URI;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class TradeGuiBackendIntegrationTest {
    public static class Receiver {
        public final LinkedBlockingQueue<OnTradeGui> changes = new LinkedBlockingQueue<>();

        @Subscribe
        public void receive(OnTradeGui event) {
            changes.add(event);
        }
    }

    @Test
    public void forwardsToRegisteredBackendAndReceivesTypedResponses() throws Exception {
        String endpoint = System.getenv("SKYCOFL_PROTOCOL_ENDPOINT");
        Assume.assumeNotNull(endpoint);
        var previousConfig = CoflCore.config;
        var previousWrapper = CoflCore.Wrapper;
        var wrapper = new WSClientWrapper(new String[0]);
        var receiver = new Receiver();
        EventBus.getDefault().register(receiver);
        try {
            CoflCore.config = LocalConfig.createDefaultConfig();
            CoflCore.Wrapper = wrapper;
            wrapper.socket = new WSClient(URI.create(endpoint));
            wrapper.socket.start();
            wrapper.isRunning = true;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!CoflCore.config.knownCommands.containsKey("tradegui off") && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            var suggestions = CommandSuggestions.matching(CoflCore.config.knownCommands, "tradegui o");
            assertEquals(2, suggestions.size());
            assertTrue(CoflCore.config.knownCommands.containsKey("report"));
            for (String state : new String[]{"on", "off", ""}) {
                String[] args = state.isEmpty() ? new String[]{"tradegui"} : new String[]{"tradegui", state};
                CoflSkyCommand.processCommand(args, "protocol-test");
                OnTradeGui event = receiver.changes.poll(8, TimeUnit.SECONDS);
                assertNotNull("No backend response for " + state, event);
                assertEquals(state.isEmpty() ? null : Boolean.valueOf(state.equals("on")), event.enabled);
            }
            wrapper.socket.SendCommand(new RawCommand("fixtureShutdown", "\"\""));
        } finally {
            wrapper.stop();
            CoflCore.Wrapper = previousWrapper;
            CoflCore.config = previousConfig;
            EventBus.getDefault().unregister(receiver);
        }
    }
}
