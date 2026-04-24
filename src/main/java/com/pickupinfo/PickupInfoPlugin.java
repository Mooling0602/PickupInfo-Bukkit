package com.pickupinfo;

import org.bukkit.plugin.java.JavaPlugin;

public class PickupInfoPlugin extends JavaPlugin {

    private PickupListener listener;

    @Override
    public void onEnable() {
        listener = new PickupListener(this);
        getServer().getPluginManager().registerEvents(listener, this);
        getLogger().info("PickupInfo enabled");
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            listener.cancel();
        }
    }
}
