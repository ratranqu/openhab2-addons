/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.velbus.internal.handler;

import static org.openhab.binding.velbus.internal.VelbusBindingConstants.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.velbus.internal.VelbusChannelIdentifier;
import org.openhab.binding.velbus.internal.packets.VelbusCounterStatusRequestPacket;
import org.openhab.binding.velbus.internal.packets.VelbusPacket;

/**
 * The {@link VelbusVMB7INHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Cedric Boon - Initial contribution
 */
public class VelbusVMB7INHandler extends VelbusSensorWithAlarmClockHandler {
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = new HashSet<>(Arrays.asList(THING_TYPE_VMB7IN));

    private final ChannelUID COUNTER_1_CHANNEL = new ChannelUID(thing.getUID(), "counter#COUNTER1");
    private final ChannelUID COUNTER_2_CHANNEL = new ChannelUID(thing.getUID(), "counter#COUNTER2");
    private final ChannelUID COUNTER_3_CHANNEL = new ChannelUID(thing.getUID(), "counter#COUNTER3");
    private final ChannelUID COUNTER_4_CHANNEL = new ChannelUID(thing.getUID(), "counter#COUNTER4");

    private ScheduledFuture<?> refreshJob;

    public VelbusVMB7INHandler(Thing thing) {
        super(thing, 0);
    }

    @Override
    public void initialize() {
        super.initialize();

        initializeAutomaticRefresh();
    }

    private void initializeAutomaticRefresh() {
        Object refreshIntervalObject = getConfig().get(REFRESH_INTERVAL);
        if (refreshIntervalObject != null) {
            int refreshInterval = ((BigDecimal) refreshIntervalObject).intValue();

            if (refreshInterval > 0) {
                startAutomaticRefresh(refreshInterval);
            }
        }
    }

    @Override
    public void dispose() {
        refreshJob.cancel(true);
    }

    private void startAutomaticRefresh(int refreshInterval) {
        VelbusBridgeHandler velbusBridgeHandler = getVelbusBridgeHandler();
        if (velbusBridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }

        refreshJob = scheduler.scheduleWithFixedDelay(() -> {
            sendCounterStatusRequest(velbusBridgeHandler, ALL_CHANNELS);
        }, 0, refreshInterval, TimeUnit.SECONDS);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        super.handleCommand(channelUID, command);

        VelbusBridgeHandler velbusBridgeHandler = getVelbusBridgeHandler();
        if (velbusBridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }

        if (command instanceof RefreshType) {
            if (channelUID.equals(COUNTER_1_CHANNEL)) {
                sendCounterStatusRequest(velbusBridgeHandler, (byte) 0x01);
            } else if (channelUID.equals(COUNTER_2_CHANNEL)) {
                sendCounterStatusRequest(velbusBridgeHandler, (byte) 0x02);
            } else if (channelUID.equals(COUNTER_3_CHANNEL)) {
                sendCounterStatusRequest(velbusBridgeHandler, (byte) 0x03);
            } else if (channelUID.equals(COUNTER_4_CHANNEL)) {
                sendCounterStatusRequest(velbusBridgeHandler, (byte) 0x04);
            }
        }
    }

    protected void sendCounterStatusRequest(VelbusBridgeHandler velbusBridgeHandler, byte channel) {
        VelbusCounterStatusRequestPacket packet = new VelbusCounterStatusRequestPacket(
                new VelbusChannelIdentifier(getModuleAddress().getAddress(), channel));

        byte[] packetBytes = packet.getBytes();
        velbusBridgeHandler.sendPacket(packetBytes);
    }

    @Override
    public void onPacketReceived(byte[] packet) {
        super.onPacketReceived(packet);

        logger.trace("onPacketReceived() was called");

        if (packet[0] == VelbusPacket.STX && packet.length >= 5) {
            byte command = packet[4];

            if (command == COMMAND_COUNTER_STATUS && packet.length >= 6) {
                int counterChannel = packet[5] & 0x03;
                int pulsesPerUnit = ((packet[5] & 0x7C) / 0x100) * 0x64;

                float counterValue = ((packet[6] << 24) + (packet[7] << 16) + (packet[8] << 8) + packet[9])
                        / pulsesPerUnit;

                switch (counterChannel) {
                    case 0x01:
                        updateState(COUNTER_1_CHANNEL, new DecimalType(counterValue));
                        break;
                    case 0x02:
                        updateState(COUNTER_2_CHANNEL, new DecimalType(counterValue));
                        break;
                    case 0x03:
                        updateState(COUNTER_3_CHANNEL, new DecimalType(counterValue));
                        break;
                    case 0x04:
                        updateState(COUNTER_4_CHANNEL, new DecimalType(counterValue));
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "The given channel is not a counter channel: " + counterChannel);
                }
            }
        }
    }
}
