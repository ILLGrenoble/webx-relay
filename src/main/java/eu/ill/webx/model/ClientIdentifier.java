package eu.ill.webx.model;


public record ClientIdentifier(long clientIndex, int clientId) {

    public String clientIdString() {
        return String.format("%08x", clientId);
    }

    public String clientIndexString() {
        return String.format("%016d", clientIndex);
    }
}
