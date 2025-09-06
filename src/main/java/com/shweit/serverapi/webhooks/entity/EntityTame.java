package com.shweit.serverapi.webhooks.entity;

import com.shweit.serverapi.MinecraftServerAPI;
import com.shweit.serverapi.webhooks.RegisterWebHooks;
import com.shweit.serverapi.webhooks.WebHook;
import com.shweit.serverapi.webhooks.WebHookEnum;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;
import org.json.JSONObject;

public final class EntityTame implements WebHook, Listener {

    private final String eventName = WebHookEnum.ENTITY_TAME.label;

    @Override
    public void register() {
        if (RegisterWebHooks.doActivateWebhook(eventName)) {
            MinecraftServerAPI plugin = MinecraftServerAPI.getInstance();
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        }
    }

    @EventHandler
    public void onEntityTame(final EntityTameEvent event) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("event", eventName);
        jsonObject.put("entity", event.getEntity().getType().name());
        jsonObject.put("location", event.getEntity().getLocation().toString());
        jsonObject.put("owner", event.getOwner().getName());

        String entityName = event.getEntity().getType().name().toLowerCase().replace("_", " ");
        String message = "Player " + event.getOwner().getName() + " tamed a " + entityName;
        jsonObject.put("message", message);

        RegisterWebHooks.sendToAllUrls(jsonObject);
    }

}
