package me.lucko.networkinterceptor;

import me.lucko.networkinterceptor.blockers.Blocker;
import me.lucko.networkinterceptor.blockers.CompositeBlocker;
import me.lucko.networkinterceptor.blockers.LearningBlocker;
import me.lucko.networkinterceptor.bukkit.BukkitConfiguration;
import me.lucko.networkinterceptor.common.AbstractConfiguration;
import me.lucko.networkinterceptor.common.CommonNetworkInterceptor;
import me.lucko.networkinterceptor.common.NetworkInterceptorPlugin;
import me.lucko.networkinterceptor.common.CommonNetworkInterceptor.IllegalConfigStateException;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public class NetworkInterceptor extends JavaPlugin implements NetworkInterceptorPlugin<JavaPlugin> {
    private final CommonNetworkInterceptor<NetworkInterceptor, JavaPlugin> delegate;
    private BukkitConfiguration config;
    private boolean registerManualStopTask = false;

    public NetworkInterceptor() {
        // init early
        // this is seen as bad practice, but we want to try and catch as
        // many requests as possible
        config = new BukkitConfiguration(getConfig());
        delegate = new CommonNetworkInterceptor<>(this);

        // check and enable bStats
        boolean useMetrics = getConfig().getBoolean("enable-metrics", true);
        if (useMetrics) {
            int pluginId = 11822;
            new Metrics(this, pluginId);
        }
        getLogger().info(useMetrics ? "bStats metrics enabled" : "bStats metrics disabled");
    }

    @Override
    public void onEnable() {
        delegate.onEnable();
        if (registerManualStopTask) {
            getServer().getScheduler().runTaskLater(this, () -> {
                if (delegate.getBlocker() instanceof CompositeBlocker) {
                    ((CompositeBlocker<JavaPlugin>) delegate.getBlocker()).stopUsingManualBlocker();
                } else if (delegate.getBlocker() instanceof LearningBlocker) {
                    Blocker<JavaPlugin> delegate = ((LearningBlocker<JavaPlugin>) this.delegate.getBlocker())
                            .getDelegate();
                    if (delegate instanceof CompositeBlocker) {
                        ((CompositeBlocker<JavaPlugin>) delegate).stopUsingManualBlocker();
                    }
                }
            }, 1L);
        }
        getCommand("networkinterceptor").setExecutor(new NetworkInterceptorCommand<>(this).asSpigotCommand());
    }

    @Override
    public void onDisable() {
        disable();
    }

    @Override
    public void reload() {
        reloadConfig();
        config = new BukkitConfiguration(getConfig());

        disable();
        try {
            enable();
        } catch (IllegalConfigStateException e) {
            getLogger().severe(e.getMessage());
            getLogger().severe("Disabling plugin");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void enable() throws IllegalConfigStateException {
        delegate.enable();
    }

    private void disable() {
        delegate.disable();
    }

    @Override
    public AbstractConfiguration getConfiguration() {
        return config;
    }

    @Override
    public void disablePlugin() {
        getServer().getPluginManager().disablePlugin(this);
    }

    @Override
    public String getServerVersion() {
        return getServer().getVersion();
    }

    @Override
    public String getPluginVersion() {
        return getDescription().getVersion();
    }

    @Override
    public void runTaskLater(Runnable runnable, long ticks) {
        getServer().getScheduler().runTaskLater(this, runnable, ticks);
    }

    @Override
    public boolean isBukkit() {
        return true;
    }

    @Override
    public boolean isBungee() {
        return false;
    }

    @Override
    public boolean isVelocity() {
        return false;
    }

    @Override
    public CommonNetworkInterceptor<NetworkInterceptor, JavaPlugin> getDelegate() {
        return delegate;
    }

    @Override
    public JavaPlugin asPlugin() {
        return this;
    }

}
