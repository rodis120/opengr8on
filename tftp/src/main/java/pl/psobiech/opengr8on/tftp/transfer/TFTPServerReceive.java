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

package pl.psobiech.opengr8on.tftp.transfer;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.psobiech.opengr8on.tftp.TFTP;
import pl.psobiech.opengr8on.tftp.exceptions.TFTPPacketException;
import pl.psobiech.opengr8on.tftp.packets.TFTPWriteRequestPacket;

public class TFTPServerReceive extends TFTPReceivingTransfer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TFTPServerReceive.class);

    private final TFTPWriteRequestPacket tftpPacket;

    private final Path path;

    public TFTPServerReceive(TFTPWriteRequestPacket tftpPacket, Path path) {
        this.tftpPacket = tftpPacket;

        this.path = path;
    }

    @Override
    public void execute(TFTP tftp) throws IOException, TFTPPacketException {
        final InetAddress requestAddress = tftpPacket.getAddress();
        final int requestPort = tftpPacket.getPort();

        incomingTransfer(tftp, true, tftpPacket.getMode(), requestAddress, requestPort, path);
    }
}
