/*
 * OpenGr8on, open source extensions to systems based on Grenton devices
 * Copyright (C) 2023 Piotr Sobiech
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.psobiech.opengr8on.vclu;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.Inet4Address;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.luaj.vm2.LuaValue.valueOf;

public class VirtualCLU extends VirtualObject {
    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualCLU.class);

    private final static Map<Integer, ZoneId> TIME_ZONES = Map.ofEntries(
        Map.entry(0, ZoneId.of("Europe/Warsaw")),
        Map.entry(1, ZoneId.of("Europe/London")),
        Map.entry(2, ZoneId.of("Europe/Moscow")),
        Map.entry(3, ZoneId.of("Europe/Istanbul")),
        Map.entry(4, ZoneId.of("Europe/Athens")),
        Map.entry(5, ZoneId.of("Asia/Dubai")),
        Map.entry(6, ZoneId.of("Asia/Jakarta")),
        Map.entry(7, ZoneId.of("Asia/Hong_Kong")),
        Map.entry(8, ZoneId.of("Australia/Sydney")),
        Map.entry(9, ZoneId.of("Australia/Perth")),
        Map.entry(10, ZoneId.of("Australia/Brisbane")),
        Map.entry(11, ZoneId.of("Pacific/Auckland")),
        Map.entry(12, ZoneId.of("Pacific/Honolulu")),
        Map.entry(13, ZoneId.of("America/Anchorage")),
        Map.entry(14, ZoneId.of("America/Chicago")),
        Map.entry(15, ZoneId.of("America/New_York")),
        Map.entry(16, ZoneId.of("America/Barbados")),
        Map.entry(17, ZoneId.of("America/Sao_Paulo")),
        Map.entry(18, ZoneId.of("America/Bogota")),
        Map.entry(19, ZoneId.of("America/Buenos_Aires")),
        Map.entry(20, ZoneId.of("America/Chicago")),
        Map.entry(21, ZoneId.of("America/Los_Angeles")),
        Map.entry(22, ZoneOffset.UTC)
    );

    private final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

    public VirtualCLU(String name, Inet4Address address) {
        super(name);

        funcs.put(
            0,
            arg -> {
                VirtualCLU.this.vars.put(1, arg);

                if (!arg.isnil()) {
                    final String logValue = String.valueOf(arg.checkstring());
                    LOGGER.info(VirtualCLU.this.name + ": " + logValue);
                }

                return LuaValue.NIL;
            }
        );

        funcs.put(
            1,
            arg -> {
                VirtualCLU.this.vars.put(1, LuaValue.NIL);

                return LuaValue.NIL;
            }
        );

        features.put(
            0,
            () -> valueOf(
                TimeUnit.MILLISECONDS.toSeconds(
                    runtimeBean.getUptime()
                )
            )
        );

        VirtualCLU.this.vars.put(2, valueOf(1));

        features.put(
            5,
            () -> valueOf(
                getCurrentDateTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            )
        );

        features.put(
            6,
            () -> valueOf(
                getCurrentDateTime()
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            )
        );

        features.put(
            7,
            () -> valueOf(
                getCurrentDateTime()
                    .getDayOfMonth()
            )
        );

        features.put(
            8,
            () -> valueOf(
                getCurrentDateTime()
                    .getMonthValue()
            )
        );

        features.put(
            9,
            () -> valueOf(
                getCurrentDateTime()
                    .getYear()
            )
        );

        features.put(
            10,
            () -> valueOf(
                getCurrentDateTime()
                    .getDayOfWeek()
                    .getValue()
            )
        );

        features.put(
            11,
            () -> valueOf(
                getCurrentDateTime()
                    .getHour()
            )
        );

        features.put(
            12,
            () -> valueOf(
                getCurrentDateTime()
                    .getMinute()
            )
        );

        features.put(
            13,
            () -> valueOf(
                Instant.now().getEpochSecond()
            )
        );
    }

    private ZonedDateTime getCurrentDateTime() {
        final ZoneId zoneId = getCurrentZoneId();

        return ZonedDateTime.now()
                            .withZoneSameInstant(zoneId);
    }

    private ZoneId getCurrentZoneId() {
        final LuaValue zoneIdLuaValue = VirtualCLU.this.vars.get(14);
        if (!zoneIdLuaValue.isint()) {
            return ZoneOffset.UTC;
        }

        return TIME_ZONES.getOrDefault(zoneIdLuaValue.checkint(), ZoneOffset.UTC);
    }
}
