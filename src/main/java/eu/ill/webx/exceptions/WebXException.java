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
package eu.ill.webx.exceptions;

/**
 * General WebX Exception
 */
public class WebXException extends Exception {

    /**
     * Default constructor
     */
    public WebXException() {
    }

    /**
     * Constructor with message
     * @param message the error message
     */
    public WebXException(String message) {
        super(message);
    }

    /**
     * Constructor with message and cause
     * @param message the error message
     * @param cause the cause of the error
     */
    public WebXException(String message, Throwable cause) {
        super(message, cause);
    }
}
