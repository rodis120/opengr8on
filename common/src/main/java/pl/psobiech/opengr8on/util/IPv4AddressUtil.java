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

package pl.psobiech.opengr8on.util;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.psobiech.opengr8on.exceptions.UnexpectedException;

public final class IPv4AddressUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(IPv4AddressUtil.class);

    private static final int PING_TIMEOUT = 2000;

    public static final Inet4Address BROADCAST_ADDRESS = parseIPv4("255.255.255.255");

    public static final Set<Inet4Address> DEFAULT_BROADCAST_ADDRESSES = Set.of(
        BROADCAST_ADDRESS,
        parseIPv4("255.255.0.0"),
        parseIPv4("255.0.0.0")
    );

    public static final Set<String> HARDWARE_ADDRESS_PREFIX_BLACKLIST = Set.of(
        HexUtil.asString(0x000569), // VMware, Inc.
        HexUtil.asString(0x001c14), // VMware, Inc.
        HexUtil.asString(0x000c29), // VMware, Inc.
        HexUtil.asString(0x005056)  // VMware, Inc.
    );

    public static final Set<String> NETWORK_INTERFACE_NAME_PREFIX_BLACKLIST = Set.of(
        "vmnet", "vboxnet"
    );

    private IPv4AddressUtil() {
        // NOP
    }

    public static Optional<NetworkInterfaceDto> getLocalIPv4NetworkInterfaceByNameOrAddress(String nameOrAddress) {
        final List<NetworkInterfaceDto> networkInterfaces = getLocalIPv4NetworkInterfaces();
        for (NetworkInterfaceDto networkInterface : networkInterfaces) {
            if (Objects.equals(networkInterface.getNetworkInterface().getName(), nameOrAddress)) {
                return Optional.of(networkInterface);
            }

            if (Objects.equals(networkInterface.getAddress().getHostAddress(), nameOrAddress)) {
                return Optional.of(networkInterface);
            }
        }

        return Optional.empty();
    }

    public static Optional<NetworkInterfaceDto> getLocalIPv4NetworkInterfaceByName(String name) {
        final List<NetworkInterfaceDto> networkInterfaces = getLocalIPv4NetworkInterfaces();
        for (NetworkInterfaceDto networkInterface : networkInterfaces) {
            if (Objects.equals(networkInterface.getNetworkInterface().getName(), name)) {
                return Optional.of(networkInterface);
            }
        }

        return Optional.empty();
    }

    public static Optional<NetworkInterfaceDto> getLocalIPv4NetworkInterfaceByIpAddress(String ipAddress) {
        final List<NetworkInterfaceDto> networkInterfaces = getLocalIPv4NetworkInterfaces();
        for (NetworkInterfaceDto networkInterface : networkInterfaces) {
            if (Objects.equals(networkInterface.getAddress().getHostAddress(), ipAddress)) {
                return Optional.of(networkInterface);
            }
        }

        return Optional.empty();
    }

    public static List<NetworkInterfaceDto> getLocalIPv4NetworkInterfaces() {
        final List<NetworkInterface> validNetworkInterfaces = allValidNetworkInterfaces();
        final List<NetworkInterfaceDto> networkInterfaces = new ArrayList<>(validNetworkInterfaces.size());
        for (NetworkInterface networkInterface : validNetworkInterfaces) {
            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                final InetAddress address = interfaceAddress.getAddress();
                if (!(address instanceof Inet4Address)) {
                    continue;
                }

                final InetAddress broadcastAddress = interfaceAddress.getBroadcast();
                if (!(broadcastAddress instanceof Inet4Address)) {
                    continue;
                }

                if (!address.isSiteLocalAddress()) {
                    continue;
                }

                final int networkMask = getNetworkMaskFromPrefix(interfaceAddress.getNetworkPrefixLength());

                networkInterfaces.add(new NetworkInterfaceDto(
                    (Inet4Address) address, (Inet4Address) broadcastAddress,
                    networkMask,
                    networkInterface
                ));
            }
        }

        return networkInterfaces;
    }

    private static int getNetworkMaskFromPrefix(short networkPrefixLength) {
        int networkMask = 0x00;
        for (int i = 0; i < Integer.SIZE - networkPrefixLength; i++) {
            networkMask += (1 << i);
        }

        networkMask ^= 0xFFFFFFFF;

        return networkMask;
    }

    private static List<NetworkInterface> allValidNetworkInterfaces() {
        final List<NetworkInterface> networkInterfaces;
        try {
            networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        } catch (SocketException e) {
            throw new UnexpectedException(e);
        }

        final List<NetworkInterface> validNetworkInterfaces = new ArrayList<>(networkInterfaces.size());
        for (NetworkInterface networkInterface : networkInterfaces) {
            try {
                if (!networkInterface.isUp()
                    || networkInterface.isLoopback()
                    || networkInterface.isPointToPoint()
                    || isBlacklisted(networkInterface)
                ) {
                    continue;
                }
            } catch (SocketException e) {
                LOGGER.warn(e.getMessage(), e);

                continue;
            }

            validNetworkInterfaces.add(networkInterface);
        }

        return validNetworkInterfaces;
    }

    private static boolean isBlacklisted(NetworkInterface networkInterface) throws SocketException {
        final String networkInterfaceName = networkInterface.getName();
        for (String blacklistedNetworkInterfaceNamePrefix : NETWORK_INTERFACE_NAME_PREFIX_BLACKLIST) {
            if (networkInterfaceName.startsWith(blacklistedNetworkInterfaceNamePrefix)) {
                return true;
            }
        }

        final byte[] hardwareAddress = networkInterface.getHardwareAddress();
        if (hardwareAddress == null) {
            return true;
        }

        return isHardwareAddressBlacklisted(hardwareAddress);
    }

    private static boolean isHardwareAddressBlacklisted(byte[] macAddress) {
        if (macAddress == null) {
            return true;
        }

        final String hardwareAddressPrefix = HexUtil.asString(Arrays.copyOf(macAddress, 3));

        return HARDWARE_ADDRESS_PREFIX_BLACKLIST.contains(hardwareAddressPrefix);
    }

    public static Inet4Address parseIPv4(String ipv4AddressAsString) {
        try {
            return (Inet4Address) InetAddress.getByAddress(
                getIPv4AsBytes(ipv4AddressAsString)
            );
        } catch (UnknownHostException e) {
            throw new UnexpectedException(e);
        }
    }

    public static Inet4Address parseIPv4(int ipv4AddressAsNumber) {
        try {
            final byte[] buffer = asBytes(ipv4AddressAsNumber);

            return (Inet4Address) InetAddress.getByAddress(buffer);
        } catch (UnknownHostException e) {
            throw new UnexpectedException(e);
        }
    }

    public static String getIPv4FromNumber(int ipv4AsNumber) {
        final byte[] buffer = asBytes(ipv4AsNumber);

        return IntStream.range(0, Integer.BYTES)
                        .mapToObj(i -> String.valueOf(buffer[i] & 0xFF))
                        .collect(Collectors.joining("."));
    }

    private static byte[] asBytes(int ipv4AsNumber) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);

        byteBuffer.putInt(ipv4AsNumber);
        byteBuffer.flip();

        return byteBuffer.array();
    }

    public static int getIPv4AsNumber(Inet4Address inetAddress) {
        return getIPv4AsNumber(inetAddress.getAddress());
    }

    public static int getIPv4AsNumber(String ipv4AddressAsString) {
        return getIPv4AsNumber(getIPv4AsBytes(ipv4AddressAsString));
    }

    public static int getIPv4AsNumber(byte[] ipv4AddressAsBytes) {
        if (ipv4AddressAsBytes.length != Integer.BYTES) {
            throw new UnexpectedException("Invalid IPv4 address: " + Arrays.toString(ipv4AddressAsBytes));
        }

        return ByteBuffer.wrap(ipv4AddressAsBytes)
                         .getInt();
    }

    private static byte[] getIPv4AsBytes(String ipv4AddressAsString) {
        final String[] ipAddressParts = Util.splitAtLeast(ipv4AddressAsString, "\\.", Integer.BYTES)
                                            .orElseThrow(() -> new UnexpectedException("Invalid IPv4 address: " + ipv4AddressAsString));

        final byte[] addressAsBytes = new byte[Integer.BYTES];
        for (int i = 0; i < addressAsBytes.length; i++) {
            addressAsBytes[i] = (byte) Integer.parseInt(ipAddressParts[i]);
        }

        return addressAsBytes;
    }

    public static boolean ping(InetAddress inetAddress) {
        try {
            return inetAddress.isReachable(PING_TIMEOUT);
        } catch (IOException e) {
            LOGGER.warn(e.getMessage(), e);

            return false;
        }
    }

    public static final class NetworkInterfaceDto {
        private final Inet4Address address;

        private final Inet4Address broadcastAddress;

        private final int networkAddress;

        private final int networkMask;

        private final NetworkInterface networkInterface;

        public NetworkInterfaceDto(
            Inet4Address address, Inet4Address broadcastAddress,
            int networkMask,
            NetworkInterface networkInterface
        ) {
            this.address = address;

            final int addressAsNumber = IPv4AddressUtil.getIPv4AsNumber(address.getAddress());
            this.networkAddress = addressAsNumber & networkMask;

            assert IPv4AddressUtil.getIPv4AsNumber(broadcastAddress) == (networkAddress | (~networkMask));
            this.broadcastAddress = broadcastAddress;
            this.networkMask      = networkMask;

            this.networkInterface = networkInterface;
        }

        public Inet4Address getAddress() {
            return address;
        }

        public Inet4Address getNetworkAddress() {
            return parseIPv4(networkAddress);
        }

        public Inet4Address getBroadcastAddress() {
            return broadcastAddress;
        }

        public int getNetworkMask() {
            return networkMask;
        }

        public NetworkInterface getNetworkInterface() {
            return networkInterface;
        }

        public boolean valid(Inet4Address address) {
            final int ipAsNumber = getIPv4AsNumber(address);

            return ipAsNumber > networkAddress && ipAsNumber < IPv4AddressUtil.getIPv4AsNumber(broadcastAddress);
        }

        public Optional<Inet4Address> nextAvailable(
            Inet4Address startingAddress,
            Duration timeout,
            Inet4Address currentAddress,
            Collection<Inet4Address> exclusionList
        ) {
            final List<Inet4Address> addresses = nextAvailableExcluding(startingAddress, timeout, 1, currentAddress, exclusionList);
            if (addresses.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(addresses.getFirst());
        }

        public List<Inet4Address> nextAvailableExcluding(
            Inet4Address startingAddress,
            Duration timeout,
            int limit,
            Inet4Address currentAddress,
            Collection<Inet4Address> exclusionList
        ) {
            final Set<Integer> excludedIpAddressNumbers = exclusionList.stream()
                                                                       .map(IPv4AddressUtil::getIPv4AsNumber)
                                                                       .collect(Collectors.toSet());

            final int currentIpAsNumber = getIPv4AsNumber(currentAddress);
            int ipAsNumber = getIPv4AsNumber(startingAddress);

            final List<Inet4Address> addresses = new ArrayList<>(limit);
            do {
                final long startedAt = System.nanoTime();

                final int addressAsNumber = ipAsNumber++;
                if (currentIpAsNumber == addressAsNumber) {
                    addresses.add(currentAddress);
                    continue;
                }

                if (excludedIpAddressNumbers.contains(addressAsNumber)) {
                    continue;
                }

                final Inet4Address address = parseIPv4(addressAsNumber);
                if (!ping(address)) {
                    addresses.add(address);
                }

                timeout = timeout.minusNanos(System.nanoTime() - startedAt);
            } while (timeout.isPositive() && addresses.size() < limit && !Thread.interrupted());

            return addresses;
        }

        @Override
        public String toString() {
            return "NetworkInterfaceDto{" +
                   "networkInterface=" + ToStringUtil.toString(networkInterface) +
                   ", address=" + ToStringUtil.toString(address) +
                   ", broadcastAddress=" + ToStringUtil.toString(broadcastAddress) +
                   ", networkAddress=" + ToStringUtil.toString(parseIPv4(networkAddress)) +
                   ", networkMask=" + ToStringUtil.toString(parseIPv4(networkMask)) +
                   '}';
        }
    }
}
