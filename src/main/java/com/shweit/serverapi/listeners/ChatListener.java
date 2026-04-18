package com.shweit.serverapi.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

@SuppressWarnings("deprecation") // AsyncPlayerChatEvent has no Spigot replacement; Paper-only AsyncChatEvent is not usable here
public final class ChatListener implements Listener {
    private static final int MAX_MESSAGES = 1000;
    private final Deque<HashMap<String, String>> messages = new ConcurrentLinkedDeque<>();

    @EventHandler
    public void onPlayerChat(final org.bukkit.event.player.AsyncPlayerChatEvent event) {
        HashMap<String, String> message = new HashMap<>();
        message.put("player", event.getPlayer().getName());
        message.put("message", event.getMessage());

        String readableTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date());
        message.put("time", readableTime);
        messages.addLast(message);
        while (messages.size() > MAX_MESSAGES) {
            messages.pollFirst();
        }
    }

    public List<HashMap<String, String>> getMessages() {
        return new ArrayList<>(messages);
    }
}
