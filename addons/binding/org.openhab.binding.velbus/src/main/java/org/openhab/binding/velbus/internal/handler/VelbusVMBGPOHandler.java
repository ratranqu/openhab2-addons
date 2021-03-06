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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.velbus.internal.packets.VelbusMemoTextPacket;
import org.openhab.binding.velbus.internal.packets.VelbusPacket;

/**
 * The {@link VelbusVMBGPOHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Cedric Boon - Initial contribution
 */
public class VelbusVMBGPOHandler extends VelbusThermostatHandler {
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = new HashSet<>(
            Arrays.asList(THING_TYPE_VMBGPO, THING_TYPE_VMBGPOD));

    public static final int MODULESETTINGS_MEMORY_ADDRESS = 0x02F0;
    public static final int LAST_MEMORY_LOCATION_ADDRESS = 0x1A03;

    private final int MEMO_TEXT_MAX_LENGTH = 63;

    private final ChannelUID MEMO_CHANNEL = new ChannelUID(thing.getUID(), "oledDisplay#MEMO");
    private final ChannelUID SCREENSAVER_CHANNEL = new ChannelUID(thing.getUID(), "oledDisplay#SCREENSAVER");

    private byte moduleSettings;

    public VelbusVMBGPOHandler(Thing thing) {
        super(thing, 4, new ChannelUID(thing.getUID(), "input#CH33"));
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        super.handleCommand(channelUID, command);

        VelbusBridgeHandler velbusBridgeHandler = getVelbusBridgeHandler();
        if (velbusBridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }

        if (channelUID.equals(SCREENSAVER_CHANNEL) && command instanceof RefreshType) {
            sendReadMemoryPacket(velbusBridgeHandler, MODULESETTINGS_MEMORY_ADDRESS);
        }

        if (channelUID.equals(SCREENSAVER_CHANNEL) && command instanceof OnOffType) {
            byte screenSaverOnOffByte = (byte) ((command == OnOffType.ON) ? 0x80 : 0x00);
            moduleSettings = (byte) (screenSaverOnOffByte | (moduleSettings & 0x7F));
            sendWriteMemoryPacket(velbusBridgeHandler, MODULESETTINGS_MEMORY_ADDRESS, moduleSettings);
            sendWriteMemoryPacket(velbusBridgeHandler, LAST_MEMORY_LOCATION_ADDRESS, (byte) 0xFF);
        }

        if (channelUID.equals(MEMO_CHANNEL) && command instanceof StringType) {
            String memoText = ((StringType) command).toFullString();
            String trucatedMemoText = memoText.substring(0, Math.min(memoText.length(), MEMO_TEXT_MAX_LENGTH));
            String[] splittedMemoText = trucatedMemoText.split("(?<=\\G.....)");

            for (int i = 0; i < splittedMemoText.length; i++) {
                VelbusMemoTextPacket packet = new VelbusMemoTextPacket(getModuleAddress().getAddress(), (byte) (i * 5),
                        splittedMemoText[i].toCharArray());

                byte[] packetBytes = packet.getBytes();
                velbusBridgeHandler.sendPacket(packetBytes);

                // The last character must be zero
                if ((((i * 5) + 5) >= trucatedMemoText.length()) && (splittedMemoText[i].length() == 5)) {
                    packet = new VelbusMemoTextPacket(getModuleAddress().getAddress(), (byte) ((i + 1) * 5),
                            new char[0]);

                    packetBytes = packet.getBytes();
                    velbusBridgeHandler.sendPacket(packetBytes);
                }
            }
        }
    }

    @Override
    public void onPacketReceived(byte[] packet) {
        super.onPacketReceived(packet);

        logger.trace("onPacketReceived() was called");

        if (packet[0] == VelbusPacket.STX && packet.length >= 5) {
            byte command = packet[4];

            if ((command == COMMAND_MEMORY_DATA_BLOCK && packet.length >= 11)
                    || (command == COMMAND_MEMORY_DATA && packet.length >= 8)) {
                byte highMemoryAddress = packet[5];
                byte lowMemoryAddress = packet[6];
                int memoryAddress = ((highMemoryAddress & 0xff) << 8) | (lowMemoryAddress & 0xff);
                byte[] data = (command == COMMAND_MEMORY_DATA_BLOCK)
                        ? new byte[] { packet[7], packet[8], packet[9], packet[10] }
                        : new byte[] { packet[7] };

                for (int i = 0; i < data.length; i++) {

                    if ((memoryAddress + i) == MODULESETTINGS_MEMORY_ADDRESS) {
                        this.moduleSettings = data[i];
                        OnOffType state = ((this.moduleSettings & 0x80) != 0x00) ? OnOffType.ON : OnOffType.OFF;
                        updateState(SCREENSAVER_CHANNEL, state);
                    }
                }
            }
        }
    }
}
