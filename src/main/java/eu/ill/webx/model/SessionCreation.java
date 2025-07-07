package eu.ill.webx.model;

/**
 * The SessionCreation is used as a means of storing the response from a creation command. The creation
 * command may be asynchronous and the session creation may take a few seconds.
 * @param sessionId The id of the session
 * @param status The creation status
 */
public record SessionCreation(SessionId sessionId, CreationStatus status) {

    /**
     * Represents the state of the session creation in the WebX Router
     */
    public enum CreationStatus {
        STARTING,
        RUNNING,
        UNKNOWN;

        /**
         * Converts an integer value from the WebX Router into a status
         * @param x the raw response from the server
         * @return a CreationStatus object
         */
        public static CreationStatus fromInteger(int x) {
            return switch (x) {
                case 0 -> STARTING;
                case 1 -> RUNNING;
                default -> UNKNOWN;
            };
        }
    }

}
