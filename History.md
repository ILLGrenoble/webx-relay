1.8.1 07/07/2025
================
 * Test for empty response from the status request. Assume session running if empty.

1.8.0 07/07/2025
================
 * Perform asynchronous creation commands (Backward compatible with older routers)
 * Add create_async call to router that returns after Xorg has been spawned
 * Poll status of router to determine when session is fully available
 * Send Connection messages with running status of session. 

1.7.0 30/06/2025
================
 * Update readme
 * Test for incorrect params when creating a webx session
 * Manage multiple response codes from the WebX Router when creating a session.

1.6.0 12/06/2025
================
 * Improve handling of session errors (ping fails): Ensure that the pinging stops, all clients are disconnected and that the session is removed from the host (disconnecting the host if it is the last session).
 * Change all logged errors to warning.

1.5.1 07/05/2025
================
 * Fix client connection with legacy router when sending webx-client version.

1.5.0 07/05/2025
================
 * Send the webx-client version in the ClientConfiguration. Usable in the future for server-client compatibility testing.

1.4.0 06/05/2025
================
 * Provide public access to WebX message header lengths and NOP message data.

1.3.1 25/04/2025
================
 * Fix log message.

1.3.0 25/04/2025
================
 * Add optional EngineConfiguration parameters. These parameters are sent to the WebXRouter and converted into environment variables for the spawned WebX Engine.

1.2.0 23/04/2025
================
 * Send a connection message to the client when connection is successful.

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

0.6.0 12/03/2025
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
