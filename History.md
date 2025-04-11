1.1.0 11/04/2025
================
 * Get message timestamp directly from message data
 * Log info for host disconnect

1.0.0 25/03/2025
================
 * Minor refactoring
 * Full code documentation

0.8.0 21/03/2025
================
 * Fix thread blocking issues during host disconnect and message being received at the same time.
 * Handle null socket response on ping (due to timeout)

0.7.0 21/03/2025
================
 * Code documentation
 * Send pings only for each session (remove host pinging): session pinging either directly to standalone engine or via the router.
 * Refactoring configuration classes
 * Move instruction thread from session to instruction publisher: single thread per host.
 * Single session ping thread per session. Single message listener for the subscriber (the host).
 * Refactoring code. Adding WebXSession. Don't create client until connection is fully made. 

0.6.0 12/03/2025[History.md](History.md)
================
 * Remove sending message queue size to client

0.5.0 12/03/2025
================
 * Update message header length (include timestamp on each message)

0.4.0 06/03/2025
================
 * Update dependencies and include only the slf4j logger api.

0.3.0 06/03/2025
================
 * Allow connections to existing sessions via a sessionId uniquely.

0.2.0 04/03/2025
================
 * Handle disconnect message from engine
 * Remove poll message (pings sent from engine)
 * Improve connection speed
 * Multi-client: filtering of messages by client index bitmask
 * Multi-client: Get client Id and index from engine
 * Multi-client: Send "connect" command when creating a new client
 * Improve performance of handling of session Id on messages and instructions
 * Add BSD license
 * Update README to cover design of relay, description of the WebX projects and how to develop the WebX Relay

0.1.0 17/02/2023
================
 * Initial release
