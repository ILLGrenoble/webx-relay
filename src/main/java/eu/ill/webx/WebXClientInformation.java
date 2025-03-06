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

public class WebXClientInformation {

    private final String username;
    private final String password;
    private final String sessionId;
    private Integer screenWidth = 1440;
    private Integer screenHeight = 900;
    private String keyboardLayout = "gb";

    public WebXClientInformation(String sessionId) {
        this.username = null;
        this.password = null;
        this.sessionId = sessionId;
    }

    public WebXClientInformation(String username, String password) {
        this.username = username;
        this.password = password;
        this.sessionId = null;
    }

    public WebXClientInformation(String username, String password, Integer screenWidth, Integer screenHeight) {
        this.username = username;
        this.password = password;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.sessionId = null;
    }

    public WebXClientInformation(String username, String password, Integer screenWidth, Integer screenHeight, String keyboardLayout) {
        this.username = username;
        this.password = password;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.keyboardLayout = keyboardLayout;
        this.sessionId = null;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Integer getScreenWidth() {
        return screenWidth;
    }

    public void setScreenWidth(Integer screenWidth) {
        this.screenWidth = screenWidth;
    }

    public Integer getScreenHeight() {
        return screenHeight;
    }

    public void setScreenHeight(Integer screenHeight) {
        this.screenHeight = screenHeight;
    }

    public String getKeyboardLayout() {
        return keyboardLayout;
    }

    public void setKeyboardLayout(String keyboardLayout) {
        this.keyboardLayout = keyboardLayout;
    }
}
