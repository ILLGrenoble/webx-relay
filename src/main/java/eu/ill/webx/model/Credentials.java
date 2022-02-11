package eu.ill.webx.model;

import java.util.Date;

public class Credentials {

    private final static int lifetimeMs = 2000;

    private final String credentials;
    private final String username;
    private final String password;
    private final Date expiration;

    public Credentials(final String credentials) {
        this.credentials = credentials;

        if (credentials != null) {
            String[] parts = credentials.split(":");
            if (parts.length == 2) {
                this.username = parts[0];
                this.password = parts[1];

            } else {
                this.username = null;
                this.password = null;
            }

        } else {
            this.username = null;
            this.password = null;
        }

        this.expiration = new Date(System.currentTimeMillis() + lifetimeMs);
    }

    public String getCredentials() {
        return credentials;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Date getExpiration() {
        return expiration;
    }

    public boolean isValid() {
        return this.username != null && !this.username.isEmpty() && this.password != null && !this.password.isEmpty();
    }

}
