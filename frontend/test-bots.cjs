/**
 * Bot Test Script - Simulates 4 bot players joining a lobby
 * 
 * Usage:
 *   1. Start the backend server (port 8080)
 *   2. Run: node test-bots.js <ROOM_CODE>
 *   
 * Example:
 *   node test-bots.js ABC123
 */

const SockJS = require('sockjs-client');
const Stomp = require('@stomp/stompjs');

// Polyfill for Node.js
Object.assign(global, { WebSocket: require('ws') });

const BACKEND_URL = 'http://localhost:8080/ws';
const BOT_NAMES = ['Bot_Jesse', 'Bot_Billy', 'Bot_Rose', 'Bot_Lucky'];

class BotClient {
  constructor(name, roomId) {
    this.name = name;
    this.roomId = roomId;
    this.client = null;
    this.playerId = null;
    this.connected = false;
  }

  connect() {
    return new Promise((resolve, reject) => {
      const socket = new SockJS(BACKEND_URL);
      this.client = Stomp.Stomp.over(socket);
      
      // Disable debug logging
      this.client.debug = () => {};

      this.client.connect({}, (frame) => {
        this.connected = true;
        console.log(`[${this.name}] Connected to server`);

        // Subscribe to lobby messages
        this.client.subscribe('/user/queue/lobby', (message) => {
          const data = JSON.parse(message.body);
          this.handleLobbyMessage(data);
        });

        // Subscribe to room messages after joining
        resolve();
      }, (error) => {
        console.error(`[${this.name}] Connection error:`, error);
        reject(error);
      });
    });
  }

  handleLobbyMessage(message) {
    console.log(`[${this.name}] Received: ${message.type}`);
    
    if (message.type === 'ROOM_JOINED') {
      this.playerId = message.playerId;
      console.log(`[${this.name}] Joined room, playerId: ${this.playerId}`);
      
      // Subscribe to room updates
      this.client.subscribe(`/topic/room/${this.roomId}`, (msg) => {
        const data = JSON.parse(msg.body);
        this.handleRoomMessage(data);
      });
      
      // Auto set ready after a short delay
      setTimeout(() => this.setReady(), 1000);
    }
    
    if (message.type === 'ERROR') {
      console.error(`[${this.name}] Error: ${message.payload}`);
    }
  }

  handleRoomMessage(message) {
    if (message.type === 'GAME_STARTED') {
      console.log(`[${this.name}] Game started!`);
    }
  }

  joinRoom() {
    console.log(`[${this.name}] Joining room: ${this.roomId}`);
    this.client.send('/app/room/join', {}, JSON.stringify({
      roomId: this.roomId,
      playerName: this.name
    }));
  }

  setReady() {
    console.log(`[${this.name}] Setting ready...`);
    this.client.send('/app/room/ready', {}, JSON.stringify({
      ready: true
    }));
  }

  disconnect() {
    if (this.client && this.connected) {
      this.client.disconnect();
      console.log(`[${this.name}] Disconnected`);
    }
  }
}

async function main() {
  const roomId = process.argv[2];
  
  if (!roomId) {
    console.log('');
    console.log('='.repeat(50));
    console.log('  BANG! Bot Test Script');
    console.log('='.repeat(50));
    console.log('');
    console.log('Usage: node test-bots.js <ROOM_CODE>');
    console.log('');
    console.log('Steps:');
    console.log('  1. Start the backend (mvnw spring-boot:run)');
    console.log('  2. Start the frontend (npm run dev)');
    console.log('  3. Create a room in the browser');
    console.log('  4. Copy the room code');
    console.log('  5. Run: node test-bots.js <ROOM_CODE>');
    console.log('');
    console.log('The bots will join the room and auto-ready.');
    console.log('Then click "Start Game" in the browser!');
    console.log('');
    process.exit(1);
  }

  console.log('');
  console.log('='.repeat(50));
  console.log(`  Connecting ${BOT_NAMES.length} bots to room: ${roomId}`);
  console.log('='.repeat(50));
  console.log('');

  const bots = BOT_NAMES.map(name => new BotClient(name, roomId));

  try {
    // Connect all bots
    for (const bot of bots) {
      await bot.connect();
      await new Promise(r => setTimeout(r, 500)); // Small delay between connections
    }

    // Join all bots to the room
    for (const bot of bots) {
      bot.joinRoom();
      await new Promise(r => setTimeout(r, 800)); // Small delay between joins
    }

    console.log('');
    console.log('All bots connected and ready!');
    console.log('Press Ctrl+C to disconnect bots.');
    console.log('');

    // Keep the script running
    process.on('SIGINT', () => {
      console.log('\nDisconnecting bots...');
      bots.forEach(bot => bot.disconnect());
      process.exit(0);
    });

  } catch (error) {
    console.error('Failed to connect bots:', error);
    bots.forEach(bot => bot.disconnect());
    process.exit(1);
  }
}

main();
