package org.openhab.binding.velbus.handler;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.openhab.binding.velbus.VelbusBindingConstants.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.junit.Before;
import org.junit.Test;

public class VelbusVMB4DCTest extends AbstractVelbusThingTest {
    private static final String TEST_MODULE_ADDRESS = "02";

    private Map<String, Object> thingProperties;
    private Thing dimmerThing;
    private VelbusDimmerHandler velbusDimmerHandler;

    private final ThingUID THING_UID_DIMMER = new ThingUID(THING_TYPE_VMB4DC, "testdimmer");
    private final ChannelUID CHANNEL_CH1 = new ChannelUID(THING_UID_DIMMER, "CH1");
    private final ChannelUID CHANNEL_CH2 = new ChannelUID(THING_UID_DIMMER, "CH2");
    private final ChannelUID CHANNEL_CH3 = new ChannelUID(THING_UID_DIMMER, "CH3");
    private final ChannelUID CHANNEL_CH4 = new ChannelUID(THING_UID_DIMMER, "CH4");
    private final ChannelTypeUID BRIGHTNESS_CHANNEL_TYPEUID = new ChannelTypeUID("velbus", "brightness");

    private final byte[] GET_STATUS_PACKET = new byte[] { (byte) 0x0F, (byte) 0xFB, 0x02, 0x02, (byte) 0xFA,
            (byte) 0xFF, (byte) 0xF9, 0x04 };
    private final byte[] RECALL_PACKET = new byte[] { (byte) 0x0F, (byte) 0xF8, 0x02, 0x05, 0x11, 0x01, 0X00, 0x00,
            0x00, (byte) 0xE0, 0x04 };
    private final byte[] OFF_PACKET = new byte[] { (byte) 0x0F, (byte) 0xF8, 0x02, 0x05, 0x07, 0x02, 0X00, 0x00, 0x00,
            (byte) 0xE9, 0x04 };
    private final byte[] PC10_PACKET = new byte[] { (byte) 0x0F, (byte) 0xF8, 0x02, 0x05, 0x07, 0x04, 0x0A, 0x00, 0x00,
            (byte) 0xDD, 0x04 };
    private final byte[] PC60_PACKET = new byte[] { (byte) 0x0F, (byte) 0xF8, 0x02, 0x05, 0x07, 0x08, 0X3C, 0x00, 0x00,
            (byte) 0XA7, 0x04 };

    @Before
    public void setUp() {
        super.setup();

        thingProperties = new HashMap<>();
        thingProperties.put(MODULE_ADDRESS, TEST_MODULE_ADDRESS);
        dimmerThing = ThingBuilder.create(THING_TYPE_VMB4DC, "testdimmer").withLabel("Dimmer Thing")
                .withBridge(bridge.getUID()).withConfiguration(new Configuration(thingProperties))
                .withChannel(ChannelBuilder.create(CHANNEL_CH1, "Dimmer").withType(BRIGHTNESS_CHANNEL_TYPEUID).build())
                .withChannel(ChannelBuilder.create(CHANNEL_CH2, "Dimmer").withType(BRIGHTNESS_CHANNEL_TYPEUID).build())
                .withChannel(ChannelBuilder.create(CHANNEL_CH3, "Dimmer").withType(BRIGHTNESS_CHANNEL_TYPEUID).build())
                .withChannel(ChannelBuilder.create(CHANNEL_CH4, "Dimmer").withType(BRIGHTNESS_CHANNEL_TYPEUID).build())
                .build();

        velbusDimmerHandler = new VelbusDimmerHandler(dimmerThing) {
            @Override
            protected @Nullable Bridge getBridge() {
                return bridge;
            }
        };
        initializeHandler(velbusDimmerHandler);
    }

    @Test
    public void testOnCommand() {
        velbusDimmerHandler.handleCommand(CHANNEL_CH1, OnOffType.ON);

        List<byte[]> packets = getBridgePackets(2);
        assertThat(packets.get(0), equalTo(GET_STATUS_PACKET));
        assertThat(packets.get(1), equalTo(RECALL_PACKET));
    }

    @Test
    public void testOffCommand() {
        velbusDimmerHandler.handleCommand(CHANNEL_CH2, OnOffType.OFF);

        List<byte[]> packets = getBridgePackets(2);
        assertThat(packets.get(0), equalTo(GET_STATUS_PACKET));
        assertThat(packets.get(1), equalTo(OFF_PACKET));
    }

    @Test
    public void test10PercentCommand() {
        velbusDimmerHandler.handleCommand(CHANNEL_CH3, new PercentType(10));

        List<byte[]> packets = getBridgePackets(2);
        assertThat(packets.get(0), equalTo(GET_STATUS_PACKET));
        assertThat(packets.get(1), equalTo(PC10_PACKET));
    }

    @Test
    public void test60PercentCommand() {
        velbusDimmerHandler.handleCommand(CHANNEL_CH4, new PercentType(60));

        List<byte[]> packets = getBridgePackets(2);
        assertThat(packets.get(0), equalTo(GET_STATUS_PACKET));
        assertThat(packets.get(1), equalTo(PC60_PACKET));
    }
}
