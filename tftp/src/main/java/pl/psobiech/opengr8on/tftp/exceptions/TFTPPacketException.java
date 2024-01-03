/*
 * OpenGr8on, open source extensions to systems based on Grenton devices
 * Copyright (C) 2023 Piotr Sobiech
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.psobiech.opengr8on.tftp.exceptions;

import java.io.IOException;
import java.net.InetAddress;

import pl.psobiech.opengr8on.tftp.packets.TFTPErrorPacket;

public class TFTPPacketException extends Exception {
    private final int error;

    public TFTPPacketException(TFTPErrorPacket errorPacket) {
        this(errorPacket.getError(), errorPacket.getMessage());
    }

    public TFTPPacketException(int error, IOException exception) {
        super(exception);

        this.error = error;
    }

    public TFTPPacketException(int error, String message) {
        super(message);

        this.error = error;
    }

    public TFTPPacketException(int error, String message, Throwable throwable) {
        super(message, throwable);

        this.error = error;
    }

    public TFTPPacketException(String message) {
        super(message);

        error = TFTPErrorPacket.UNDEFINED;
    }

    public TFTPErrorPacket asError(InetAddress address, int port) {
        return new TFTPErrorPacket(
            address, port,
            getError(), getMessage()
        );
    }

    public int getError() {
        return error;
    }
}
