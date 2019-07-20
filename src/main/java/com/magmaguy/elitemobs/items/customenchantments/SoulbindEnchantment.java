package com.magmaguy.elitemobs.items.customenchantments;

import com.magmaguy.elitemobs.ChatColorConverter;
import com.magmaguy.elitemobs.EntityTracker;
import com.magmaguy.elitemobs.MetadataHandler;
import com.magmaguy.elitemobs.config.enchantments.EnchantmentsConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.tags.ItemTagType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SoulbindEnchantment extends CustomEnchantment {

    public static String key = "soulbind";

    public SoulbindEnchantment() {
        super(key);
    }

    public static void addEnchantment(Item item, Player player) {
        if (!EnchantmentsConfig.getEnchantment(SoulbindEnchantment.key + ".yml").isEnabled()) return;
        if (item == null) return;
        addEnchantment(item.getItemStack(), player);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!item.isValid())
                    return;
                ArmorStand soulboundPlayer = (ArmorStand) item.getLocation().getWorld().spawnEntity(
                        item.getLocation().clone().add(new Vector(0, -50, 0)), EntityType.ARMOR_STAND);
                EntityTracker.registerArmorStands(soulboundPlayer);
                soulboundPlayer.setVisible(false);
                soulboundPlayer.setMarker(true);
                soulboundPlayer.setCustomName(player.getDisplayName() + "'s");
                soulboundPlayer.setCustomNameVisible(true);
                soulboundPlayer.setGravity(false);
                new BukkitRunnable() {
                    int counter = 0;
                    Location lastLocation = item.getLocation().clone();

                    @Override
                    public void run() {
                        counter++;
                        if (counter > 20 * 60 * 3 || !item.isValid()) {
                            cancel();
                            EntityTracker.unregisterArmorStand(soulboundPlayer);
                            return;
                        }
                        if (!lastLocation.equals(item.getLocation()))
                            soulboundPlayer.teleport(item.getLocation().clone().add(new Vector(0, 0.5, 0)));
                        if (counter == 1)
                            soulboundPlayer.teleport(item.getLocation().clone().add(new Vector(0, 0.5, 0)));
                    }
                }.runTaskTimer(MetadataHandler.PLUGIN, 1, 1);
            }
        }.runTaskLater(MetadataHandler.PLUGIN, 20 * 3);

    }

    private static NamespacedKey namespacedKey = new NamespacedKey(MetadataHandler.PLUGIN, key);

    public static void addEnchantment(ItemStack itemStack, Player player) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.getCustomTagContainer().setCustomTag(namespacedKey, ItemTagType.STRING, player.getUniqueId().toString());
        List<String> lore = itemStack.getItemMeta().getLore();
        if (lore == null)
            lore = new ArrayList<>();
        lore.add(ChatColorConverter.convert("&6Soulbound to &f" + player.getDisplayName()));
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
    }

    public static boolean isValidSoulbindUser(ItemMeta itemMeta, Player player) {
        if (itemMeta == null)
            return true;
        if (!itemMeta.getCustomTagContainer().hasCustomTag(namespacedKey, ItemTagType.STRING))
            return true;
        return UUID.fromString(itemMeta.getCustomTagContainer().getCustomTag(new NamespacedKey(MetadataHandler.PLUGIN, key), ItemTagType.STRING)).equals(player.getUniqueId());
    }


    public static class SoulbindEnchantmentEvents implements Listener {
        @EventHandler(priority = EventPriority.LOWEST)
        public void onPickup(PlayerPickupItemEvent event) {
            if (isValidSoulbindUser(event.getItem().getItemStack().getItemMeta(), event.getPlayer())) return;
            event.setCancelled(true);
        }

        @EventHandler
        public void onInventoryPickup(InventoryClickEvent inventoryClickEvent) {
            if (inventoryClickEvent.getClickedInventory() == null) return;
            if (inventoryClickEvent.getClickedInventory().getType().equals(InventoryType.PLAYER)) return;
            if (!inventoryClickEvent.getWhoClicked().getType().equals(EntityType.PLAYER)) return;
            if (inventoryClickEvent.getCurrentItem() == null) return;
            if (inventoryClickEvent.getCurrentItem().getItemMeta() == null) return;
            if (isValidSoulbindUser(inventoryClickEvent.getCurrentItem().getItemMeta(), (Player) inventoryClickEvent.getWhoClicked()))
                return;
            inventoryClickEvent.setCancelled(true);
        }
    }

    public static void soulbindWatchdog() {
        new BukkitRunnable() {
            @Override
            public void run() {
                //scan through what players are wearing
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!isValidSoulbindUser(player.getInventory().getItemInMainHand().getItemMeta(), player)) {
                        player.getWorld().dropItem(player.getLocation(), player.getInventory().getItemInMainHand());
                        player.getInventory().getItemInMainHand().setAmount(0);
                    }

                    if (!isValidSoulbindUser(player.getInventory().getItemInOffHand().getItemMeta(), player)) {
                        player.getWorld().dropItem(player.getLocation(), player.getInventory().getItemInOffHand());
                        player.getInventory().getItemInOffHand().setAmount(0);
                    }

                    if (player.getInventory().getBoots() != null && !isValidSoulbindUser(player.getInventory().getBoots().getItemMeta(), player)) {
                        player.getWorld().dropItem(player.getLocation(), player.getInventory().getBoots());
                        player.getInventory().getBoots().setAmount(0);
                    }

                    if (player.getInventory().getLeggings() != null && !isValidSoulbindUser(player.getInventory().getLeggings().getItemMeta(), player)) {
                        player.getWorld().dropItem(player.getLocation(), player.getInventory().getLeggings());
                        player.getInventory().getLeggings().setAmount(0);
                    }

                    if (player.getInventory().getChestplate() != null && !isValidSoulbindUser(player.getInventory().getChestplate().getItemMeta(), player)) {
                        player.getWorld().dropItem(player.getLocation(), player.getInventory().getChestplate());
                        player.getInventory().getChestplate().setAmount(0);
                    }

                    if (player.getInventory().getHelmet() != null && !isValidSoulbindUser(player.getInventory().getHelmet().getItemMeta(), player)) {
                        player.getWorld().dropItem(player.getLocation(), player.getInventory().getHelmet());
                        player.getInventory().getHelmet().setAmount(0);
                    }
                }
            }
        }.runTaskTimer(MetadataHandler.PLUGIN, 20, 20 * 1);
    }

}
