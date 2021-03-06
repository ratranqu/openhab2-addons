/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.velbus.internal.packets;

import org.openhab.binding.velbus.internal.VelbusChannelIdentifier;

/**
 * The {@link VelbusDimmerPacket} represents a Velbus packet that can be used to
 * dim a light to a given percentage.
 *
 * @author Cedric Boon - Initial contribution
 */
public class VelbusDimmerPacket extends VelbusPacket {
    private final byte FIRST_GENERATION_DEVICE_FASTEST_DIMSPEED_HIGH_BYTE = (byte) 0xFF;
    private final byte FIRST_GENERATION_DEVICE_FASTEST_DIMSPEED_LOW_BYTE = (byte) 0xFF;

    private Boolean isFirstGenerationDevice;
    private byte command;
    private byte channel;
    private byte percentage;
    private int dimspeed;

    public VelbusDimmerPacket(VelbusChannelIdentifier velbusChannelIdentifier, byte command, byte percentage,
            int dimspeed, Boolean isFirstGenerationDevice) {
        super(velbusChannelIdentifier.getAddress(), PRIO_HI);

        this.channel = velbusChannelIdentifier.getChannelByte();
        this.command = command;
        this.percentage = percentage;
        this.dimspeed = dimspeed;
        this.isFirstGenerationDevice = isFirstGenerationDevice;
    }

    @Override
    protected byte[] getDataBytes() {
        byte dimspeedHighByte = (byte) ((dimspeed & 0xFF00) / 0x100);
        byte dimspeedLowByte = (byte) (dimspeed & 0xFF);

        if (dimspeed == 0 && isFirstGenerationDevice) {
            dimspeedHighByte = FIRST_GENERATION_DEVICE_FASTEST_DIMSPEED_HIGH_BYTE;
            dimspeedLowByte = FIRST_GENERATION_DEVICE_FASTEST_DIMSPEED_LOW_BYTE;
        }

        return new byte[] { command, channel, percentage, dimspeedHighByte, dimspeedLowByte };
    }

}
