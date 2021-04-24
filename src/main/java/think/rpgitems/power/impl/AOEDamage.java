package think.rpgitems.power.impl;

import static java.lang.Double.max;
import static java.lang.Double.min;
import static think.rpgitems.Events.*;
import static think.rpgitems.power.Utils.getLivingEntitiesInCone;
import static think.rpgitems.power.Utils.getNearestLivingEntities;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.RPGItems;
import think.rpgitems.data.Context;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.power.*;
import think.rpgitems.utils.LightContext;
import think.rpgitems.utils.cast.CastUtils;

/**
 * Power AOEDamage.
 *
 * <p>On trigger the power will deal {@link #damage damage} to all entities within the {@link #range
 * range}. By default, the user will not be targeted as well if not set via {@link #selfapplication
 * selfapplication}.
 */
@Meta(
        defaultTrigger = "RIGHT_CLICK",
        withSelectors = true,
        generalInterface = {
            PowerLeftClick.class,
            PowerRightClick.class,
            PowerPlain.class,
            PowerSneak.class,
            PowerLivingEntity.class,
            PowerSprint.class,
            PowerHurt.class,
            PowerHit.class,
            PowerHitTaken.class,
            PowerBowShoot.class,
            PowerBeamHit.class,
            PowerLocation.class
        },
        implClass = AOEDamage.Impl.class)
public class AOEDamage extends BasePower {
    @Property public int range = 10;
    @Property public int minrange = 0;
    @Property public double angle = 180;
    @Property public int count = 100;
    @Property public boolean incluePlayers = false;
    @Property public boolean selfapplication = false;
    @Property public boolean mustsee = false;
    @Property public String name = null;
    @Property public double damage = 0;

    @Property public long delay = 0;

    @Property public boolean suppressMelee = false;

    @Property public boolean selectAfterDelay = false;

    @Property public double firingRange = 64;

    @Property public FiringLocation firingLocation = FiringLocation.SELF;

    @Property public boolean castOff = false;

    public boolean isCastOff() {
        return castOff;
    }

    public FiringLocation getFiringLocation() {
        return firingLocation;
    }

    public double getFiringRange() {
        return firingRange;
    }

    public enum FiringLocation {
        SELF,
        TARGET
    }

    /** Select target after delay. */
    public boolean isSelectAfterDelay() {
        return selectAfterDelay;
    }

    /** Maximum view angle */
    public double getAngle() {
        return angle;
    }

    /** Maximum count, excluding the user */
    public int getCount() {
        return count;
    }

    /** Damage of this power */
    public double getDamage() {
        return damage;
    }

    /** Delay of the damage */
    public long getDelay() {
        return delay;
    }

    /** Minimum radius */
    public int getMinrange() {
        return minrange;
    }

    /** Range of the power */
    public int getRange() {
        return range;
    }

    /** Whether include players */
    public boolean isIncluePlayers() {
        return incluePlayers;
    }

    /** Whether only apply to the entities that player have line of sight */
    public boolean isMustsee() {
        return mustsee;
    }

    /** Whether damage will be apply to the user */
    public boolean isSelfapplication() {
        return selfapplication;
    }

    /** Whether to suppress the hit trigger */
    public boolean isSuppressMelee() {
        return suppressMelee;
    }

    public static class Impl
            implements PowerOffhandClick<AOEDamage>,
                    PowerPlain<AOEDamage>,
                    PowerLeftClick<AOEDamage>,
                    PowerRightClick<AOEDamage>,
                    PowerHit<AOEDamage>,
                    PowerSprint<AOEDamage>,
                    PowerSneak<AOEDamage>,
                    PowerHurt<AOEDamage>,
                    PowerHitTaken<AOEDamage>,
                    PowerTick<AOEDamage>,
                    PowerBowShoot<AOEDamage>,
                    PowerSneaking<AOEDamage>,
                    PowerBeamHit<AOEDamage>,
                    PowerProjectileHit<AOEDamage>,
                    PowerLivingEntity<AOEDamage>,
                    PowerLocation<AOEDamage> {

        @Override
        public PowerResult<Void> rightClick(
                AOEDamage power, final Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> fire(AOEDamage power, Player player, ItemStack stack) {
            Supplier<Location> traceResultSupplier = player::getEyeLocation;
            if (power.getFiringLocation().equals(FiringLocation.TARGET)) {
                if (power.isCastOff()) {
                    CastUtils.CastLocation castLocation =
                            CastUtils.rayTrace(
                                    player,
                                    player.getEyeLocation(),
                                    player.getEyeLocation().getDirection(),
                                    power.getFiringRange());
                    Location targetLocation = castLocation.getTargetLocation();
                    traceResultSupplier = () -> targetLocation;
                } else {
                    traceResultSupplier =
                            () -> {
                                CastUtils.CastLocation castLocation =
                                        CastUtils.rayTrace(
                                                player,
                                                player.getEyeLocation(),
                                                player.getEyeLocation().getDirection(),
                                                power.getFiringRange());
                                Location targetLocation = castLocation.getTargetLocation();
                                return targetLocation;
                            };
                }
            }

            Supplier<Location> finalTraceResultSupplier = traceResultSupplier;
            return fire(
                    power,
                    player,
                    stack,
                    () -> {
                        List<LivingEntity> nearbyEntities;
                        List<LivingEntity> ent;
                        if (power.getFiringLocation().equals(FiringLocation.TARGET)) {
                            Location targetLocation = finalTraceResultSupplier.get();
                            ent =
                                    getNearestLivingEntities(
                                            power,
                                            targetLocation,
                                            player,
                                            power.getRange(),
                                            power.getMinrange());
                        } else {
                            nearbyEntities =
                                    getNearestLivingEntities(
                                            power,
                                            player.getLocation(),
                                            player,
                                            power.getRange(),
                                            power.getMinrange());
                            ent =
                                    getLivingEntitiesInCone(
                                            nearbyEntities,
                                            player.getEyeLocation().toVector(),
                                            power.getAngle(),
                                            player.getEyeLocation().getDirection());
                        }
                        return ent;
                    });
        }

        private PowerResult<Void> fire(
                AOEDamage power,
                Player player,
                ItemStack stack,
                Supplier<List<LivingEntity>> supplier) {
            Context.instance()
                    .putTemp(
                            player.getUniqueId(),
                            DAMAGE_SOURCE,
                            power.getNamespacedKey().toString());
            Context.instance().putTemp(player.getUniqueId(), OVERRIDING_DAMAGE, power.getDamage());
            Context.instance()
                    .putTemp(player.getUniqueId(), SUPPRESS_MELEE, power.isSuppressMelee());
            if (power.isSelfapplication()) dealDamage(player, power.getDamage());
            LivingEntity[] entities = supplier.get().toArray(new LivingEntity[0]);
            int c = power.getCount();
            if (power.getDelay() <= 0) {
                for (int i = 0; i < c && i < entities.length; ++i) {
                    LivingEntity e = entities[i];
                    if ((power.isMustsee() && !player.hasLineOfSight(e))
                            || (e == player)
                            || (!power.isIncluePlayers() && e instanceof Player)) {
                        c++;
                        continue;
                    }
                    LightContext.putTemp(
                            player.getUniqueId(), OVERRIDING_DAMAGE, power.getDamage());
                    LightContext.putTemp(
                            player.getUniqueId(), SUPPRESS_MELEE, power.isSuppressMelee());
                    LightContext.putTemp(player.getUniqueId(), DAMAGE_SOURCE_ITEM, stack);
                    e.damage(power.getDamage(), player);
                    LightContext.removeTemp(player.getUniqueId(), SUPPRESS_MELEE);
                    LightContext.removeTemp(player.getUniqueId(), OVERRIDING_DAMAGE);
                    LightContext.removeTemp(player.getUniqueId(), DAMAGE_SOURCE_ITEM);
                }
            } else {
                (new BukkitRunnable() {
                            @Override
                            public void run() {
                                LivingEntity[] entities1 = entities;
                                if (power.isSelectAfterDelay()) {
                                    entities1 = supplier.get().toArray(new LivingEntity[0]);
                                }
                                int c = power.getCount();

                                for (int i = 0; i < c && i < entities1.length; ++i) {
                                    LivingEntity e = entities1[i];
                                    if ((power.isMustsee() && !player.hasLineOfSight(e))
                                            || (e == player)
                                            || (!power.isIncluePlayers() && e instanceof Player)) {
                                        c++;
                                        continue;
                                    }
                                    LightContext.putTemp(
                                            player.getUniqueId(),
                                            OVERRIDING_DAMAGE,
                                            power.getDamage());
                                    LightContext.putTemp(
                                            player.getUniqueId(),
                                            SUPPRESS_MELEE,
                                            power.isSuppressMelee());
                                    LightContext.putTemp(
                                            player.getUniqueId(), DAMAGE_SOURCE_ITEM, stack);
                                    e.damage(power.getDamage(), player);
                                    LightContext.putTemp(
                                            player.getUniqueId(),
                                            SUPPRESS_MELEE,
                                            power.isSuppressMelee());
                                    LightContext.putTemp(
                                            player.getUniqueId(), OVERRIDING_DAMAGE, null);
                                    LightContext.removeTemp(
                                            player.getUniqueId(), DAMAGE_SOURCE_ITEM);
                                }
                            }
                        })
                        .runTaskLater(RPGItems.plugin, power.getDelay());
            }

            return PowerResult.ok();
        }

        @Override
        public Class<? extends AOEDamage> getPowerClass() {
            return AOEDamage.class;
        }

        private void dealDamage(LivingEntity entity, double damage) {
            if (entity.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
                PotionEffect e = entity.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                assert e != null;
                if (e.getAmplifier() >= 4) return;
            }
            double health = entity.getHealth();
            double newHealth = health - damage;
            newHealth = max(newHealth, 0.1);
            newHealth =
                    min(newHealth, entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            entity.setHealth(newHealth);
        }

        @Override
        public PowerResult<Void> leftClick(
                AOEDamage power, final Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> offhandClick(
                AOEDamage power, final Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Double> hit(
                AOEDamage power,
                final Player player,
                ItemStack stack,
                LivingEntity entity,
                double damage,
                EntityDamageByEntityEvent event) {
            return fire(power, player, stack).with(damage);
        }

        @Override
        public PowerResult<Void> sprint(
                AOEDamage power, Player player, ItemStack stack, PlayerToggleSprintEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sneak(
                AOEDamage power, Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> hurt(
                AOEDamage power, Player target, ItemStack stack, EntityDamageEvent event) {
            return fire(power, target, stack);
        }

        @Override
        public PowerResult<Double> takeHit(
                AOEDamage power,
                Player target,
                ItemStack stack,
                double damage,
                EntityDamageEvent event) {
            return fire(power, target, stack).with(damage);
        }

        @Override
        public PowerResult<Float> bowShoot(
                AOEDamage power, Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(power, player, stack).with(event.getForce());
        }

        @Override
        public PowerResult<Void> tick(AOEDamage power, Player player, ItemStack stack) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sneaking(AOEDamage power, Player player, ItemStack stack) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Double> hitEntity(
                AOEDamage power,
                Player player,
                ItemStack stack,
                LivingEntity entity,
                double damage,
                BeamHitEntityEvent event) {
            Location location = entity.getLocation();
            int range = power.getRange();
            return fire(
                            power,
                            player,
                            stack,
                            () -> getNearbyEntities(power, player, location, range))
                    .with(damage);
        }

        private List<LivingEntity> getNearbyEntities(
                AOEDamage power, Player player, Location location, int range) {
            return Utils.getNearbyEntities(power, location, player, range).stream()
                    .filter(entity -> entity instanceof LivingEntity)
                    .map(entity -> ((LivingEntity) entity))
                    .collect(Collectors.toList());
        }

        @Override
        public PowerResult<Void> hitBlock(
                AOEDamage power,
                Player player,
                ItemStack stack,
                Location location,
                BeamHitBlockEvent event) {
            int range = power.getRange();
            return fire(
                    power, player, stack, () -> getNearbyEntities(power, player, location, range));
        }

        @Override
        public PowerResult<Void> beamEnd(
                AOEDamage power,
                Player player,
                ItemStack stack,
                Location location,
                BeamEndEvent event) {
            int range = power.getRange();
            return fire(
                    power, player, stack, () -> getNearbyEntities(power, player, location, range));
        }

        @Override
        public PowerResult<Void> projectileHit(
                AOEDamage power, Player player, ItemStack stack, ProjectileHitEvent event) {
            int range = power.getRange();
            return fire(
                    power,
                    player,
                    stack,
                    () -> getNearbyEntities(power, player, event.getEntity().getLocation(), range));
        }

        @Override
        public PowerResult<Void> fire(
                AOEDamage power,
                Player player,
                ItemStack stack,
                LivingEntity entity,
                @Nullable Double value) {
            Supplier<Location> traceResultSupplier = entity::getEyeLocation;
            if (power.getFiringLocation().equals(FiringLocation.TARGET)) {
                if (power.isCastOff()) {
                    CastUtils.CastLocation castLocation =
                            CastUtils.rayTrace(
                                    entity,
                                    entity.getEyeLocation(),
                                    entity.getEyeLocation().getDirection(),
                                    power.getFiringRange());
                    Location targetLocation = castLocation.getTargetLocation();
                    traceResultSupplier = () -> targetLocation;
                } else {
                    traceResultSupplier =
                            () -> {
                                CastUtils.CastLocation castLocation =
                                        CastUtils.rayTrace(
                                                entity,
                                                entity.getEyeLocation(),
                                                entity.getEyeLocation().getDirection(),
                                                power.getFiringRange());
                                Location targetLocation = castLocation.getTargetLocation();
                                return targetLocation;
                            };
                }
            }

            Supplier<Location> finalTraceResultSupplier = traceResultSupplier;
            return fire(
                    power,
                    player,
                    stack,
                    () -> {
                        List<LivingEntity> nearbyEntities;
                        List<LivingEntity> ent;
                        if (power.getFiringLocation().equals(FiringLocation.TARGET)) {
                            Location targetLocation = finalTraceResultSupplier.get();
                            ent =
                                    getNearestLivingEntities(
                                            power,
                                            targetLocation,
                                            player,
                                            power.getRange(),
                                            power.getMinrange());
                        } else {
                            nearbyEntities =
                                    getNearestLivingEntities(
                                            power,
                                            player.getLocation(),
                                            player,
                                            power.getRange(),
                                            power.getMinrange());
                            ent =
                                    getLivingEntitiesInCone(
                                            nearbyEntities,
                                            player.getEyeLocation().toVector(),
                                            power.getAngle(),
                                            player.getEyeLocation().getDirection());
                        }
                        return ent;
                    });
        }

        @Override
        public PowerResult<Void> fire(
                AOEDamage power, Player player, ItemStack stack, Location location) {
            int range = power.getRange();
            return fire(
                    power, player, stack, () -> getNearbyEntities(power, player, location, range));
        }
    }

    /** Display text of this power. Will use default text in case of null */
    @Override
    public String getName() {
        return "AOEDamage";
    }

    @Override
    public String displayText() {
        return getName() != null ? getName() : "Deal damage to nearby mobs";
    }
}
