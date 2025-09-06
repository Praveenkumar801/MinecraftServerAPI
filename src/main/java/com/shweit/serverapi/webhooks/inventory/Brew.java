package com.shweit.serverapi.webhooks.inventory;

import com.shweit.serverapi.MinecraftServerAPI;
import com.shweit.serverapi.webhooks.RegisterWebHooks;
import com.shweit.serverapi.webhooks.WebHook;
import com.shweit.serverapi.webhooks.WebHookEnum;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.json.JSONObject;

public final class Brew implements WebHook, Listener {

    private final String eventName = WebHookEnum.BREW.label;

    @Override
    public void register() {
        if (RegisterWebHooks.doActivateWebhook(eventName)) {
            MinecraftServerAPI plugin = MinecraftServerAPI.getInstance();
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        }
    }

    @EventHandler
    public void onBrew(final BrewEvent event) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("event", eventName);
        jsonObject.put("fuelLevel", event.getFuelLevel());
        jsonObject.put("location", event.getBlock().getLocation().toString());

        String message = "Brewing stand finished brewing";
        if (event.getContents().getIngredient() != null) {
            jsonObject.put("ingredient", event.getContents().getIngredient().getType().name());
            String ingredientName = event.getContents().getIngredient().getType().name().toLowerCase().replace("_", " ");
            message = "Brewing stand finished brewing with " + ingredientName;
        }
        jsonObject.put("message", message);
        RegisterWebHooks.sendToAllUrls(jsonObject);
    }
}
