package com.shweit.serverapi.endpoints.v1;

import com.shweit.serverapi.MinecraftServerAPI;
import com.shweit.serverapi.utils.Logger;
import fi.iki.elonen.NanoHTTPD;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;

import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static com.shweit.serverapi.utils.Helper.deleteDirectory;

public final class PluginAPI {
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private static boolean isUnsafeUrl(final String urlString) {
        try {
            URL url = new URL(urlString);
            if (!ALLOWED_SCHEMES.contains(url.getProtocol().toLowerCase())) {
                return true;
            }
            InetAddress address = InetAddress.getByName(url.getHost());
            return address.isLoopbackAddress()
                || address.isSiteLocalAddress()
                || address.isLinkLocalAddress()
                || address.isAnyLocalAddress();
        } catch (Exception e) {
            return true;
        }
    }

    public NanoHTTPD.Response getPlugins(final Map<String, String> ignoredParams) {
        Plugin[] plugins = Bukkit.getPluginManager().getPlugins();

        JSONObject response = new JSONObject();
        for (Plugin plugin : plugins) {
            response.put(plugin.getName(), plugin.getDescription().getVersion());
        }

        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", response.toString());
    }

    public NanoHTTPD.Response getPlugin(final Map<String, String> params) {
        String pluginName = params.get("name");
        if (pluginName == null || pluginName.isEmpty()) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "application/json", "{}");
        }

        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "application/json", "{}");
        }

        JSONObject response = new JSONObject();
        response.put("name", plugin.getName());
        response.put("version", plugin.getDescription().getVersion());
        response.put("description", plugin.getDescription().getDescription());
        response.put("authors", plugin.getDescription().getAuthors());
        response.put("website", plugin.getDescription().getWebsite());
        response.put("contributors", plugin.getDescription().getContributors());
        response.put("dependencies", plugin.getDescription().getDepend());
        response.put("soft-dependencies", plugin.getDescription().getSoftDepend());
        response.put("load-order", plugin.getDescription().getLoad());
        response.put("enabled", plugin.isEnabled());

        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", response.toString());
    }

    public NanoHTTPD.Response postPlugin(final Map<String, String> params) {
        String pluginUrl = params.get("url");
        if (pluginUrl == null || pluginUrl.isEmpty()) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"Missing plugin URL.\"}");
        }

        if (isUnsafeUrl(pluginUrl)) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.FORBIDDEN, "application/json",
                "{\"error\":\"Only HTTP/HTTPS URLs to public addresses are allowed.\"}");
        }

        try {
            URL url = new URL(pluginUrl);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.connect();

            InputStream inputStream = new BufferedInputStream(connection.getInputStream());

            String rawFileName = pluginUrl.substring(pluginUrl.lastIndexOf('/') + 1);
            String fileName = rawFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
            if (!fileName.toLowerCase().endsWith(".jar")) {
                inputStream.close();
                return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "application/json",
                    "{\"error\":\"Only .jar files are allowed.\"}");
            }

            File pluginDir = new File("plugins");
            File pluginFile = new File(pluginDir, fileName);

            if (!pluginFile.getCanonicalPath().startsWith(pluginDir.getCanonicalPath())) {
                inputStream.close();
                return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.FORBIDDEN, "application/json",
                    "{\"error\":\"Invalid file path.\"}");
            }

            FileOutputStream outputStream = new FileOutputStream(pluginFile);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            if ("true".equals(params.get("reload"))) {
                final long delay = 20L;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Bukkit.reload();
                    }
                }.runTaskLater(MinecraftServerAPI.getInstance(), delay);
            }

            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", "{}");
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "application/json", "{\"error\":\"Failed to download plugin.\"}");
        }
    }

    public NanoHTTPD.Response deletePlugin(final Map<String, String> params) {
        String pluginName = params.get("name");
        if (pluginName == null || pluginName.isEmpty()) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "application/json", "{}");
        }

        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "application/json", "{}");
        }

        try {
            // Deactivate the plugin
            if (plugin.isEnabled()) {
                Bukkit.getPluginManager().disablePlugin(plugin);
            }

            // Get the Plugin .jar file
            File pluginFile = findPluginJar(pluginName);
            if (pluginFile != null && pluginFile.exists()) {
                // Rename the file before deletion to prevent issues
                Path backupPath = Paths.get(pluginFile.getParent(), pluginFile.getName() + ".deleting");
                Files.move(pluginFile.toPath(), backupPath, StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(backupPath);
            } else {
                return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "application/json", "{}");
            }

            File pluginDir = new File("plugins", plugin.getName());
            if (pluginDir.exists() && pluginDir.isDirectory()) {
                deleteDirectory(pluginDir);
            }


            if ("true".equals(params.get("reload"))) {
                final long delay = 20L;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Bukkit.reload();
                    }
                }.runTaskLater(MinecraftServerAPI.getInstance(), delay);
            }
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "application/json", "{}");
        }


        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", "{}");
    }

    public NanoHTTPD.Response activatePlugin(final Map<String, String> params) {
        String pluginName = params.get("name");
        if (pluginName == null || pluginName.isEmpty()) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "application/json", "{}");
        }

        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "application/json", "{}");
        }

        try {
            if (!plugin.isEnabled()) {
                Bukkit.getPluginManager().enablePlugin(plugin);
            }
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "application/json", "{}");
        }

        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", "{}");
    }

    public NanoHTTPD.Response deactivatePlugin(final Map<String, String> params) {
        String pluginName = params.get("name");
        if (pluginName == null || pluginName.isEmpty()) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "application/json", "{}");
        }

        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "application/json", "{}");
        }

        try {
            if (plugin.isEnabled()) {
                Bukkit.getPluginManager().disablePlugin(plugin);
            }
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "application/json", "{}");
        }

        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", "{}");
    }

    private File findPluginJar(final String pluginName) {
        File pluginDir = new File("plugins");
        File[] files = pluginDir.listFiles((dir, name) -> name.toLowerCase().startsWith(pluginName.toLowerCase()) && name.endsWith(".jar"));
        return files != null && files.length > 0 ? files[0] : null;
    }
}
