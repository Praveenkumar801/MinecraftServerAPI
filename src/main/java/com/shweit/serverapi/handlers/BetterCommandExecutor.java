package com.shweit.serverapi.handlers;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Better command executor that provides informative output for commands
 */
public class BetterCommandExecutor {
    
    /**
     * Execute a command and capture its output
     */
    public static CommandResult executeCommand(String command) {
        CommandOutputCapture outputCapture = new CommandOutputCapture();
        boolean success = false;
        
        try {
            // Execute the command
            success = Bukkit.getServer().dispatchCommand(outputCapture, command);
            
            // Get captured output
            List<HashMap<String, String>> output = outputCapture.getOutputMessages();
            
            // If no output was captured, try to provide useful information based on the command
            if (output.isEmpty() && success) {
                String info = getCommandInfo(command);
                if (info != null) {
                    HashMap<String, String> msg = new HashMap<>();
                    msg.put("message", info);
                    msg.put("time", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date()));
                    output.add(msg);
                }
            }
            
            return new CommandResult(command, success, output);
            
        } catch (Exception e) {
            List<HashMap<String, String>> errorOutput = new ArrayList<>();
            HashMap<String, String> errorMsg = new HashMap<>();
            errorMsg.put("message", "Error executing command: " + e.getMessage());
            errorMsg.put("time", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date()));
            errorOutput.add(errorMsg);
            
            return new CommandResult(command, false, errorOutput);
        }
    }
    
    /**
     * Get informative output for commands that don't normally produce output
     */
    private static String getCommandInfo(String command) {
        String cmd = command.toLowerCase().trim();
        String[] parts = cmd.split("\\s+");
        
        if (parts.length == 0) {
            return null;
        }
        
        // Handle common Minecraft commands with custom messages
        switch (parts[0]) {
            case "time":
                if (parts.length >= 3 && parts[1].equals("set")) {
                    if (parts[2].equals("day")) {
                        return "Set the time to day (1000 ticks)";
                    } else if (parts[2].equals("night")) {
                        return "Set the time to night (13000 ticks)";
                    } else if (parts[2].equals("noon")) {
                        return "Set the time to noon (6000 ticks)";
                    } else if (parts[2].equals("midnight")) {
                        return "Set the time to midnight (18000 ticks)";
                    } else {
                        try {
                            int ticks = Integer.parseInt(parts[2]);
                            return "Set the time to " + ticks + " ticks";
                        } catch (NumberFormatException e) {
                            return "Time set command executed";
                        }
                    }
                } else if (parts.length >= 3 && parts[1].equals("add")) {
                    return "Added " + parts[2] + " ticks to the current time";
                }
                break;
                
            case "weather":
                if (parts.length >= 2) {
                    String weatherType = parts[1];
                    String duration = parts.length >= 3 ? " for " + parts[2] + " seconds" : "";
                    
                    switch (weatherType) {
                        case "clear":
                            return "Set weather to clear" + duration;
                        case "rain":
                            return "Set weather to rain" + duration;
                        case "thunder":
                            return "Set weather to thunder" + duration;
                        default:
                            return "Weather command executed";
                    }
                }
                break;
                
            case "gamemode":
            case "gm":
                if (parts.length >= 2) {
                    String mode = parts[1];
                    String target = parts.length >= 3 ? " for " + parts[2] : "";
                    return "Set gamemode to " + mode + target;
                }
                break;
                
            case "give":
                if (parts.length >= 3) {
                    String player = parts[1];
                    String item = parts[2];
                    String amount = parts.length >= 4 ? parts[3] : "1";
                    return "Gave " + amount + " " + item + " to " + player;
                }
                break;
                
            case "tp":
            case "teleport":
                if (parts.length >= 2) {
                    return "Teleported " + (parts.length == 2 ? "to " + parts[1] : parts[1] + " to destination");
                }
                break;
                
            case "kill":
                String target = parts.length >= 2 ? parts[1] : "@s";
                return "Killed " + target;
                
            case "say":
                if (parts.length >= 2) {
                    String message = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
                    return "[Server] " + message;
                }
                break;
                
            case "op":
                if (parts.length >= 2) {
                    return "Made " + parts[1] + " a server operator";
                }
                break;
                
            case "deop":
                if (parts.length >= 2) {
                    return "Removed operator status from " + parts[1];
                }
                break;
                
            case "whitelist":
                if (parts.length >= 2) {
                    switch (parts[1]) {
                        case "add":
                            return parts.length >= 3 ? "Added " + parts[2] + " to whitelist" : "Whitelist add executed";
                        case "remove":
                            return parts.length >= 3 ? "Removed " + parts[2] + " from whitelist" : "Whitelist remove executed";
                        case "on":
                            return "Whitelist enabled";
                        case "off":
                            return "Whitelist disabled";
                        case "list":
                            return getWhitelistInfo();
                        case "reload":
                            return "Whitelist reloaded";
                    }
                }
                break;
                
            case "ban":
                if (parts.length >= 2) {
                    String reason = parts.length >= 3 ? " Reason: " + String.join(" ", java.util.Arrays.copyOfRange(parts, 2, parts.length)) : "";
                    return "Banned player " + parts[1] + reason;
                }
                break;
                
            case "pardon":
            case "unban":
                if (parts.length >= 2) {
                    return "Unbanned player " + parts[1];
                }
                break;
                
            case "kick":
                if (parts.length >= 2) {
                    String reason = parts.length >= 3 ? " Reason: " + String.join(" ", java.util.Arrays.copyOfRange(parts, 2, parts.length)) : "";
                    return "Kicked player " + parts[1] + reason;
                }
                break;
                
            case "stop":
                return "Server is stopping...";
                
            case "reload":
                return "Server configuration reloaded";
                
            case "save-all":
                return "Saved all worlds";
                
            case "save-on":
                return "Enabled world auto-saving";
                
            case "save-off":
                return "Disabled world auto-saving";
                
            case "list":
                return getPlayerListInfo();
                
            case "difficulty":
                if (parts.length >= 2) {
                    return "Set difficulty to " + parts[1];
                }
                break;
                
            case "gamerule":
                if (parts.length >= 3) {
                    return "Set gamerule " + parts[1] + " to " + parts[2];
                } else if (parts.length >= 2) {
                    return "Queried gamerule " + parts[1];
                }
                break;
                
            case "effect":
                if (parts.length >= 3) {
                    if (parts[1].equals("clear")) {
                        return "Cleared effects from " + parts[2];
                    } else if (parts[1].equals("give") && parts.length >= 4) {
                        return "Applied effect " + parts[3] + " to " + parts[2];
                    }
                }
                break;
                
            case "playsound":
                if (parts.length >= 3) {
                    return "Played sound " + parts[1] + " to " + parts[2];
                }
                break;
                
            case "title":
                if (parts.length >= 3) {
                    return "Displayed title to " + parts[1];
                }
                break;
                
            case "msg":
            case "tell":
            case "w":
                if (parts.length >= 3) {
                    String msg = String.join(" ", java.util.Arrays.copyOfRange(parts, 2, parts.length));
                    return "Sent message to " + parts[1] + ": " + msg;
                }
                break;
                
            case "scoreboard":
                return "Scoreboard command executed";
                
            case "team":
                return "Team command executed";
                
            case "summon":
                if (parts.length >= 2) {
                    return "Summoned entity: " + parts[1];
                }
                break;
                
            case "setblock":
                if (parts.length >= 5) {
                    return "Set block at " + parts[1] + " " + parts[2] + " " + parts[3] + " to " + parts[4];
                }
                break;
                
            case "fill":
                if (parts.length >= 8) {
                    return "Filled area with " + parts[7];
                }
                break;
                
            case "clone":
                return "Cloned blocks from source to destination";
                
            case "particle":
                if (parts.length >= 2) {
                    return "Created particle effect: " + parts[1];
                }
                break;
                
            default:
                // For unknown commands, just indicate execution
                return "Command '" + parts[0] + "' executed successfully";
        }
        
        return "Command executed successfully";
    }
    
    private static String getPlayerListInfo() {
        List<String> playerNames = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerNames.add(player.getName());
        }
        
        if (playerNames.isEmpty()) {
            return "No players online";
        } else {
            return "Players online (" + playerNames.size() + "/" + Bukkit.getMaxPlayers() + "): " + String.join(", ", playerNames);
        }
    }
    
    private static String getWhitelistInfo() {
        int count = Bukkit.getWhitelistedPlayers().size();
        return "Whitelist contains " + count + " player(s)";
    }
    
    /**
     * Result of command execution
     */
    public static class CommandResult {
        private final String command;
        private final boolean success;
        private final List<HashMap<String, String>> output;
        
        public CommandResult(String command, boolean success, List<HashMap<String, String>> output) {
            this.command = command;
            this.success = success;
            this.output = output;
        }
        
        public String getCommand() {
            return command;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public List<HashMap<String, String>> getOutput() {
            return output;
        }
    }
}