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
