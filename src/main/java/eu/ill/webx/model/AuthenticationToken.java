package eu.ill.webx.model;

public class AuthenticationToken {

    private String token;

    public AuthenticationToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
