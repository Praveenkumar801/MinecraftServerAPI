/**
 * MinecraftServerAPI.java
 * <p>
 *     This class is the main class of the plugin.
 *     It is responsible for starting and stopping the web server.
 *     It also creates the configuration file if it does not exist.
 * </p>
 */

package com.shweit.serverapi;

import com.shweit.serverapi.commands.RegisterCommands;
import com.shweit.serverapi.endpoints.RegisterEndpoints;
import com.shweit.serverapi.utils.Logger;
import com.shweit.serverapi.webhooks.RegisterWebHooks;
import com.shweit.serverapi.webhooks.server.ServerStop;
import fi.iki.elonen.NanoHTTPD;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.shweit.serverapi.listeners.PlayerLoginListener;

import java.util.List;
import java.util.Set;

public class MinecraftServerAPI extends JavaPlugin  {

    private static final int DEFAULT_PORT = 7000;
    private WebServer server;
    public static FileConfiguration config;
    public static String pluginName = "MinecraftServerAPI";
    private static MinecraftServerAPI instance;

    private static boolean blockNewConnections = false;
    private static String blockNewConnectionsMessage;

    public static boolean isPluginInstalled(final String string) {
        return Bukkit.getPluginManager().isPluginEnabled(string);
    }

    @Override
    public final void onEnable() {
        registerEvents();
        createConfig();

        config = getConfig();
        instance = this;

        boolean authEnabled = getConfig().getBoolean("authentication.enabled", true);
        String authKey = getConfig().getString("authentication.key", "CHANGE_ME");

        Set<String> rejectedKeys = Set.of("CHANGE_ME", "TestKey", "test", "admin", "password");

        if (!authEnabled) {
            Logger.warning("Authentication is disabled. This is not recommended.");
        } else if (rejectedKeys.contains(authKey)) {
            Logger.error("Please change the authKey in the config.yml file. Well-known default keys are rejected.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        int port = getConfig().getInt("port", DEFAULT_PORT);
        String bindAddress = getConfig().getString("bind-address", "127.0.0.1");

        boolean ipWhitelistEnabled = getConfig().getBoolean("ip-whitelist.enabled", false);
        List<String> whitelistedIps = getConfig().getStringList("ip-whitelist.allowed-ips");

        if (ipWhitelistEnabled) {
            if (whitelistedIps.isEmpty()) {
                Logger.warning("IP whitelist is enabled but no IPs are configured. All requests will be blocked.");
            } else {
                Logger.info("IP whitelist enabled. Allowed IPs: " + whitelistedIps);
            }
        }

        server = new WebServer(port, bindAddress, authEnabled, authKey, ipWhitelistEnabled, whitelistedIps);

        new RegisterEndpoints(server).registerEndpoints();

        new RegisterWebHooks().registerWebHooks();

        new RegisterCommands(this).register();

        try {
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            Logger.info("Web server started on port " + port);
        } catch (Exception e) {
            Logger.error("Failed to start web server: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public final void onDisable() {
        new ServerStop().register();

        if (server != null) {
            server.stop();
            Logger.info("Web server stopped.");
        }
    }

    private void createConfig() {
        saveDefaultConfig();
    }

    public static MinecraftServerAPI getInstance() {
        return instance;
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new PlayerLoginListener(), this);
    }

    public static void setBlockNewConnections(final boolean block, final String message) {
        blockNewConnections = block;
        if (block) {
            blockNewConnectionsMessage = message;
        }
    }

    public static boolean isBlockNewConnections() {
        return blockNewConnections;
    }

    public static String getBlockNewConnectionsMessage() {
        return blockNewConnectionsMessage;
    }
}
