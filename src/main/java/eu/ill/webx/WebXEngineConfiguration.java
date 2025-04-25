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
package eu.ill.webx;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides engine configuration.
 * Used for setting environment variables when creating a new WebX Engine via the WebX Router
 */
public class WebXEngineConfiguration {

    private final Map<String, String> parameters = new HashMap<>();

    /**
     * Default constructor
     */
    public WebXEngineConfiguration() {
    }

    /**
     * Sets a parameter for the WebX Engine.
     * @param name the name of the parameter
     * @param value the value of the parameter
     */
    public void setParameter(String name, String value) {
        this.parameters.put(name, value);
    }

    /**
     * Converts the parameters to a connection string.
     * @return the connection string
     */
    public String connectionString() {
        return parameters.entrySet()
                         .stream()
                         .map(entry -> entry.getKey() + "=" + entry.getValue())
                         .reduce((param1, param2) -> param1 + "," + param2)
                         .orElse("");
    }
}
