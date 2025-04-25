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

import java.util.Base64;

/**
 * Provides client connection configuration.
 * Used for requesting a new session (login) or connection to a running one (sessionId)
 * Standalone session passes directly to a standalone WebX Engine
 */
public class WebXClientConfiguration {

    private final String username;
    private final String password;
    private final String sessionId;
    private final Integer screenWidth;
    private final Integer screenHeight;
    private final String keyboardLayout;

    /**
     * Returns a configuration containing login details, screen size and keyboard layout. This will result in a new session being created by the WebX Router (if there isn't
     * one already running for the user).
     * @param username The username
     * @param password The password
     * @param screenWidth Desired screen width for a new X11 session
     * @param screenHeight Desired screen height for a new X11 session
     * @param keyboardLayout The requested keyboard layout
     * @return The WebXClientConfiguration
     */
    public static WebXClientConfiguration ForLogin(final String username, final String password, final Integer screenWidth, final Integer screenHeight, final String keyboardLayout) {
        return new WebXClientConfiguration(username, password, screenWidth, screenHeight, keyboardLayout);
    }

    /**
     * Returns a configuration containing login details, screen size. Keyboard layout is fixed at "gb". This will result in a new session being created by the WebX Router (if there isn't
     * one already running for the user).
     * @param username The username
     * @param password The password
     * @param screenWidth Desired screen width for a new X11 session
     * @param screenHeight Desired screen height for a new X11 session
     * @return The WebXClientConfiguration
     */
    public static WebXClientConfiguration ForLogin(final String username, final String password, final Integer screenWidth, final Integer screenHeight) {
        return new WebXClientConfiguration(username, password, screenWidth, screenHeight, "gb");
    }

    /**
     * Returns a configuration containing login details default screen size (1920x1024) and keyboard layout ("gb") are used. This will result in a new session being created by the WebX Router (if there isn't
     * one already running for the user).
     * @param username The username
     * @param password The password
     * @return The WebXClientConfiguration
     */
    public static WebXClientConfiguration ForLogin(final String username, final String password) {
        return new WebXClientConfiguration(username, password, 1920, 1024, "gb");
    }

    /**
     * Returns a configuration containing a sessionId. The WebX Router will attempt to connect to an existing session with an identical Id.
     * @param sessionId Session Id of a running session.
     * @return The WebXClientConfiguration
     */
    public static WebXClientConfiguration ForExistingSession(final String sessionId) {
        return new WebXClientConfiguration(sessionId);
    }

    /**
     * Returns a configuration for a standalone session.
     * @return The WebXClientConfiguration
     */
    public static WebXClientConfiguration ForStandaloneSession() {
        return new WebXClientConfiguration("00000000000000000000000000000000");
    }

    /**
     * Private constructor containing login details, screen size and keyboard layout. This will result in a new session being created by the WebX Router (if there isn't
     * one already running for the user).
     * @param username The username
     * @param password The password
     * @param screenWidth Desired screen width for a new X11 session
     * @param screenHeight Desired screen height for a new X11 session
     * @param keyboardLayout The requested keyboard layout
     */
    private WebXClientConfiguration(final String username, final String password, final Integer screenWidth, final Integer screenHeight, final String keyboardLayout) {
        this.username = username;
        this.password = password;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.keyboardLayout = keyboardLayout;
        this.sessionId = null;
    }

    /**
     * Private constructor containing a sessionId. The WebX Router will attempt to connect to an existing session with an identical Id.
     * @param sessionId Session Id of a running session.
     */
    private WebXClientConfiguration(final String sessionId) {
        this.username = null;
        this.password = null;
        this.screenWidth = null;
        this.screenHeight = null;
        this.keyboardLayout = null;
        this.sessionId = sessionId;
    }

    /**
     * Returns the username
     * @return The username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the password
     * @return The password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Returns the screen width
     * @return The screen width
     */
    public Integer getScreenWidth() {
        return screenWidth;
    }

    /**
     * Returns the screen height
     * @return The screen height
     */
    public Integer getScreenHeight() {
        return screenHeight;
    }

    /**
     * Returns the keyboard layout
     * @return The keyboard layout
     */
    public String getKeyboardLayout() {
        return keyboardLayout;
    }

    /**
     * Returns the session Id
     * @return The session Id
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the connection string for the client
     * @return The connection string
     */
    public String connectionString() {
        final String username = this.username == null ? "" : this.username;
        final String password = this.password == null ? "" : this.password;

        String usernameBase64 = Base64.getEncoder().encodeToString(username.getBytes());
        String passwordBase64 = Base64.getEncoder().encodeToString(password.getBytes());
        return String.format("%s,%s,%d,%d,%s", usernameBase64, passwordBase64, screenWidth, screenHeight, keyboardLayout);
    }
}
