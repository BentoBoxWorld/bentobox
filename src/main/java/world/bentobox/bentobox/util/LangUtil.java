package world.bentobox.bentobox.util;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.banner.Pattern;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TropicalFish;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.TropicalFishBucketMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.hooks.LangUtilsHook;

import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unused")
public class LangUtil {

    private static LangUtilsHook hook;

    private LangUtil() {
    }

    private static LangUtilsHook getHook() {
        if (hook == null) {
            hook = BentoBox.getInstance().getHooks().getHook(LangUtilsHook.class);
        }
        return hook;
    }

    /**
     * Get the item display name.
     *
     * If the item contains a custom name, return its custom name.
     * If the item itself does not have a custom name, the material
     * name will be translated and returned.
     *
     * @param item The item
     * @param user the User's locale will be used for translation.
     * @return The Display-Name of the item.
     */
    @NonNull
    public static String getItemDisplayName(@NonNull ItemStack item, @NonNull User user) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return Util.prettifyText(item.getType().name());
    }

    /**
     * Name translation of ItemStack.
     * <p>
     * Translate the material names of the items so that players can
     * see the names they know well.
     *
     * @param itemStack the ItemStack whose name will be translated.
     * @param user      the User's locale will be used for translation.
     * @return The translated item name.
     */
    @NonNull
    public static String getItemName(@NonNull ItemStack itemStack, @NonNull User user) {
        return getHook() != null
            ? getHook().getItemName(itemStack, user)
            : Util.prettifyText(itemStack.getType().name());
    }

    /**
     * Name translation of Bukkit material.
     * <p>
     * Translate the material names of the items so that players can
     * see the names they know well.
     *
     * @param material the Bukkit material whose name will be translated.
     * @param user     the User's locale will be used for translation.
     * @return The translated material name.
     */
    @NonNull
    public static String getMaterialName(@NonNull Material material, @NonNull User user) {
        return getHook() != null
            ? getHook().getMaterialName(material, user)
            : Util.prettifyText(material.name());
    }

    /**
     * Return the display name of the entity.
     *
     * @param entity The entity
     * @param user   the User's locale will be used for translation.
     * @return The name of the entity
     */
    @NonNull
    public static String getEntityDisplayName(@NonNull Entity entity, @NonNull User user) {
        return entity.getCustomName() != null
            ? entity.getCustomName()
            : getEntityName(entity, user);
    }

    /**
     * Translate the name of the entity type.
     *
     * @param entityType the EntityType whose name will be translated.
     * @param user       the User's locale will be used for translation.
     * @return The translated EntityType name.
     */
    @NonNull
    public static String getEntityName(@NonNull EntityType entityType, @NonNull User user) {
        return getHook() != null
            ? getHook().getEntityName(entityType, user)
            : Util.prettifyText(entityType.toString());
    }

    /**
     * Translate the name of the entity type.
     *
     * @param entity the EntityType whose name will be translated.
     * @param user   the User's locale will be used for translation.
     * @return The translated EntityType name.
     */
    @NonNull
    public static String getEntityName(@NonNull Entity entity, @NonNull User user) {
        return getHook() != null
            ? getHook().getEntityName(entity, user)
            : Util.prettifyText(entity.getType().toString());
    }

    /**
     * Translate the name of the Biome.
     *
     * @param biome the Biome whose name will be translated.
     * @param user  the User's locale will be used for translation.
     * @return The translated Biome name.
     */
    @NonNull
    public static String getBiomeName(@NonNull Biome biome, @NonNull User user) {
        return getHook() != null
            ? getHook().getBiomeName(biome, user)
            : Util.prettifyText(biome.name());
    }

    /**
     * Return the display name of the enchantment(with level).
     *
     * @param enchant The enchantment.
     * @param level   The enchantment level.
     * @param user    The User's locale will be used for translation.
     * @return Translated enchanted name with level.
     */
    @NonNull
    public static String getEnchantDisplayName(@NonNull Enchantment enchant, int level, @NonNull User user) {
        return getHook() != null
            ? getHook().getEnchantDisplayName(enchant, level, user)
            : enchant.getKey().getKey() + " " + level;
    }

    /**
     * Return the display name of the enchantment(with level).
     *
     * @param entry The Entry of an enchantment with level The type
     *              is {@code Map.Entry<Enchantment, Integer>}
     * @param user  The User's locale will be used for translation.
     * @return Translated enchanted name with level.
     */
    @NonNull
    public static String getEnchantDisplayName(Map.@NonNull Entry<Enchantment, Integer> entry, @NonNull User user) {
        return getHook() != null
            ? getHook().getEnchantDisplayName(entry, user)
            : entry.getKey().getKey().getKey() + " " + entry.getValue();
    }

    /**
     * Return the name of the enchantment.
     *
     * @param enchant The enchantment.
     * @param user    The User's locale will be used for translation.
     * @return The translated enchant name.
     */
    @NonNull
    public static String getEnchantName(@NonNull Enchantment enchant, @NonNull User user) {
        return getHook() != null
            ? getHook().getEnchantName(enchant, user)
            : enchant.getKey().getKey();
    }

    /**
     * Return the enchantment level indicated by Roman numerals.
     * Can only get Roman numerals within 10.
     *
     * @param level The enchantment level.
     * @param user  The user's language will be used for translation.
     * @return The converted enchantment level.
     */
    @NonNull
    public static String getEnchantLevelName(int level, @NonNull User user) {
        return getHook() != null
            ? getHook().getEnchantLevelName(level, user)
            : String.valueOf(level);
    }

    /**
     * Translate the name of the potion.
     *
     * @param potionType The type of the potion.
     * @param user       The user's language will be used for translation.
     * @return Translated potion name.
     */
    @NonNull
    public static String getPotionTypeName(@NonNull PotionType potionType, @NonNull User user) {
        if (getHook() != null) {
            return getHook().getPotionTypeName(potionType, user);
        }
        switch (potionType) {
            case UNCRAFTABLE:     return "Uncraftable Potion";
            case WATER:           return "Water Bottle";
            case MUNDANE:         return "Mundane Potion";
            case THICK:           return "Thick Potion";
            case AWKWARD:         return "Awkward Potion";
            case NIGHT_VISION:    return "Potion of Night Vision";
            case INVISIBILITY:    return "Potion of Invisibility";
            case JUMP:            return "Potion of Leaping";
            case FIRE_RESISTANCE: return "Potion of Fire Resistance";
            case SPEED:           return "Potion of Swiftness";
            case SLOWNESS:        return "Potion of Slowness";
            case WATER_BREATHING: return "Potion of Water Breathing";
            case INSTANT_HEAL:    return "Potion of Healing";
            case INSTANT_DAMAGE:  return "Potion of Harming";
            case POISON:          return "Potion of Poison";
            case REGEN:           return "Potion of Regeneration";
            case STRENGTH:        return "Potion of Strength";
            case WEAKNESS:        return "Potion of Weakness";
            case LUCK:            return "Potion of Luck";
            case TURTLE_MASTER:   return "Potion of the Turtle Master";
            case SLOW_FALLING:    return "Potion of Slow Falling";
            default:
                return Util.prettifyText(potionType.name());
        }
    }

    /**
     * Translate the name of the splash potion.
     *
     * @param potionType The type of the splash potion.
     * @param user       The user's language will be used for translation.
     * @return Translated splash potion name.
     */
    @NonNull
    public static String getSplashPotionName(@NonNull PotionType potionType, @NonNull User user) {
        if (getHook() != null) {
            return getHook().getSplashPotionName(potionType, user);
        }
        switch (potionType) {
            case UNCRAFTABLE:     return "Splash Uncraftable Potion";
            case WATER:           return "Splash Water Bottle";
            case MUNDANE:         return "Mundane Splash Potion";
            case THICK:           return "Thick Splash Potion";
            case AWKWARD:         return "Awkward Splash Potion";
            case NIGHT_VISION:    return "Splash Potion of Night Vision";
            case INVISIBILITY:    return "Splash Potion of Invisibility";
            case JUMP:            return "Splash Potion of Leaping";
            case FIRE_RESISTANCE: return "Splash Potion of Fire Resistance";
            case SPEED:           return "Splash Potion of Swiftness";
            case SLOWNESS:        return "Splash Potion of Slowness";
            case WATER_BREATHING: return "Splash Potion of Water Breathing";
            case INSTANT_HEAL:    return "Splash Potion of Healing";
            case INSTANT_DAMAGE:  return "Splash Potion of Harming";
            case POISON:          return "Splash Potion of Poison";
            case REGEN:           return "Splash Potion of Regeneration";
            case STRENGTH:        return "Splash Potion of Strength";
            case WEAKNESS:        return "Splash Potion of Weakness";
            case LUCK:            return "Splash Potion of Luck";
            case TURTLE_MASTER:   return "Splash Potion of the Turtle Master";
            case SLOW_FALLING:    return "Splash Potion of Slow Falling";
            default:
                return Util.prettifyText(potionType.name());
        }
    }

    /**
     * Translate the name of the lingering potion.
     *
     * @param potionType The type of lingering potion.
     * @param user       The user's language will be used for translation.
     * @return Translated lingering potion name.
     */
    @NonNull
    public static String getLingeringPotionName(@NonNull PotionType potionType, @NonNull User user) {
        if (getHook() != null) {
            return getHook().getLingeringPotionName(potionType, user);
        }
        switch (potionType) {
            case UNCRAFTABLE:     return "Lingering Uncraftable Potion";
            case WATER:           return "Lingering Water Bottle";
            case MUNDANE:         return "Mundane Lingering Potion";
            case THICK:           return "Thick Lingering Potion";
            case AWKWARD:         return "Awkward Lingering Potion";
            case NIGHT_VISION:    return "Lingering Potion of Night Vision";
            case INVISIBILITY:    return "Lingering Potion of Invisibility";
            case JUMP:            return "Lingering Potion of Leaping";
            case FIRE_RESISTANCE: return "Lingering Potion of Fire Resistance";
            case SPEED:           return "Lingering Potion of Swiftness";
            case SLOWNESS:        return "Lingering Potion of Slowness";
            case WATER_BREATHING: return "Lingering Potion of Water Breathing";
            case INSTANT_HEAL:    return "Lingering Potion of Healing";
            case INSTANT_DAMAGE:  return "Lingering Potion of Harming";
            case POISON:          return "Lingering Potion of Poison";
            case REGEN:           return "Lingering Potion of Regeneration";
            case STRENGTH:        return "Lingering Potion of Strength";
            case WEAKNESS:        return "Lingering Potion of Weakness";
            case LUCK:            return "Lingering Potion of Luck";
            case TURTLE_MASTER:   return "Lingering Potion of the Turtle Master";
            case SLOW_FALLING:    return "Lingering Potion of Slow Falling";
            default:
                return Util.prettifyText(potionType.name());
        }
    }

    /**
     * Translate the name of the tipped arrow.
     *
     * @param potionType Potion type of tipped arrow.
     * @param user       The user's language will be used for translation.
     * @return Translated tipped arrow name.
     */
    @NonNull
    public static String getTippedArrowName(@NonNull PotionType potionType, @NonNull User user) {
        if (getHook() != null) {
            return getHook().getTippedArrowName(potionType, user);
        }
        switch (potionType) {
            case UNCRAFTABLE:     return "Uncraftable Tipped Arrow";
            case WATER:           return "Arrow of Splashing";
            case MUNDANE:
            case THICK:
            case AWKWARD:         return "Tipped Arrow";
            case NIGHT_VISION:    return "Arrow of Night Vision";
            case INVISIBILITY:    return "Arrow of Invisibility";
            case JUMP:            return "Arrow of Leaping";
            case FIRE_RESISTANCE: return "Arrow of Fire Resistance";
            case SPEED:           return "Arrow of Swiftness";
            case SLOWNESS:        return "Arrow of Slowness";
            case WATER_BREATHING: return "Arrow of Water Breathing";
            case INSTANT_HEAL:    return "Arrow of Healing";
            case INSTANT_DAMAGE:  return "Arrow of Harming";
            case POISON:          return "Arrow of Poison";
            case REGEN:           return "Arrow of Regeneration";
            case STRENGTH:        return "Arrow of Strength";
            case WEAKNESS:        return "Arrow of Weakness";
            case LUCK:            return "Arrow of Luck";
            case TURTLE_MASTER:   return "Arrow of the Turtle Master";
            case SLOW_FALLING:    return "Arrow of Slow Falling";
            default:
                return Util.prettifyText(potionType.name());
        }
    }

    /**
     * Translate the name of the base effect of the potion. If the PotionType
     * has no base effect, the translation of "No Effects" is returned. e.g.
     * Water Bottle, Mundane Potion.
     *
     * @param potionType The basic effect of PotionType.
     * @param user       The user's language will be used for translation.
     * @return Return the translation result.
     */
    @NonNull
    public static String getPotionBaseEffectName(@NonNull PotionType potionType, @NonNull User user) {
        if (getHook() != null) {
            return getHook().getPotionBaseEffectName(potionType, user);
        }
        PotionEffectType effectType = potionType.getEffectType();
        if (effectType == null) {
            return "No Effects";
        }
        return Util.prettifyText(effectType.getName());
    }

    /**
     * Translate the name of the potion effect.
     *
     * @param effectType The potion effect.
     * @param user       The user's language will be used for translation.
     * @return Translated name of potion effect.
     */
    @NonNull
    public static String getPotionEffectName(@NonNull PotionEffectType effectType, @NonNull User user) {
        return getHook() != null
            ? getHook().getPotionEffectName(effectType, user)
            : Util.prettifyText(effectType.getName());
    }

    /**
     * Translate the name of the potion level.
     *
     * @param amplifier The  potion level.
     * @param user      The user's language will be used for translation.
     * @return The translated name of the potion level.
     */
    @NonNull
    public static String getEffectAmplifierName(int amplifier, @NonNull User user) {
        if (getHook() != null) {
            return getHook().getEffectAmplifierName(amplifier, user);
        }
        return amplifier > 0 ? Integer.toString(amplifier) : "";
    }

    /**
     * Make the custom potion effect like the player usually sees.
     *
     * @param effect The potion effect.
     * @param user   The user's language will be used for translation.
     * @return The translated and formatted potion effect name, level, and duration.
     */
    @NonNull
    public static String getPotionEffectDisplay(@NonNull PotionEffect effect, @NonNull User user) {
        if (getHook() != null) {
            return getHook().getPotionEffectDisplay(effect, user);
        }

        String name = getPotionEffectName(effect.getType(), user);
        String ampl = getEffectAmplifierName(effect.getAmplifier(), user);

        if (ampl.length() > 0) {
            name = name + " " + ampl;
        }

        int duration = effect.getDuration();
        if (duration > 20) {
            int m = duration / 20 / 60;
            int s = duration / 20 % 60;
            String ts = String.format("%d:%02d", m, s);
            name = String.format("%s (%s)", name, ts);
        }

        return name;
    }

    /**
     * Translate the type name of tropical fish.
     *
     * @param fishPattern The type of tropical fish.
     * @param user        The user's language will be used for translation.
     * @return The translated name of the tropical fish type.
     */
    @NonNull
    public static String getTropicalFishTypeName(TropicalFish.@NonNull Pattern fishPattern, @NonNull User user) {
        return getHook() != null
            ? getHook().getTropicalFishTypeName(fishPattern, user)
            : Util.prettifyText(fishPattern.name());
    }

    /**
     * Get the names of 22 predefined tropical fish according to the
     * 'variant' tag of TropicalFish.
     *
     * @param meta Metadata carrying information about tropical fish.
     * @param user The return value is localized according to the
     *             user's locale.
     * @return If variant is predefined, return the name of the
     *         tropical fish, otherwise return null.
     */
    @Nullable
    public static String getPredefinedTropicalFishName(@NonNull TropicalFishBucketMeta meta, @NonNull User user) {
        if (getHook() != null) {
            return getHook().getPredefinedTropicalFishName(meta, user);
        }

        if (meta.hasVariant()) {
            TropicalFish.Pattern pattern = meta.getPattern();

            // https://minecraft.gamepedia.com/Tropical_Fish#Entity_data

            int type = pattern.ordinal() > 5 ? 1 : 0;
            int patt = pattern.ordinal() % 6;
            int bcol = meta.getBodyColor().ordinal();
            int pcol = meta.getPatternColor().ordinal();

            int variant = (pcol & 255) << 24 | (bcol & 255) << 16 | (patt & 255) << 8 | type;

            switch (variant) {
                case 117506305: return "Anemone";
                case 117899265: return "Black Tang";
                case 185008129: return "Blue Tang";
                case 117441793: return "Butterflyfish";
                case 118161664: return "Cichlid";
                case 65536    : return "Clownfish";
                case 50726144 : return "Cotton Candy Betta";
                case 67764993 : return "Dottyback";
                case 234882305: return "Emperor Red Snapper";
                case 67110144 : return "Goatfish";
                case 117441025: return "Moorish Idol";
                case 16778497 : return "Ornate Butterflyfish";
                case 101253888: return "Parrotfish";
                case 50660352 : return "Queen Angelfish";
                case 918529   : return "Red Cichlid";
                case 235340288: return "Red Lipped Blenny";
                case 918273   : return "Red Snapper";
                case 67108865 : return "Threadfin";
                case 917504   : return "Tomato Clownfish";
                case 459008   : return "Triggerfish";
                case 67699456 : return "Yellowtail Parrotfish";
                case 67371009 : return "Yellow Tang";
                default       : break;
            }
        }
        return null;
    }

    /**
     * Translate the name of the dye color.
     *
     * @param color The color of the dye.
     * @param user  The user's language will be used for translation.
     * @return The name of the dye color that has been translated.
     */
    @NonNull
    public static String getDyeColorName(@NonNull DyeColor color, @NonNull User user) {
        return getHook() != null
            ? getHook().getDyeColorName(color, user)
            : Util.prettifyText(color.name());
    }

    /**
     * Translate merchant's level name.
     *
     * @param level The merchant's level.
     * @param user  The user's language will be used for translation.
     * @return Translated name of merchant's level.
     */
    @NonNull
    public static String getVillagerLevelName(int level, @NonNull User user) {
        return getHook() != null
            ? getHook().getVillagerLevelName(level, user)
            : Integer.toString(level);
    }

    /**
     * Translate the profession name of the villager.
     *
     * @param profession The villager's profession.
     * @param user       The user's language will be used for translation.
     * @return The translated profession name of the villager.
     */
    @NonNull
    public static String getVillagerProfessionName(Villager.@NonNull Profession profession, @NonNull User user) {
        return getHook() != null
            ? getHook().getVillagerProfessionName(profession, user)
            : Util.prettifyText(profession.name());
    }

    /**
     * Translate the name of the banner pattern.
     *
     * @param pattern Contains the color banner pattern.
     * @param user    The user's language will be used for translation.
     * @return The translated name of banner pattern.
     */
    @NonNull
    public static String getBannerPatternName(@NonNull Pattern pattern, @NonNull User user) {
        return getHook() != null
            ? getHook().getBannerPatternName(pattern, user)
            : pattern.getColor().name().toLowerCase(Locale.ROOT)
                + "_"
                + pattern.getPattern().name().toLowerCase(Locale.ROOT);
    }

    /**
     * Get the description of the music disk.
     *
     * @param material Material for music records.
     * @param user     The user's language will be used for translation.
     * @return If the given material is a music disk, the description of the
     *         music disk is returned. Otherwise, return NULL.
     */
    @Nullable
    public static String getMusicDiskDesc(@NonNull Material material, @NonNull User user) {
        if (getHook() != null) {
            return getHook().getMusicDiskDesc(material, user);
        }
        // Without LangUtils, English is returned.
        switch (material) {
            case MUSIC_DISC_13      : return "C418 - 13";
            case MUSIC_DISC_CAT     : return "C418 - cat";
            case MUSIC_DISC_BLOCKS  : return "C418 - blocks";
            case MUSIC_DISC_CHIRP   : return "C418 - chirp";
            case MUSIC_DISC_FAR     : return "C418 - far";
            case MUSIC_DISC_MALL    : return "C418 - mall";
            case MUSIC_DISC_MELLOHI : return "C418 - mellohi";
            case MUSIC_DISC_STAL    : return "C418 - stal";
            case MUSIC_DISC_STRAD   : return "C418 - strad";
            case MUSIC_DISC_WARD    : return "C418 - ward";
            case MUSIC_DISC_11      : return "C418 - 11";
            case MUSIC_DISC_WAIT    : return "C418 - wait";
            case MUSIC_DISC_PIGSTEP : return "Lena Raine - Pigstep";
            default                 : return null;
        }
    }

    /**
     * Get the description of the Banner-Pattern added in Minecraft 1.14 and
     * above.
     *
     * @param material Material for banner pattern items.
     * @param user     The user's language will be used for translation.
     * @return The description of the Banner-Pattern item.
     */
    @Nullable
    public static String getNewBannerPatternDesc(@NonNull Material material, @NonNull User user) {
        if (getHook() != null) {
            return getHook().getNewBannerPatternDesc(material, user);
        }
        // Without LangUtils, English is returned.
        switch (material) {
            case FLOWER_BANNER_PATTERN  : return "Flower Charge";
            case CREEPER_BANNER_PATTERN : return "Creeper Charge";
            case SKULL_BANNER_PATTERN   : return "Skull Charge";
            case MOJANG_BANNER_PATTERN  : return "Thing";
            case GLOBE_BANNER_PATTERN   : return "Globe";
            case PIGLIN_BANNER_PATTERN  : return "Snout";
            default                     : return null;
        }
    }

}
