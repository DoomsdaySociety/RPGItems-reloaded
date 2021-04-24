package think.rpgitems.power.impl;

import static think.rpgitems.power.Utils.*;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.power.*;
import think.rpgitems.utils.PotionEffectUtils;

/**
 * Power aoe.
 *
 * <p>On right click the aoe power will apply {@link #type effect} to all entities within the {@link
 * #range range} for {@link #duration duration} ticks at power {@link #amplifier amplifier}. By
 * default, the user will be targeted by the potion as well if not set via {@link #selfapplication
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
        implClass = AOE.Impl.class)
public class AOE extends BasePower {
    @Property(order = 4, required = true)
    public int amplifier = 1;

    @Property(order = 3)
    public int duration = 15;

    @Property(order = 1)
    public int range = 5;

    @Property(order = 5)
    public boolean selfapplication = true;

    @Property(order = 2)
    @Deserializer(PotionEffectUtils.class)
    @Serializer(PotionEffectUtils.class)
    @AcceptedValue(preset = Preset.POTION_EFFECT_TYPE)
    public PotionEffectType type;

    @Property(alias = "name")
    public String display = null;

    @Property public int count = -1;

    /**
     * a cone angle from player eye location to center. or eye direction if two points is near.
     * similar to AOEDamage.
     */
    @Property public int angle = 180;

    @Property public Target target = Target.ALL;

    enum Target {
        MOBS,
        PLAYERS,
        ALL
    }

    public int getCount() {
        return count;
    }

    public int getAngle() {
        return angle;
    }

    public Target getTarget() {
        return target;
    }

    @Override
    public String getName() {
        return "aoe";
    }

    @Override
    public String displayText() {
        return getDisplay() != null
                ? getDisplay()
                : I18n.formatDefault(
                        "power.aoe.display",
                        getType().getName(),
                        getAmplifier(),
                        getDuration(),
                        isSelfapplication()
                                ? I18n.formatDefault("power.aoe.selfapplication.including")
                                : I18n.formatDefault("power.aoe.selfapplication.excluding"),
                        getRange(),
                        0);
    }

    public String getDisplay() {
        return display;
    }

    /** Type of the potion */
    public PotionEffectType getType() {
        return type;
    }

    /** Amplifier of the potion */
    public int getAmplifier() {
        return amplifier;
    }

    /** Duration of the potion */
    public int getDuration() {
        return duration;
    }

    /** Whether the potion will be apply to the user */
    public boolean isSelfapplication() {
        return selfapplication;
    }

    /** Range of the potion */
    public int getRange() {
        return range;
    }

    public static class Impl
            implements PowerRightClick<AOE>,
                    PowerLeftClick<AOE>,
                    PowerOffhandClick<AOE>,
                    PowerPlain<AOE>,
                    PowerHit<AOE>,
                    PowerBowShoot<AOE>,
                    PowerBeamHit<AOE>,
                    PowerProjectileHit<AOE>,
                    PowerLivingEntity<AOE>,
                    PowerLocation<AOE> {
        @Override
        public PowerResult<Void> rightClick(
                AOE power, final Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> fire(AOE power, Player player, ItemStack stack) {
            return fire(
                    power,
                    player.getEyeLocation(),
                    player,
                    getNearbyEntities(power, player.getLocation(), player, power.getRange()));
        }

        private PowerResult<Void> fire(
                AOE power, Location center, Player player, Collection<Entity> entityList) {
            int range = power.getRange();

            PotionEffect effect =
                    new PotionEffect(
                            power.getType(), power.getDuration(), power.getAmplifier() - 1);
            //            player.getWorld().playEffect(center, Effect.POTION_BREAK,
            // getType().getColor().asRGB());

            List<LivingEntity> collect =
                    entityList.stream()
                            .filter(entity -> entity instanceof LivingEntity)
                            .map(entity -> ((LivingEntity) entity))
                            .collect(Collectors.toList());
            if (!Objects.equals(center.getWorld(), player.getEyeLocation().getWorld())) {
                return PowerResult.noop();
            }
            Vector subtract = center.clone().subtract(player.getEyeLocation()).toVector();
            if (center.distance(player.getEyeLocation()) < 1) {
                subtract = player.getEyeLocation().getDirection();
            }
            getLivingEntitiesInConeSorted(collect, center.toVector(), power.getAngle(), subtract)
                    .stream()
                    .filter(
                            entity -> {
                                switch (power.getTarget()) {
                                    case MOBS:
                                        return entity instanceof Mob;
                                    case PLAYERS:
                                        return entity instanceof Player;
                                    default:
                                        return true;
                                }
                            })
                    .limit(power.getCount() == -1 ? Integer.MAX_VALUE : power.getCount())
                    .forEach(
                            ent -> {
                                if (player.equals(ent) && power.isSelfapplication()) {
                                    player.addPotionEffect(effect);
                                }
                                if (ent != null
                                        && Objects.equals(
                                                ent.getLocation().getWorld(), center.getWorld())
                                        && ent.getLocation().distance(center) <= range) {
                                    ent.addPotionEffect(effect);
                                }
                            });
            return PowerResult.ok();
        }

        @Override
        public Class<? extends AOE> getPowerClass() {
            return AOE.class;
        }

        @Override
        public PowerResult<Void> leftClick(
                AOE power, final Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> offhandClick(
                AOE power, final Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Double> hit(
                AOE power,
                Player player,
                ItemStack stack,
                LivingEntity entity,
                double damage,
                EntityDamageByEntityEvent event) {
            return fire(power, player, stack).with(damage);
        }

        @Override
        public PowerResult<Float> bowShoot(
                AOE power, Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(power, player, stack).with(event.getForce());
        }

        @Override
        public PowerResult<Void> hitBlock(
                AOE power,
                Player player,
                ItemStack stack,
                Location location,
                BeamHitBlockEvent event) {
            Location center = event.getLocation();
            return fire(
                    power,
                    center,
                    player,
                    getNearbyEntities(power, center, player, power.getRange()));
        }

        @Override
        public PowerResult<Void> projectileHit(
                AOE power, Player player, ItemStack stack, ProjectileHitEvent event) {
            Location center = event.getEntity().getLocation();
            return fire(
                    power,
                    center,
                    player,
                    getNearbyEntities(power, center, player, power.getRange()));
        }

        @Override
        public PowerResult<Void> beamEnd(
                AOE power, Player player, ItemStack stack, Location location, BeamEndEvent event) {
            Location center = location;
            return fire(
                    power,
                    center,
                    player,
                    getNearbyEntities(power, center, player, power.getRange()));
        }

        @Override
        public PowerResult<Void> fire(
                AOE power,
                Player player,
                ItemStack stack,
                LivingEntity entity,
                @Nullable Double value) {
            return fire(
                    power,
                    entity.getEyeLocation(),
                    player,
                    getNearbyEntities(power, entity.getLocation(), player, power.getRange()));
        }

        @Override
        public PowerResult<Void> fire(
                AOE power, Player player, ItemStack stack, Location location) {
            return fire(
                    power,
                    location,
                    player,
                    getNearbyEntities(power, location, player, power.getRange()));
        }
    }
}
