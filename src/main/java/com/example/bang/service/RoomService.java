package com.example.bang.service;

import com.example.bang.model.PlayerInfo;
import com.example.bang.model.Room;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToPlayer = new ConcurrentHashMap<>();

    public Room createRoom(String roomName, String hostSessionId, String hostName) {
        String roomId = generateRoomId();
        String playerId = UUID.randomUUID().toString();

        PlayerInfo host = PlayerInfo.builder()
                .id(playerId)
                .sessionId(hostSessionId)
                .name(hostName)
                .ready(true)
                .isHost(true)
                .build();

        Room room = Room.builder()
                .id(roomId)
                .name(roomName)
                .hostId(playerId)
                .build();
        room.addPlayer(host);

        rooms.put(roomId, room);
        sessionToRoom.put(hostSessionId, roomId);
        sessionToPlayer.put(hostSessionId, playerId);

        return room;
    }

    public Room joinRoom(String roomId, String sessionId, String playerName) {
        Room room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }
        if (room.isFull()) {
            throw new IllegalStateException("Room is full");
        }
        if (room.isGameStarted()) {
            throw new IllegalStateException("Game already started");
        }

        String playerId = UUID.randomUUID().toString();
        PlayerInfo player = PlayerInfo.builder()
                .id(playerId)
                .sessionId(sessionId)
                .name(playerName)
                .ready(false)
                .isHost(false)
                .build();

        room.addPlayer(player);
        sessionToRoom.put(sessionId, roomId);
        sessionToPlayer.put(sessionId, playerId);

        return room;
    }

    public Room leaveRoom(String sessionId) {
        String roomId = sessionToRoom.get(sessionId);
        if (roomId == null) return null;

        Room room = rooms.get(roomId);
        if (room == null) return null;

        String playerId = sessionToPlayer.get(sessionId);
        room.removePlayer(playerId);

        sessionToRoom.remove(sessionId);
        sessionToPlayer.remove(sessionId);

        if (room.getPlayers().isEmpty()) {
            rooms.remove(roomId);
            return null;
        }

        // Transfer host if needed
        if (playerId.equals(room.getHostId()) && !room.getPlayers().isEmpty()) {
            PlayerInfo newHost = room.getPlayers().get(0);
            room.setHostId(newHost.getId());
            newHost.setHost(true);
        }

        return room;
    }

    public void handleDisconnect(String sessionId) {
        String roomId = sessionToRoom.get(sessionId);
        if (roomId == null) return;

        String playerId = sessionToPlayer.get(sessionId);
        
        Room room = rooms.get(roomId);
        if (room != null && playerId != null) {
            PlayerInfo player = room.getPlayer(playerId);
            if (player != null) {
                player.setSessionId(null);
            }
        }

        sessionToRoom.remove(sessionId);
        sessionToPlayer.remove(sessionId);
    }

    public Optional<Room> getRoom(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    public String getRoomIdForSession(String sessionId) {
        return sessionToRoom.get(sessionId);
    }

    public String getPlayerIdForSession(String sessionId) {
        return sessionToPlayer.get(sessionId);
    }

    public void setPlayerReady(String sessionId, boolean ready) {
        String roomId = sessionToRoom.get(sessionId);
        String playerId = sessionToPlayer.get(sessionId);
        if (roomId == null || playerId == null) return;

        Room room = rooms.get(roomId);
        if (room == null) return;

        PlayerInfo player = room.getPlayer(playerId);
        if (player != null) {
            player.setReady(ready);
        }
    }

    public boolean canStartGame(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) return false;
        
        if (!room.canStart()) return false;
        
        return room.getPlayers().stream().allMatch(PlayerInfo::isReady);
    }

    public void markGameStarted(String roomId) {
        Room room = rooms.get(roomId);
        if (room != null) {
            room.setGameStarted(true);
        }
    }

    public Room reconnect(String roomId, String playerId, String newSessionId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }

        PlayerInfo player = room.getPlayer(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Player not found in room");
        }

        // Remove old session mappings
        String oldSessionId = player.getSessionId();
        if (oldSessionId != null) {
            sessionToRoom.remove(oldSessionId);
            sessionToPlayer.remove(oldSessionId);
        }

        // Update player session
        player.setSessionId(newSessionId);

        // Update mappings
        sessionToRoom.put(newSessionId, roomId);
        sessionToPlayer.put(newSessionId, playerId);

        return room;
    }

    private String generateRoomId() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }
}
