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

/**
 * Encapsulates the binary data of a ZMQ REQ-REP socket response
 * @param data the raw binary data
 */
public record SocketResponse(byte[] data) {

    /**
     * Converts the data to string if not null
     * @return the string value of the data if not null
     */
    public String toString() {
        return this.data == null ? null : new String(this.data);
    }
}
