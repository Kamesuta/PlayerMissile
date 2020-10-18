package net.teamfruit.playermissile;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class PlayerMissile extends JavaPlugin implements Listener {
    private final NamespacedKey arrowKey = new NamespacedKey(this, "missile_arrow");
    private final NamespacedKey boolKey = new NamespacedKey(this, "missile_arrow_data");

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this, this);

        // Config
        saveDefaultConfig();

        // Result Item
        ItemStack configItem = getConfig().getItemStack("item");
        assert configItem != null;
        ItemStack result = configItem.clone();

        // Result Item Meta
        ItemMeta meta = result.getItemMeta();
        meta.getPersistentDataContainer().set(boolKey, PersistentDataType.BYTE, (byte) 1);
        result.setItemMeta(meta);

        // Recipe Items
        List<?> configRecipe = getConfig().getList("recipe");
        assert configRecipe != null;
        List<Material> materials = configRecipe.stream().map(String.class::cast).map(Material::valueOf).collect(Collectors.toList());

        // Recipe
        ShapelessRecipe recipe = new ShapelessRecipe(arrowKey, result);
        materials.forEach(recipe::addIngredient);
        getServer().addRecipe(recipe);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        // Recipe
        getServer().removeRecipe(arrowKey);
    }

    @EventHandler
    public void onArrowShoot(EntityShootBowEvent event) {
        // Source Entity Validation
        LivingEntity source = event.getEntity();
        if (!(source instanceof Player))
            return;
        Player player = (Player) source;

        // Arrow Entity Validation
        Entity projectile = event.getProjectile();
        if (projectile.getType() != EntityType.ARROW)
            return;
        if (!(projectile instanceof Arrow))
            return;
        Arrow arrow = (Arrow) projectile;

        // Source Player Inventory Validation
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand.getType() != Material.BOW)
            return;

        // Arrow Item Validation
        ItemStack item = event.getArrowItem();
        PersistentDataContainer persistent = item.getItemMeta().getPersistentDataContainer();
        if (!persistent.has(boolKey, PersistentDataType.BYTE) || persistent.get(boolKey, PersistentDataType.BYTE) != 1)
            return;

        // Permission Validation
        if (!player.hasPermission("playermissile.use")) {
            player.sendActionBar(getConfig().getString("message.nopermission"));
            event.setCancelled(true);
            return;
        }

        // Infinity Use
        if (!player.hasPermission("playermissile.infinity") && player.getVehicle() != null) {
            player.sendActionBar(getConfig().getString("message.noinfpermission"));
            event.setCancelled(true);
            return;
        }

        // Arrow Entity Settings
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrow.getPersistentDataContainer().set(boolKey, PersistentDataType.BYTE, (byte) 1);

        // Mount
        Optional.ofNullable(player.getVehicle()).ifPresent(e -> e.removePassenger(player));
        projectile.addPassenger(player);
    }

    @EventHandler
    public void onArrowEnd(ProjectileHitEvent event) {
        // Arrow Entity Validation
        Entity projectile = event.getEntity();
        if (projectile.getType() != EntityType.ARROW)
            return;
        if (!(projectile instanceof Arrow))
            return;
        Arrow arrow = (Arrow) projectile;

        // Source Entity Validation
        ProjectileSource source = arrow.getShooter();
        if (!(source instanceof Player))
            return;
        Player player = (Player) source;

        // Arrow Entity Validation
        PersistentDataContainer persistent = arrow.getPersistentDataContainer();
        if (!persistent.has(boolKey, PersistentDataType.BYTE) || persistent.get(boolKey, PersistentDataType.BYTE) != 1)
            return;

        // Dismount
        projectile.removePassenger(player);
    }

}
