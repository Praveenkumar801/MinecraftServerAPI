package com.shweit.serverapi.handlers;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A custom CommandSender implementation that captures command output
 */
public class CommandOutputCapture implements ConsoleCommandSender {
    private final ConsoleCommandSender originalSender;
    private final List<HashMap<String, String>> outputMessages = new ArrayList<>();

    public CommandOutputCapture() {
        this.originalSender = Bukkit.getConsoleSender();
    }

    @Override
    public void sendMessage(String message) {
        // Capture the message
        if (message != null && !message.isEmpty()) {
            HashMap<String, String> outputMessage = new HashMap<>();
            outputMessage.put("message", message);
            String readableTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date());
            outputMessage.put("time", readableTime);
            outputMessages.add(outputMessage);
        }
        
        // Forward to the original sender
        originalSender.sendMessage(message);
    }

    @Override
    public void sendMessage(String[] messages) {
        for (String message : messages) {
            sendMessage(message);
        }
    }

    @Override
    public void sendMessage(UUID sender, String message) {
        HashMap<String, String> outputMessage = new HashMap<>();
        outputMessage.put("message", message);
        outputMessage.put("sender", sender.toString());
        String readableTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date());
        outputMessage.put("time", readableTime);
        outputMessages.add(outputMessage);
        
        originalSender.sendMessage(sender, message);
    }

    @Override
    public void sendMessage(UUID sender, String[] messages) {
        for (String message : messages) {
            sendMessage(sender, message);
        }
    }

    public List<HashMap<String, String>> getOutputMessages() {
        return outputMessages;
    }

    // Delegate all other methods to the original sender
    @Override
    public Server getServer() {
        return originalSender.getServer();
    }

    @Override
    public String getName() {
        return originalSender.getName();
    }

    @Override
    public Spigot spigot() {
        return originalSender.spigot();
    }

    @Override
    public boolean isConversing() {
        return originalSender.isConversing();
    }

    @Override
    public void acceptConversationInput(String input) {
        originalSender.acceptConversationInput(input);
    }

    @Override
    public boolean beginConversation(Conversation conversation) {
        return originalSender.beginConversation(conversation);
    }

    @Override
    public void abandonConversation(Conversation conversation) {
        originalSender.abandonConversation(conversation);
    }

    @Override
    public void abandonConversation(Conversation conversation, ConversationAbandonedEvent details) {
        originalSender.abandonConversation(conversation, details);
    }

    @Override
    public void sendRawMessage(String message) {
        sendMessage(message);
    }

    @Override
    public void sendRawMessage(UUID sender, String message) {
        sendMessage(sender, message);
    }

    @Override
    public boolean isPermissionSet(String name) {
        return originalSender.isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
        return originalSender.isPermissionSet(perm);
    }

    @Override
    public boolean hasPermission(String name) {
        return originalSender.hasPermission(name);
    }

    @Override
    public boolean hasPermission(Permission perm) {
        return originalSender.hasPermission(perm);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
        return originalSender.addAttachment(plugin, name, value);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        return originalSender.addAttachment(plugin);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
        return originalSender.addAttachment(plugin, name, value, ticks);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
        return originalSender.addAttachment(plugin, ticks);
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {
        originalSender.removeAttachment(attachment);
    }

    @Override
    public void recalculatePermissions() {
        originalSender.recalculatePermissions();
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return originalSender.getEffectivePermissions();
    }

    @Override
    public boolean isOp() {
        return originalSender.isOp();
    }

    @Override
    public void setOp(boolean value) {
        originalSender.setOp(value);
    }
}