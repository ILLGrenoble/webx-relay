package eu.ill.webx.services;

import eu.ill.webx.model.Credentials;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class AuthService {

    private static final AuthService instance = new AuthService();

    private Map<String, Credentials> authorisations = new HashMap<>();
    private Thread thread = null;

    private AuthService() {
    }

    public static AuthService instance() {
        return instance;
    }

    public void start() {
        this.thread = new Thread(this::loop);
        this.thread.start();
    }

    public synchronized String addAuthorisation(String credentials) {
        String token = UUID.randomUUID().toString().replace("-", "");;
        this.authorisations.put(token, new Credentials(credentials));

        return token;
    }

    public synchronized Credentials getCredentials(String token) {
        if (this.authorisations.containsKey(token)) {
            return this.authorisations.remove(token);
        }

        return new Credentials(null);
    }

    public void loop() {
        while (true) {
            synchronized (this) {
                long currentTime = System.currentTimeMillis();
                for (Iterator<Map.Entry<String, Credentials>> it = authorisations.entrySet().iterator(); it.hasNext();) {
                    Map.Entry<String, Credentials> entry = it.next();
                    Credentials credentials = entry.getValue();
                    long expiration = credentials.getExpiration().getTime();

                    if (expiration <= currentTime) {
                        it.remove();
                    }
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }

        }
    }
}
