package think.rpgitems.power.impl;

import static think.rpgitems.power.Utils.*;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;

@Meta(immutableTrigger = true, withSelectors = true, implClass = Mount.Impl.class)
public class Mount extends BasePower {
    @Property(order = 1)
    public int maxDistance = 5;

    @Property(order = 2)
    public int maxTicks = 200;

    public int getMaxDistance() {
        return maxDistance;
    }

    public int getMaxTicks() {
        return maxTicks;
    }

    @Override
    public String getName() {
        return "mount";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.mount", (0) / 20D);
    }

    public static class Impl implements PowerRightClick<Mount> {

        @Override
        public PowerResult<Void> rightClick(
                Mount power, Player player, ItemStack stack, PlayerInteractEvent event) {
            if (player.isInsideVehicle()) {
                return PowerResult.fail();
            }
            List<LivingEntity> entities =
                    getLivingEntitiesInCone(
                            getNearestLivingEntities(
                                    power,
                                    player.getEyeLocation(),
                                    player,
                                    power.getMaxDistance(),
                                    0),
                            player.getLocation().toVector(),
                            30,
                            player.getLocation().getDirection());
            for (LivingEntity entity : entities) {
                if (entity.isValid()
                        && entity.getType() != EntityType.ARMOR_STAND
                        && !entity.isInsideVehicle()
                        && entity.getPassengers().isEmpty()
                        && player.hasLineOfSight(entity)
                        && entity.addPassenger(player)) {
                    player.getWorld()
                            .playSound(player.getLocation(), Sound.ENTITY_HORSE_ARMOR, 1.0F, 1.0F);
                    Listener listener =
                            new Listener() {
                                @EventHandler
                                public void onPlayerQuit(PlayerQuitEvent e) {
                                    if (e.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                                        entity.removePassenger(player);
                                        player.leaveVehicle();
                                    }
                                }
                            };
                    Bukkit.getPluginManager().registerEvents(listener, RPGItems.plugin);
                    new BukkitRunnable() {
                        private long ticks = 0L;

                        @Override
                        public void run() {
                            if (ticks >= power.getMaxTicks()
                                    || entity.isDead()
                                    || entity.getPassengers().isEmpty()
                                    || player.isDead()) {
                                cancel();
                                HandlerList.unregisterAll(listener);
                                if (!entity.isDead() && !entity.getPassengers().isEmpty()) {
                                    entity.removePassenger(player);
                                    player.leaveVehicle();
                                }
                            }
                            ticks++;
                        }
                    }.runTaskTimer(RPGItems.plugin, 1, 1);
                    return PowerResult.ok();
                }
            }

            return PowerResult.fail();
        }

        @Override
        public Class<? extends Mount> getPowerClass() {
            return Mount.class;
        }
    }
}
