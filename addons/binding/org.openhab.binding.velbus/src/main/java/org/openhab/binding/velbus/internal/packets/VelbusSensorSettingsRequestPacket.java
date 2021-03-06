/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.velbus.internal.packets;

import static org.openhab.binding.velbus.internal.VelbusBindingConstants.COMMAND_TEMP_SENSOR_SETTINGS_REQUEST;

/**
 * The {@link VelbusSensorSettingsRequestPacket} represents a Velbus packet that can be used to
 * request the sensor settings of the given Velbus module.
 *
 * @author Cedric Boon - Initial contribution
 */
public class VelbusSensorSettingsRequestPacket extends VelbusPacket {
    public VelbusSensorSettingsRequestPacket(byte address) {
        super(address, PRIO_LOW);
    }

    @Override
    protected byte[] getDataBytes() {
        return new byte[] { COMMAND_TEMP_SENSOR_SETTINGS_REQUEST, 0x00 };
    }

}
