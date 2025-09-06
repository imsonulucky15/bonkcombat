package me.imsonulucky.bonkcombat.util;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CombatEnd extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;

    public CombatEnd(Player player) {
        super(false); // now synchronous, safe to fire on main thread
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
