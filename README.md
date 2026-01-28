# BANG! - Multiplayer Card Game

A complete multiplayer implementation of the classic BANG! Wild West card game with a Java Spring Boot backend and Vue 3 frontend.

## Tech Stack

### Backend
- **Java 21** with **Spring Boot 3.2.x**
- **Spring WebSocket** (STOMP/SockJS) for real-time communication
- **Lombok** for reduced boilerplate
- **Maven** for dependency management

### Frontend
- **Vue 3** with Composition API
- **Vite** for fast development
- **Tailwind CSS** for styling
- **Pinia** for state management
- **GSAP** for animations
- **Howler.js** for sound effects

## Features

- **Lobby System**: Create/join rooms with 4-7 players
- **Real-time Gameplay**: WebSocket-based synchronization
- **Complete Game Logic**: All phases (Draw, Play, Discard, Reaction)
- **16 Character Abilities**: Bart Cassidy, Willy the Kid, Calamity Janet, etc.
- **80-Card Deck**: Full implementation of all card types
- **Animated Actions**: GSAP-powered card animations
- **Distance Calculation**: Mustang, Scope, and weapon ranges
- **Win Conditions**: Sheriff/Deputies vs Outlaws vs Renegade

## Getting Started

### Prerequisites
- Java 21+
- Node.js 18+
- Maven 3.8+

### Running the Backend

```bash
# From the project root
./mvnw spring-boot:run
```

The backend will start on `http://localhost:8080`

### Running the Frontend

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

The frontend will start on `http://localhost:5173`

## Game Rules

### Roles
- **Sheriff** (1): Eliminate all Outlaws and the Renegade
- **Deputy** (1-2): Protect the Sheriff
- **Outlaw** (2-3): Kill the Sheriff
- **Renegade** (1): Be the last one standing

### Phases
1. **Draw Phase**: Draw 2 cards from the deck
2. **Play Phase**: Play cards (max 1 BANG! per turn unless special ability)
3. **Discard Phase**: Discard down to your current HP

### Key Cards
- **BANG!**: Shoot another player within range
- **Missed!**: Dodge a BANG!
- **Beer**: Heal 1 HP (not in 2-player endgame)
- **Weapons**: Increase your shooting range
- **Mustang**: Others see you at +1 distance
- **Scope**: You see others at -1 distance

## Project Structure

```
BANG/
├── src/main/java/com/example/bang/
│   ├── config/          # WebSocket & CORS configuration
│   ├── controller/      # WebSocket message handlers
│   ├── dto/             # Data transfer objects
│   ├── model/           # Domain models (Card, Player, etc.)
│   └── service/         # Game logic services
├── frontend/
│   ├── src/
│   │   ├── components/  # Vue components
│   │   ├── composables/ # Vue composables
│   │   ├── stores/      # Pinia stores
│   │   ├── views/       # Page components
│   │   └── router/      # Vue Router config
│   └── public/
│       └── sounds/      # Sound effect files
└── pom.xml
```

## Sound Files

Add the following sound files to `frontend/public/sounds/`:
- `gunshot.mp3` - BANG! card sound
- `play_card.mp3` - Generic card play
- `draw.mp3` - Drawing cards
- `death.mp3` - Player elimination
- `damage.mp3` - Taking damage
- `click.mp3` - UI clicks

## License

This is a fan-made implementation for educational purposes.
BANG! is a trademark of dV Giochi.
