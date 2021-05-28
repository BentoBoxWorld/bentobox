package world.bentobox.bentobox.hooks;

import com.meowj.langutils.lang.LanguageHelper;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.api.user.User;

import java.util.Map.Entry;

/**
 * @author ApacheZy
 * @since 1.7 todo: please check this.
 */
@SuppressWarnings("unused")
public class LangUtilsHook extends Hook {

    public LangUtilsHook() {
        super("LangUtils", Material.BOOK);
    }

    @Override
    public boolean hook() {
        // Because there are other plugins with the same name,
        // we should check here whether it is the plugin we need.
        Plugin plugin = getPlugin();

        if (plugin != null && plugin.isEnabled()) {
            return "tag_r72EhIAL".equals(plugin.getConfig().getString("Extra-TAG"));
        }

        return false;
    }

    @Override
    public String getFailureCause() {
        return "Unsupported version. Download the latest version: https://www.spigotmc.org/resources/langutils.89987/";
    }

    private String getUserLocale(User user) {
        return user.getLocale().toLanguageTag();
    }

    /**
     * Get the item display name.
     * <p>
     * If the item contains a custom name, return its custom name.
     * If the item itself does not have a custom name, the material
     * name will be translated and returned.
     *
     * @param item The item
     * @param user the User's locale will be used for translation.
     * @return The Display-Name of the item.
     */
    @NonNull
    public String getItemDisplayName(@NonNull ItemStack item, @NonNull User user) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return getItemName(item, user);
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
    public String getItemName(@NonNull ItemStack itemStack, @NonNull User user) {
        return LanguageHelper.getItemName(itemStack, getUserLocale(user));
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
    public String getMaterialName(@NonNull Material material, @NonNull User user) {
        return LanguageHelper.getMaterialName(material, getUserLocale(user));
    }

    /**
     * Return the display name of the entity.
     *
     * @param entity The entity
     * @param user   the User's locale will be used for translation.
     * @return The name of the entity
     */
    @NonNull
    public String getEntityDisplayName(@NonNull Entity entity, @NonNull User user) {
        return entity.getCustomName() != null
            ? entity.getCustomName()
            : getEntityName(entity, user);
    }

    /**
     * Translate the name of the entity type.
     *
     * @param entity the EntityType whose name will be translated.
     * @param user   the User's locale will be used for translation.
     * @return The translated EntityType name.
     */
    @NonNull
    public String getEntityName(@NonNull Entity entity, @NonNull User user) {
        return LanguageHelper.getEntityName(entity, getUserLocale(user));
    }

    /**
     * Translate the name of the entity type.
     *
     * @param entityType the EntityType whose name will be translated.
     * @param user       the User's locale will be used for translation.
     * @return The translated EntityType name.
     */
    @NonNull
    public String getEntityName(@NonNull EntityType entityType, @NonNull User user) {
        return LanguageHelper.getEntityName(entityType, getUserLocale(user));
    }

    /**
     * Translate the name of the Biome.
     *
     * @param biome the Biome whose name will be translated.
     * @param user  the User's locale will be used for translation.
     * @return The translated Biome name.
     */
    @NonNull
    public String getBiomeName(@NonNull Biome biome, @NonNull User user) {
        return LanguageHelper.getBiomeName(biome, getUserLocale(user));
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
    public String getEnchantDisplayName(@NonNull Enchantment enchant, int level, @NonNull User user) {
        return LanguageHelper.getEnchantmentDisplayName(enchant, level, getUserLocale(user));
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
    public String getEnchantDisplayName(@NonNull Entry<Enchantment, Integer> entry, @NonNull User user) {
        return LanguageHelper.getEnchantmentDisplayName(entry, getUserLocale(user));
    }

    /**
     * Return the name of the enchantment.
     *
     * @param enchant The enchantment.
     * @param user    The User's locale will be used for translation.
     * @return The translated enchant name.
     */
    @NonNull
    public String getEnchantName(@NonNull Enchantment enchant, @NonNull User user) {
        return LanguageHelper.getEnchantmentName(enchant, getUserLocale(user));
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
    public String getEnchantLevelName(int level, @NonNull User user) {
        return LanguageHelper.getEnchantmentLevelName(level, getUserLocale(user));
    }

    /**
     * Translate the name of the potion.
     *
     * @param potionType The type of the potion.
     * @param user       The user's language will be used for translation.
     * @return Translated potion name.
     */
    @NonNull
    public String getPotionTypeName(@NonNull PotionType potionType, @NonNull User user) {
        return LanguageHelper.getPotionName(potionType, getUserLocale(user));
    }

    /**
     * Translate the name of the splash potion.
     *
     * @param potionType The type of the splash potion.
     * @param user       The user's language will be used for translation.
     * @return Translated splash potion name.
     */
    @NonNull
    public String getSplashPotionName(@NonNull PotionType potionType, @NonNull User user) {
        return LanguageHelper.getSplashPotionName(potionType, getUserLocale(user));
    }

    /**
     * Translate the name of the lingering potion.
     *
     * @param potionType The type of lingering potion.
     * @param user       The user's language will be used for translation.
     * @return Translated lingering potion name.
     */
    @NonNull
    public String getLingeringPotionName(@NonNull PotionType potionType, @NonNull User user) {
        return LanguageHelper.getLingeringPotionName(potionType, getUserLocale(user));
    }

    /**
     * Translate the name of the tipped arrow.
     *
     * @param potionType Potion type of tipped arrow.
     * @param user       The user's language will be used for translation.
     * @return Translated tipped arrow name.
     */
    @NonNull
    public String getTippedArrowName(@NonNull PotionType potionType, @NonNull User user) {
        return LanguageHelper.getTippedArrowName(potionType, getUserLocale(user));
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
    public String getPotionBaseEffectName(@NonNull PotionType potionType, @NonNull User user) {
        return LanguageHelper.getPotionBaseEffectName(potionType, getUserLocale(user));
    }

    /**
     * Translate the name of the potion effect.
     *
     * @param effectType The potion effect.
     * @param user       The user's language will be used for translation.
     * @return Translated name of potion effect.
     */
    @NonNull
    public String getPotionEffectName(@NonNull PotionEffectType effectType, @NonNull User user) {
        return LanguageHelper.getPotionEffectName(effectType, getUserLocale(user));
    }

    /**
     * Translate the name of the potion level.
     *
     * @param amplifier The  potion level.
     * @param user      The user's language will be used for translation.
     * @return The translated name of the potion level.
     */
    @NonNull
    public String getEffectAmplifierName(int amplifier, @NonNull User user) {
        return LanguageHelper.getEffectAmplifierName(amplifier, getUserLocale(user));
    }

    /**
     * Make the custom potion effect like the player usually sees.
     *
     * @param effect The potion effect.
     * @param user   The user's language will be used for translation.
     * @return The translated and formatted potion effect name, level, and duration.
     */
    @NonNull
    public String getPotionEffectDisplay(@NonNull PotionEffect effect, @NonNull User user) {
        return LanguageHelper.getPotionEffectDisplay(effect, getUserLocale(user));
    }

    /**
     * Translate the type name of tropical fish.
     *
     * @param fishPattern The type of tropical fish.
     * @param user        The user's language will be used for translation.
     * @return The translated name of the tropical fish type.
     */
    @NonNull
    public String getTropicalFishTypeName(TropicalFish.@NonNull Pattern fishPattern, @NonNull User user) {
        return LanguageHelper.getTropicalFishTypeName(fishPattern, getUserLocale(user));
    }

    /**
     * Get the names of 22 predefined tropical fish according to the
     * 'variant' tag of TropicalFish.
     *
     * @param meta Metadata carrying information about tropical fish.
     * @param user The return value is localized according to the
     *             user's locale.
     * @return If variant is predefined, return the name of the
     *     tropical fish, otherwise return null.
     */
    @Nullable
    public String getPredefinedTropicalFishName(@NonNull TropicalFishBucketMeta meta, @NonNull User user) {
        return LanguageHelper.getPredefinedTropicalFishName(meta, getUserLocale(user));
    }

    /**
     * Translate the name of the dye color.
     *
     * @param color The color of the dye.
     * @param user  The user's language will be used for translation.
     * @return The name of the dye color that has been translated.
     */
    @NonNull
    public String getDyeColorName(@NonNull DyeColor color, @NonNull User user) {
        return LanguageHelper.getDyeColorName(color, getUserLocale(user));
    }

    /**
     * Translate merchant's level name.
     *
     * @param level The merchant's level.
     * @param user  The user's language will be used for translation.
     * @return Translated name of merchant's level.
     */
    @NonNull
    public String getVillagerLevelName(int level, @NonNull User user) {
        return LanguageHelper.getVillagerLevelName(level, getUserLocale(user));
    }

    /**
     * Translate the profession name of the villager.
     *
     * @param profession The villager's profession.
     * @param user       The user's language will be used for translation.
     * @return The translated profession name of the villager.
     */
    @NonNull
    public String getVillagerProfessionName(Villager.@NonNull Profession profession, @NonNull User user) {
        return LanguageHelper.getVillagerProfessionName(profession, getUserLocale(user));
    }

    /**
     * Translate the name of the banner pattern.
     *
     * @param pattern Contains the color banner pattern.
     * @param user    The user's language will be used for translation.
     * @return The translated name of banner pattern.
     */
    @NonNull
    public String getBannerPatternName(@NonNull Pattern pattern, @NonNull User user) {
        return LanguageHelper.getBannerPatternName(pattern, getUserLocale(user));
    }

    /**
     * Get the description of the music disk.
     *
     * @param material Material for music records.
     * @param user     The user's language will be used for translation.
     * @return If the given material is a music disk, the description of the
     *     music disk is returned. Otherwise, return NULL.
     */
    @Nullable
    public String getMusicDiskDesc(@NonNull Material material, @NonNull User user) {
        return LanguageHelper.getMusicDiskDesc(material, getUserLocale(user));
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
    public String getNewBannerPatternDesc(@NonNull Material material, @NonNull User user) {
        return LanguageHelper.getNewBannerPatternDesc(material, getUserLocale(user));
    }

}
