package com.civpvp.attrhider;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;

/**
 * Uses ProtocolLib to strip away stuff that should never have been sent in the first place
 * such as enchantment, durability and potion duration information.
 * @author Squeenix
 *
 */
public class AttrHider extends JavaPlugin implements Listener {
	private ProtocolManager protocolManager;

	@Override
	public void onEnable() {
	    registerPacketListeners();
	    Bukkit.getPluginManager().registerEvents(this, this);
	}
	
	private void registerPacketListeners(){
		protocolManager = ProtocolLibrary.getProtocolManager();

        //Strips armour
        protocolManager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.ENTITY_EQUIPMENT) {
            @Override
            public void onPacketSending(PacketEvent e) {
                try {
                    PacketContainer p = e.getPacket();
                    StructureModifier<ItemStack> items = p.getItemModifier();
                    ItemStack i = items.read(0);
                    if (i != null && shouldBeObfuscated(i.getType())) {
                    	Color color = null;
                    	if (i.getItemMeta() instanceof LeatherArmorMeta) {
                    		LeatherArmorMeta lam = (LeatherArmorMeta) i.getItemMeta();
                    		color = lam.getColor();
                    	}
                    	ItemStack is = new ItemStack(i.getType(), 1 , (short) 1);
                    	if (i.getItemMeta().getEnchants().values().size() != 0) {
                    		is.addEnchantment(Enchantment.DURABILITY, 1);
                    	}
                    	if (color != null) {
                    		LeatherArmorMeta lam = (LeatherArmorMeta) is.getItemMeta();
                    		lam.setColor(color);
                    		is.setItemMeta(lam);
                    	}
                        items.write(0, is);
                    }
                } catch (FieldAccessException exception) { //Should catch if the packet is the wrong type
                    exception.printStackTrace();
                }
            }
        });

        //Strips potion duration length and sets it to 420 ticks so you can blaze it
	    protocolManager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.ENTITY_EFFECT) {
	    	@Override
	    	public void onPacketSending(PacketEvent e) {
	    		try {
		    		PacketContainer p = e.getPacket();
		    		if(e.getPlayer().getEntityId() != p.getIntegers().read(0)) { //Don't hide a player's own stuff from them
		    			p.getIntegers().write(1, 420);
		    		}
	    		} catch (FieldAccessException exception) { 
	    			exception.printStackTrace();
	    		}
	    	}
	    });
	    
	    //Make reported health random
		ProtocolLibrary.getProtocolManager().addPacketListener(
		new PacketAdapter(this, ListenerPriority.NORMAL, new PacketType[] { PacketType.Play.Server.ENTITY_METADATA }) {
			public void onPacketSending(PacketEvent event) {
				try {
					Player observer = event.getPlayer();
					//Get the entity from the packet
					Entity entity = (Entity)event.getPacket().getEntityModifier(observer.getWorld()).read(0);
					//If the entity is not the observer, and the entity is alive, and the entity is not a dragon or wither,
					//and the entity is not the observer's mount
					if ((entity != null) && (observer != entity) && ((entity instanceof LivingEntity)) &&
					  (!(entity instanceof EnderDragon) && !(entity instanceof Wither)) && (entity.getPassenger() != observer)) {
						event.setPacket(event.getPacket().deepClone());
						StructureModifier<List<WrappedWatchableObject>> watcher = event.getPacket().getWatchableCollectionModifier();
					  	for (WrappedWatchableObject watch : watcher.read(0)) {
							if ((watch.getIndex() == 7) && (((Float)watch.getValue()).floatValue() > 0.0F)) {
								watch.setValue(20f);
							}
				    	}
					}
	  	        }
				catch (Exception e){
	  	          e.printStackTrace();
	  	        }
			}
		});
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
                || type == Material.GOLD_HELMET
                || type == Material.GOLD_CHESTPLATE
                || type == Material.GOLD_LEGGINGS
                || type == Material.GOLD_BOOTS
                || type == Material.LEATHER_HELMET
                || type == Material.LEATHER_CHESTPLATE
                || type == Material.LEATHER_LEGGINGS
                || type == Material.LEATHER_BOOTS
                || type == Material.DIAMOND_SWORD
                || type == Material.GOLD_SWORD
                || type == Material.IRON_SWORD
                || type == Material.STONE_SWORD
                || type == Material.WOOD_SWORD
                || type == Material.DIAMOND_AXE
                || type == Material.GOLD_AXE
                || type == Material.IRON_AXE
                || type == Material.STONE_AXE
                || type == Material.WOOD_AXE
                || type == Material.DIAMOND_PICKAXE
                || type == Material.GOLD_PICKAXE
                || type == Material.IRON_PICKAXE
                || type == Material.STONE_PICKAXE
                || type == Material.WOOD_PICKAXE
                || type == Material.DIAMOND_SPADE
                || type == Material.GOLD_SPADE
                || type == Material.IRON_SPADE
                || type == Material.STONE_SPADE
                || type == Material.WOOD_SPADE;
    }
}

