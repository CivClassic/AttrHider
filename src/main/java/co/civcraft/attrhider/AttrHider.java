package co.civcraft.attrhider;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.WrappedDataWatcherObject;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;

public class AttrHider extends JavaPlugin {
  @Override
  public void onEnable() {
    saveDefaultConfig();

    ProtocolManager manager = ProtocolLibrary.getProtocolManager();

    if (getConfig().getBoolean("hide-item-meta")) {
      manager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.ENTITY_EQUIPMENT) {
        @Override
        public void onPacketSending(PacketEvent event) {
          PacketContainer packet = event.getPacket();
          if (event.getPlayer().hasPermission("attrhider.bypass")) {
            return;
          }

          ItemStack item = packet.getItemModifier().read(0);
          if (item == null) {
            return;
          }
          if (!shouldBeObfuscated(item.getType())) {
            // remove some meta information for all items
            ItemMeta meta = item.getItemMeta();
            if(meta != null) {
	            meta.setLore(null);
	            meta.setDisplayName(null);
	            meta.setUnbreakable(false);
	            item.setItemMeta(meta);
            }
            packet.getItemModifier().write(0, item);
            return;
          }

          // more thorough obfuscation
          Color colour = null;
          ItemMeta meta = item.getItemMeta();
          if (meta instanceof LeatherArmorMeta) {
            LeatherArmorMeta lam = (LeatherArmorMeta) item.getItemMeta();
            colour = lam.getColor();
          }
          ItemStack newItem = new ItemStack(item.getType(), 1);
          if (meta.hasEnchants()) {
            newItem.addEnchantment(Enchantment.DURABILITY, 1);
          }
          if (colour != null) {
            LeatherArmorMeta lam = (LeatherArmorMeta) item.getItemMeta();
            lam.setColor(colour);
            newItem.setItemMeta(lam);
          }
          packet.getItemModifier().write(0, newItem);
        }
      });
    }
    if (getConfig().getBoolean("hide-potion-meta")) {
      manager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.ENTITY_EQUIPMENT) {
        @Override
        public void onPacketSending(PacketEvent event) {
          PacketContainer packet = event.getPacket();
          ItemStack item = packet.getItemModifier().read(0);

          if (item == null
              || !isPotion(item.getType())
              || event.getPlayer().hasPermission("attrhider.bypass")) {
            return;
          }

          // only show the potion type
          PotionMeta meta = (PotionMeta) item.getItemMeta();
          meta.clearCustomEffects();
          PotionData base = meta.getBasePotionData();
          meta.setBasePotionData(new PotionData(base.getType()));
          item.setItemMeta(meta);

          packet.getItemModifier().write(0, item);
        }
      });
    }
    if (getConfig().getBoolean("hide-potion-effects")) {
      manager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.ENTITY_EFFECT) {
        @Override
        public void onPacketSending(PacketEvent event) {
          PacketContainer packet = event.getPacket();
          if (event.getPlayer().hasPermission("attrhider.bypass")) {
            return;
          }
          StructureModifier<Integer> ints = packet.getIntegers();
          if (event.getPlayer().getEntityId() == ints.read(0)) {
            return;
          }
          // set amplifier to 0
          packet.getBytes().write(1, (byte) 0);
          // set duration to 0
          ints.write(1, 0);
        }
      });
    }
    if (getConfig().getBoolean("hide-health")) {
      manager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.ENTITY_METADATA) {
        @Override
        public void onPacketSending(PacketEvent event) {
          PacketContainer packet = event.getPacket();
          Player player = event.getPlayer();
          if (player.hasPermission("attrhider.bypass")) {
            return;
          }
          if (packet.getMeta("special").isPresent()) {
          	return;
		  }

          Entity entity = event.getPacket().getEntityModifier(event).read(0);
          StructureModifier<List<WrappedWatchableObject>> modifier = packet.getWatchableCollectionModifier();
          List<WrappedWatchableObject> read = new ArrayList<>(modifier.read(0));

          if (entity == null || player.getUniqueId().equals(entity.getUniqueId())
            	|| entity.getEntityId() == player.getEntityId()
              || !(entity instanceof LivingEntity)
              || entity instanceof EnderDragon
              || entity instanceof Wither
              || entity.getPassengers().contains(player)) {
            return;
          }

			for (int i = 0; i < read.size(); i++) {
				WrappedWatchableObject obj = read.get(i);
				if (obj.getIndex() == 8) {
					float value = (float) obj.getValue();
					if (value > 0) {
						read.set(i, new WrappedWatchableObject(new WrappedDataWatcherObject(8, WrappedDataWatcher.Registry.get(Float.class)), 1f));
					}
				}
			}

			event.setCancelled(true);

			final PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
			container.setMeta("special", true);
			container.getEntityModifier(event).write(0, entity);
			container.getWatchableCollectionModifier().write(0, read);

			try {
				ProtocolLibrary.getProtocolManager().sendServerPacket(event.getPlayer(), container);
			} catch (final InvocationTargetException e) {
				e.printStackTrace();
			}
        }
      });
    }
  }

  private boolean shouldBeObfuscated(Material type) {
    return type == Material.DIAMOND_HELMET
        || type == Material.DIAMOND_CHESTPLATE
        || type == Material.DIAMOND_LEGGINGS
        || type == Material.DIAMOND_BOOTS
        || type == Material.IRON_HELMET
        || type == Material.IRON_CHESTPLATE
        || type == Material.IRON_LEGGINGS
        || type == Material.IRON_BOOTS
        || type == Material.GOLDEN_HELMET
        || type == Material.GOLDEN_CHESTPLATE
        || type == Material.GOLDEN_LEGGINGS
        || type == Material.GOLDEN_BOOTS
        || type == Material.LEATHER_HELMET
        || type == Material.LEATHER_CHESTPLATE
        || type == Material.LEATHER_LEGGINGS
        || type == Material.LEATHER_BOOTS
        || type == Material.DIAMOND_SWORD
        || type == Material.GOLDEN_SWORD
        || type == Material.IRON_SWORD
        || type == Material.STONE_SWORD
        || type == Material.WOODEN_SWORD
        || type == Material.DIAMOND_AXE
        || type == Material.GOLDEN_AXE
        || type == Material.IRON_AXE
        || type == Material.STONE_AXE
        || type == Material.WOODEN_AXE
        || type == Material.DIAMOND_PICKAXE
        || type == Material.GOLDEN_PICKAXE
        || type == Material.IRON_PICKAXE
        || type == Material.STONE_PICKAXE
        || type == Material.WOODEN_PICKAXE
        || type == Material.DIAMOND_SHOVEL
        || type == Material.GOLDEN_SHOVEL
        || type == Material.IRON_SHOVEL
        || type == Material.STONE_SHOVEL
        || type == Material.WOODEN_SHOVEL
        || type == Material.FIREWORK_ROCKET
        || type == Material.WRITTEN_BOOK
        || type == Material.ENCHANTED_BOOK;
  }

  private boolean isPotion(Material type) {
    return type == Material.POTION
        || type == Material.LINGERING_POTION
        || type == Material.SPLASH_POTION;
  }
}
