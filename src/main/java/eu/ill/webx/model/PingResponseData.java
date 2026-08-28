/*
 * WebX Relay
 * Copyright (C) 2023 Institut Laue-Langevin
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.ill.webx.model;

import java.util.Date;

/**
 * Hold data concerning WebX Engine ping responses
 * @param source the source of the Ping
 * @param rttMs the Ping Round-Trip Time in milliseconds
 * @param date the creation date of the ping response data
 */
public record PingResponseData(Source source, long rttMs, Date date) {

    /**
     * Constructs the Ping response data and sets the date to the current date
     * @param source the source of the Ping
     * @param rttMs the Ping Round-Trip Time in milliseconds
     */
    public PingResponseData(Source source, long rttMs) {
        this(source, rttMs, new Date());
    }

    /**
     * Specifies the source of the ping data (server or client)
     */
    public enum Source {
        /**
         * Specifies that the source of the ping data is the server
         */
        SERVER,

        /**
         * Specifies that the source of the ping data is the client
         */
        CLIENT,
    }
}
