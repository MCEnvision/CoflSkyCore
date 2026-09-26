package CoflCore.network;

import CoflCore.commands.CommandType;
import CoflCore.commands.RawCommand;
import CoflCore.events.OnTradeGui;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TradeGuiProtocolTest {
    public static class Receiver {
        public final List<Boolean> states = new ArrayList<>();

        @Subscribe
        public void receive(OnTradeGui event) {
            states.add(event.enabled);
        }
    }

    @Test
    public void websocketDispatchesEnableDisableAndStatus() throws Exception {
        Receiver receiver = new Receiver();
        EventBus.getDefault().register(receiver);
        try {
            WSClient client = new WSClient(URI.create("ws://localhost"));
            for (String value : new String[]{"true", "false", "null"}) {
                String data = "{\"enabled\":" + value + "}";
                client.onTextMessage(null, WSClient.gson.toJson(new RawCommand("tradeGui", data)));
            }
            assertEquals(java.util.Arrays.asList(true, false, null), receiver.states);
            assertEquals("tradeGui", CommandType.TradeGui.ToJson());
        } finally {
            EventBus.getDefault().unregister(receiver);
        }
    }

    @Test
    public void malformedResponsesNeverDispatchStateChanges() {
        Receiver receiver = new Receiver();
        EventBus.getDefault().register(receiver);
        try {
            for (String payload : new String[]{"{}", "null", "[]", "{\"enabled\":\"false\"}", "{\"enabled\":0}"}) {
                var body = new CoflCore.commands.JsonStringCommand(CommandType.TradeGui, payload);
                assertThrows(IllegalArgumentException.class, () -> WSClient.HandleCommand(body));
            }
            assertTrue(receiver.states.isEmpty());
        } finally {
            EventBus.getDefault().unregister(receiver);
        }
    }
}
