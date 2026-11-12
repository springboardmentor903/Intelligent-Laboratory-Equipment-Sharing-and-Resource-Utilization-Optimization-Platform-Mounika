import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

/**
 * Creates and activates a managed STOMP client over WebSocket/SockJS
 * with auto-reconnection and resynchronization callbacks.
 */
export function connectEquipmentStatusWebSocket({ onMessage, onConnect, onDisconnect, onError }) {
  const isSecure = typeof window !== 'undefined' && window.location.protocol === 'https:';
  const wsProtocol = isSecure ? 'wss:' : 'ws:';
  const host = typeof window !== 'undefined' ? window.location.host : 'localhost:5173';
  const directWsUrl = `${wsProtocol}//${host}/ws-lab-direct`;

  const client = new Client({
    brokerURL: directWsUrl,
    // SockJS fallback factory if native WebSocket is unavailable
    webSocketFactory: () => new SockJS('/ws-lab'),
    reconnectDelay: 4000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: () => {
      // debug logs suppressed for production cleanliness
    },
    onConnect: (frame) => {
      if (onConnect) onConnect(frame);

      // Subscribe to equipment status broadcast topic
      client.subscribe('/topic/equipment-status', (message) => {
        try {
          const payload = JSON.parse(message.body);
          if (onMessage) onMessage(payload);
        } catch (e) {
          console.error('[WebSocket] Error parsing status update payload:', e);
        }
      });
    },
    onDisconnect: () => {
      if (onDisconnect) onDisconnect();
    },
    onStompError: (frame) => {
      console.error('[WebSocket] STOMP error:', frame?.headers?.['message']);
      if (onError) onError(frame);
    },
  });

  client.activate();

  return {
    disconnect: () => {
      try {
        client.deactivate();
      } catch (err) {
        console.warn('[WebSocket] Error during deactivation:', err);
      }
    },
  };
}
