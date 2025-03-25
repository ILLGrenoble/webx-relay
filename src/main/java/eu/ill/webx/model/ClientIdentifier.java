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
 * Encapsulates data representing a unique client in a WebX Engine
 * @param clientIndex The unique index of a client (used to filter messages)
 * @param clientId The unique Id of a client (used to identify instructions to the WebX Engine)
 */
public record ClientIdentifier(long clientIndex, int clientId) {

    /**
     * Returns a hex representation of the client Id
     * @return a hex representation of the client Id
     */
    public String clientIdString() {
        return String.format("%08x", clientId);
    }

    /**
     * Returns the hex representation of the client Index
     * @return the hex representation of the client Index
     */
    public String clientIndexString() {
        return String.format("%016d", clientIndex);
    }
}
