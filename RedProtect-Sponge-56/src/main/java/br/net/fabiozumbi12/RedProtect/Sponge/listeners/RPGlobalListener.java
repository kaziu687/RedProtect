package br.net.fabiozumbi12.RedProtect.Sponge.listeners;

import java.util.Map;

import br.net.fabiozumbi12.RedProtect.Sponge.RPUtil;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.explosive.PrimedTNT;
import org.spongepowered.api.entity.hanging.Hanging;
import org.spongepowered.api.entity.living.Ambient;
import org.spongepowered.api.entity.living.ArmorStand;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.Villager;
import org.spongepowered.api.entity.living.animal.Animal;
import org.spongepowered.api.entity.living.golem.Golem;
import org.spongepowered.api.entity.living.monster.Creeper;
import org.spongepowered.api.entity.living.monster.Monster;
import org.spongepowered.api.entity.living.monster.Wither;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.entity.vehicle.Boat;
import org.spongepowered.api.entity.vehicle.minecart.Minecart;
import org.spongepowered.api.entity.vehicle.minecart.TNTMinecart;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.event.cause.entity.damage.source.IndirectEntityDamageSource;
import org.spongepowered.api.event.entity.CollideEntityEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.item.inventory.UseItemStackEvent;
import org.spongepowered.api.event.world.ChangeWorldWeatherEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.weather.Weathers;

import br.net.fabiozumbi12.RedProtect.Sponge.RedProtect;
import br.net.fabiozumbi12.RedProtect.Sponge.Region;
import br.net.fabiozumbi12.RedProtect.Sponge.config.RPLang;

public class RPGlobalListener{
	
	public RPGlobalListener(){
		RedProtect.get().logger.debug("default","Loaded RPGlobalListener...");
	}

	/**
	 * @param p - Player
	 * @param b - Block
	 * @param fat - 1 = Place Block | 2 = Break Block
	 * @return Boolean - Can build or not.
	 */
	private boolean bypassBuild(Player p, BlockSnapshot b, int fat) {
		return fat == 1 && RedProtect.get().cfgs.getGlobalFlagList(p.getWorld().getName() + ".if-build-false.place-blocks").contains(b.getState().getName()) || fat == 2 && RedProtect.get().cfgs.getGlobalFlagList(p.getWorld().getName() + ".if-build-false.break-blocks").contains(b.getState().getName()) || p.hasPermission("redprotect.bypass.world") || (!RedProtect.get().cfgs.needClaimToBuild(p, b) && RedProtect.get().cfgs.getGlobalFlagBool(p.getWorld().getName(),"build"));
	}

	@Listener(order = Order.FIRST, beforeModifications = true)
    public void PlayerDropItem(DropItemEvent.Dispense e, @Root Player cause){    	
    	for (Entity ent:e.getEntities()){
    		Location<World> l = ent.getLocation();
    		Region r = RedProtect.get().rm.getTopRegion(l);
	    	
	    	if (r == null && !RedProtect.get().cfgs.getGlobalFlag(l.getExtent().getName(),"player-candrop")){
	    		e.setCancelled(true);
	    	}
    	}    	
    }
	
	@Listener(order = Order.FIRST, beforeModifications = true)
    public void onItemPickup(CollideEntityEvent event, @Root Player p) {
    	for (Entity ent:event.getEntities()){
    		if (!(ent instanceof Item)){
    			continue;
    		}    		
    		Region r = RedProtect.get().rm.getTopRegion(ent.getLocation());
    		if (r == null && !RedProtect.get().cfgs.getGlobalFlag(ent.getLocation().getExtent().getName(),"player-canpickup")){
    			event.setCancelled(true);
    		}
    	}
    }
	
	@Listener(order = Order.FIRST, beforeModifications = true)
    public void onFlow(ChangeBlockEvent.Place e, @First BlockSnapshot source){		
		BlockSnapshot bfrom = e.getTransactions().get(0).getOriginal();
		
		boolean flow = RedProtect.get().cfgs.getGlobalFlag(bfrom.getLocation().get().getExtent().getName(),"liquid-flow");
		boolean allowWater = RedProtect.get().cfgs.getGlobalFlag(bfrom.getLocation().get().getExtent().getName(),"allow-changes-of","water-flow");
		boolean allowLava = RedProtect.get().cfgs.getGlobalFlag(bfrom.getLocation().get().getExtent().getName(),"allow-changes-of","lava-flow");
		boolean allowdamage = RedProtect.get().cfgs.getGlobalFlag(bfrom.getLocation().get().getExtent().getName(),"allow-changes-of","flow-damage");
				
		Region r = RedProtect.get().rm.getTopRegion(bfrom.getLocation().get());
		if (r != null){
			return;
		}

		RedProtect.get().logger.debug("blocks","Is BlockFromToEvent.Place event is to " + source.getState().getType().getName() + " from " + bfrom.getState().getType().getName());
		
		if (!flow && (source.getState().getType().equals(BlockTypes.WATER) ||
    		source.getState().getType().equals(BlockTypes.LAVA) ||
    		source.getState().getType().equals(BlockTypes.FLOWING_LAVA) ||
    		source.getState().getType().equals(BlockTypes.FLOWING_WATER))
    		){
			e.setCancelled(true);  
         	return;
		}
				
    	if (!allowWater && (
    			source.getState().getType().equals(BlockTypes.WATER) ||
    			source.getState().getType().equals(BlockTypes.FLOWING_WATER) 
    			)){
          	 e.setCancelled(true);  
          	 return;
    	}
    	if (!allowLava && (
    			source.getState().getType().equals(BlockTypes.LAVA) ||
    			source.getState().getType().equals(BlockTypes.FLOWING_LAVA)
    			)){
          	 e.setCancelled(true);  
          	 return;
    	}	    	
    	if (!allowdamage && !source.getState().getType().equals(bfrom.getState().getType()) && !bfrom.getState().getType().equals(BlockTypes.AIR)){
    		e.setCancelled(true);
        }
    }
	
	@Listener(order = Order.FIRST, beforeModifications = true)
    public void onDecay(ChangeBlockEvent.Decay e, @First BlockSnapshot source){
		BlockSnapshot bfrom = e.getTransactions().get(0).getOriginal();
		boolean allowDecay = RedProtect.get().cfgs.getGlobalFlag(bfrom.getLocation().get().getExtent().getName(),"allow-changes-of","leaves-decay");
		
		if (!allowDecay){
          	 e.setCancelled(true);
        }
	}
	
	@Listener(order = Order.FIRST, beforeModifications = true)	
	public void onBlockPlace(ChangeBlockEvent.Place e, @First Player p) {
		RedProtect.get().logger.debug("default","RPGlobalListener - Is ChangeBlockEvent event! Cancelled? " + e.isCancelled());
		
		BlockSnapshot b = e.getTransactions().get(0).getFinal();
		ItemType item = ItemTypes.NONE;
		if (p.getItemInHand(HandTypes.MAIN_HAND).isPresent()){
			item = p.getItemInHand(HandTypes.MAIN_HAND).get().getItem();
        } else if (p.getItemInHand(HandTypes.OFF_HAND).isPresent()){
        	item = p.getItemInHand(HandTypes.OFF_HAND).get().getItem();
        }
		Region r = RedProtect.get().rm.getTopRegion(e.getTransactions().get(0).getOriginal().getLocation().get());
				
		if (r != null){
			return;
		}

        if (!RPUtil.canBuildNear(p, b.getLocation().get())){
            e.setCancelled(true);
            return;
        }

		if (item.getName().contains("minecart") || item.getName().contains("boat")){
			if (!RedProtect.get().cfgs.getGlobalFlag(p.getWorld().getName(), "use-minecart") && !p.hasPermission("RedProtect.get().bypass")){
	            e.setCancelled(true);
	            RedProtect.get().logger.debug("default","RPGlobalListener - Can't place minecart/boat!");
            }
		} else {
            if (!bypassBuild(p, b, 1)){
                e.setCancelled(true);
                RedProtect.get().logger.debug("default", "RPGlobalListener - Can't Build!");
            }
		}		
	}
	
	@Listener(order = Order.FIRST, beforeModifications = true)	
	public void onChangeWeather(ChangeWorldWeatherEvent e) {
		if (!RedProtect.get().cfgs.getGlobalFlag(e.getTargetWorld().getName(),"allow-weather") && !e.getWeather().equals(Weathers.CLEAR)){
			e.setCancelled(true);
		}
	}
		
	@Listener(order = Order.FIRST, beforeModifications = true)	
	public void onPlayerInteract(InteractEvent e, @First Player p){
		RedProtect.get().logger.debug("default","RPGlobalListener - Is InteractEvent event! Cancelled? " + e.isCancelled());
		if (!e.getInteractionPoint().isPresent()){
			return;
		}
		BlockSnapshot b = p.getWorld().createSnapshot(e.getInteractionPoint().get().toInt());
		String bname = b.getState().getName().toLowerCase();
		Location<World> loc = new Location<>(p.getWorld(), e.getInteractionPoint().get());
		
		//Temporary fix until this event return wrong location
		if (new Location<>(p.getWorld(), e.getInteractionPoint().get()).getBlockY() <= 2){
			loc = p.getLocation();
		}
		
		Region r = RedProtect.get().rm.getTopRegion(loc);
		   
		if (!canInteract(p, r)){
        	e.setCancelled(true);
        }
		
		if (r != null){
			return;
		}
		
		if (bname.contains("rail") || bname.contains("water")){
            if (!RedProtect.get().cfgs.getGlobalFlag(p.getWorld().getName(),"use-minecart") && !p.hasPermission("RedProtect.get().bypass")){
        		e.setCancelled(true);
            }
        } else {
        	if (!RedProtect.get().cfgs.getGlobalFlag(p.getWorld().getName(),"interact") && !p.hasPermission("RedProtect.get().bypass")){
    			e.setCancelled(true);
    			return;
    		}
        	if (!RedProtect.get().cfgs.getGlobalFlag(p.getWorld().getName(),"build") && !p.hasPermission("RedProtect.get().bypass") 
        			&& bname.contains("leaves")){
    			e.setCancelled(true);
            }
        }	
	}
	
	@Listener(order = Order.FIRST, beforeModifications = true)	
	public void onBlockBreakGlobal(ChangeBlockEvent.Break e, @First Player p) {
		RedProtect.get().logger.debug("default","RPGlobalListener - Is BlockBreakEvent event! Cancelled? " + e.isCancelled());

		BlockSnapshot bt = e.getTransactions().get(0).getOriginal();
		BlockState b = bt.getState();
		World w = bt.getLocation().get().getExtent();
		Region r = RedProtect.get().rm.getTopRegion(bt.getLocation().get());
		if (r != null){
			return;
		}

		if (!RPUtil.canBuildNear(p, bt.getLocation().get())){
			e.setCancelled(true);
			return;
		}

		if (!bypassBuild(p, bt, 2)){
			e.setCancelled(true);
		}
	}
	
	@Listener(order = Order.FIRST, beforeModifications = true)	
    public void onBlockBurnGlobal(ChangeBlockEvent.Modify e){
		Transaction<BlockSnapshot> b = e.getTransactions().get(0);
		Region r = RedProtect.get().rm.getTopRegion(b.getOriginal().getLocation().get());
    	if (r != null){
    		return;
    	}
    	
    	if (e.getCause().first(Monster.class).isPresent()) {
            if (!RedProtect.get().cfgs.getGlobalFlag(b.getOriginal().getLocation().get().getExtent().getName(),"entity-block-damage")){
            	e.setCancelled(true);
            }
    	}
    	
		if (b.getFinal().getState().getType().equals(BlockTypes.FIRE) && !RedProtect.get().cfgs.getGlobalFlag(b.getOriginal().getLocation().get().getExtent().getName(),"fire-block-damage")){
			e.setCancelled(true);
        }
    }

	@Listener(order = Order.FIRST, beforeModifications = true)	
    public void onPlayerInteract(InteractEntityEvent e, @First Player p) {
		
        Entity ent = e.getTargetEntity();
        Location<World> l = ent.getLocation();
        Region r = RedProtect.get().rm.getTopRegion(l);
                
        if (!canInteract(p, r)){
        	e.setCancelled(true);
        }
        
        if (r != null){
			return;
		}
        
        if (ent.getType().getName().contains("minecart") || ent.getType().getName().contains("boat")){
        	if (!RedProtect.get().cfgs.getGlobalFlag(ent.getWorld().getName(),"use-minecart") && !p.hasPermission("RedProtect.get().bypass.world")) {
                e.setCancelled(true);
            }
        } else {
        	if (!RedProtect.get().cfgs.getGlobalFlag(ent.getWorld().getName(),"interact") && !p.hasPermission("RedProtect.get().bypass.world") && (!(ent instanceof Player))) {
                e.setCancelled(true);
            }
        }      
	}
			
	@Listener(order = Order.FIRST, beforeModifications = true)	
	public void onBucketUse(UseItemStackEvent.Start e, @First Player p){    	
    	Location<World> l = p.getLocation();
		Region r = RedProtect.get().rm.getTopRegion(l);	
		
		if (!canInteract(p, r)){
        	e.setCancelled(true);
        }
		/*
		if (r != null){
			return;
		}
		
    	if (!RedProtect.get().cfgs.getGlobalFlag(p.getWorld().getName(),"build") && !p.hasPermission("RedProtect.get().bypass")) {
    		e.setCancelled(true);
			return;
    	}*/
    }
	
	@Listener(order = Order.FIRST, beforeModifications = true)	
    public void onEntityDamageEntity(DamageEntityEvent e) {
		
        Entity e1 = e.getTargetEntity();
        Entity e2 = null;
        
        Region r = RedProtect.get().rm.getTopRegion(e1.getLocation());
        if (e1 instanceof Living && !(e1 instanceof Monster)){        	
        	if (r == null && RedProtect.get().cfgs.getGlobalFlag(e1.getWorld().getName(),"invincible")){
        		e.setCancelled(true);  
        	}        	
        }
        
        if (e.getCause().first(IndirectEntityDamageSource.class).isPresent()){
    		e2 = e.getCause().first(IndirectEntityDamageSource.class).get().getSource();
    		RedProtect.get().logger.debug("player","RPLayerListener: Is DamageEntityEvent event. Damager "+e2.getType().getName()); 
    	} else {
    		return;
    	}
        
        Location<World> loc = e1.getLocation();
		Region r1 = RedProtect.get().rm.getTopRegion(loc);
		if (r1 != null){
			return;
		}
		
		if (e2 instanceof Creeper || e2 instanceof PrimedTNT || e2 instanceof TNTMinecart) {
			if (e1 instanceof Player) {
                if (!RedProtect.get().cfgs.getGlobalFlag(e1.getWorld().getName(),"explosion-entity-damage")) {
                    e.setCancelled(true);
                    return;
                }
            }        
        	if (e1 instanceof Animal || e1 instanceof Villager || e1 instanceof Golem || e1 instanceof Ambient) {
            	if (!RedProtect.get().cfgs.getGlobalFlag(e1.getWorld().getName(),"explosion-entity-damage")){
                    e.setCancelled(true);
                    return;
                }
            }
        	if (e1 instanceof Monster) {
            	if (!RedProtect.get().cfgs.getGlobalFlag(e1.getWorld().getName(),"explosion-entity-damage")){
                    e.setCancelled(true);
                    return;
                }
            }
        	if (e1 instanceof Hanging || e1 instanceof ArmorStand) {
            	if (!RedProtect.get().cfgs.getGlobalFlag(e1.getWorld().getName(),"build")){
                    e.setCancelled(true);
                    return;
                }
            }
		}
        
        if (e2 instanceof Player) {
        	Player p = (Player)e2;
        	
        	if (e1 instanceof Player) {
                if (!RedProtect.get().cfgs.getGlobalFlag(e1.getWorld().getName(),"pvp") && !p.hasPermission("RedProtect.get().bypass")) {
                    e.setCancelled(true);
                    return;
                }
            }        
        	if (e1 instanceof Animal || e1 instanceof Villager || e1 instanceof Golem || e1 instanceof Ambient) {
            	if (!RedProtect.get().cfgs.getGlobalFlag(e1.getWorld().getName(),"player-hurt-passives") && !p.hasPermission("RedProtect.get().bypass")){
                    e.setCancelled(true);
                    return;
                }
            }
        	if (e1 instanceof Monster) {
            	if (!RedProtect.get().cfgs.getGlobalFlag(e1.getWorld().getName(),"player-hurt-monsters") && !p.hasPermission("RedProtect.get().bypass")){
                    e.setCancelled(true);
                    return;
                }
            }
        	
        	if (e1 instanceof Boat || e1 instanceof Minecart) {
            	if (!RedProtect.get().cfgs.getGlobalFlag(e1.getWorld().getName(),"use-minecart") && !p.hasPermission("RedProtect.get().bypass")){
        			e.setCancelled(true);
        			return;
        		}
            }
        	if (e1 instanceof Hanging || e1 instanceof ArmorStand) {
            	if (!RedProtect.get().cfgs.getGlobalFlag(e1.getWorld().getName(),"build") && !p.hasPermission("RedProtect.get().bypass")){
                    e.setCancelled(true);
                    return;
                }
            }
        }
        
        if (e2 instanceof Projectile) {
        	Projectile proj = (Projectile)e2;
        	if (proj.getShooter() instanceof Player){
        		Player p = (Player)proj.getShooter();  
        		
            	if (e1 instanceof Player) {
                    if (!RedProtect.get().cfgs.getGlobalFlag(e1.getWorld().getName(),"pvp") && !p.hasPermission("RedProtect.get().bypass")) {
                        e.setCancelled(true);
                        return;
                    }
                }        
            	if (e1 instanceof Animal || e1 instanceof Villager || e1 instanceof Golem || e1 instanceof Ambient) {
                	if (!RedProtect.get().cfgs.getGlobalFlag(e1.getWorld().getName(),"player-hurt-passives") && !p.hasPermission("RedProtect.get().bypass")){
                        e.setCancelled(true);
                        return;
                    }
                }
            	if (e1 instanceof Monster) {
                	if (!RedProtect.get().cfgs.getGlobalFlag(e1.getWorld().getName(),"player-hurt-monsters") && !p.hasPermission("RedProtect.get().bypass")){
                        e.setCancelled(true);
                        return;
                    }
                }
            	if (e1 instanceof Hanging || e1 instanceof ArmorStand) {
                	if (!RedProtect.get().cfgs.getGlobalFlag(e1.getWorld().getName(),"build") && !p.hasPermission("RedProtect.get().bypass")){
                        e.setCancelled(true);
                    }
                }
        	}        	
        }        
	}
	
	@Listener(order = Order.FIRST, beforeModifications = true)	
    public void onEntityExplode(ExplosionEvent.Detonate e) {
    	    	
    	World w = e.getTargetWorld();
        for (Location<World> b:e.getAffectedLocations()) {
            Region r = RedProtect.get().rm.getTopRegion(b);
        	if (r == null && !RedProtect.get().cfgs.getGlobalFlag(w.getName(),"entity-block-damage")){
        		e.setCancelled(true);
        		return;
        	} 
        }
    }
	
	@Listener(order = Order.FIRST, beforeModifications = true)	
    public void onFireSpread(NotifyNeighborBlockEvent  e, @First BlockSnapshot source){
		
		Map<Direction, BlockState> dirs = e.getNeighbors();
    	
    	for (Direction dir:dirs.keySet()){
    		BlockSnapshot b = source.getLocation().get().getRelative(dir).createSnapshot();
    		BlockState bstate = source.getState();
        	Region r = RedProtect.get().rm.getTopRegion(b.getLocation().get());   
    		if (r != null){
        		return;
        	}
    		
    		if ((bstate.getType().equals(BlockTypes.FIRE) || bstate.getType().getName().contains("lava")) && 
    				!RedProtect.get().cfgs.getGlobalFlag(b.getLocation().get().getExtent().getName(),"fire-spread")){
    			e.setCancelled(true);
    			return;
    		}
    	}
	}
	
	@Listener(order = Order.FIRST, beforeModifications = true)	
	@IsCancelled(Tristate.FALSE)
    public void onCreatureSpawn(SpawnEntityEvent event) {
    	
        for (Entity e: event.getEntities()){
        	if (e == null || e.getType() == null){
        		continue;
        	}
        	
        	if (e instanceof Wither && !RedProtect.get().cfgs.getGlobalFlag(e.getWorld().getName(),"spawn-wither")){ 
                event.setCancelled(true);
                return;
            }  
        	
        	if (e instanceof Monster && !RedProtect.get().cfgs.getGlobalFlag(e.getWorld().getName(),"spawn-monsters")) {
            	Location<World> l = e.getLocation();
                Region r = RedProtect.get().rm.getTopRegion(l);
                if (r == null) {
                	RedProtect.get().logger.debug("spawn","RPGlobalListener - Cancelled spawn of Monster " + e.getType().getName());
                    event.setCancelled(true);
                    return;
                }
            }
            if ((e instanceof Animal || e instanceof Villager || e instanceof Ambient || e instanceof Golem) && !RedProtect.get().cfgs.getGlobalFlag(e.getWorld().getName(),"spawn-passives")) {
            	Location<World> l = e.getLocation();
                Region r = RedProtect.get().rm.getTopRegion(l);
                if (r == null) {
                	RedProtect.get().logger.debug("spawn","RPGlobalListener - Cancelled spawn of Animal " + e.getType().getName());
                    event.setCancelled(true);                    
                    return;
                }
            }
            if (e.getType() != null){
                RedProtect.get().logger.debug("spawn","RPGlobalListener - Spawn mob " + e.getType().getName());
            }
        }         
    }
	
	private boolean canInteract(Player p, Region r){
		boolean claimRps = RedProtect.get().cfgs.getGlobalFlag(p.getWorld().getName(),"deny-item-usage","allow-on-claimed-rps");
		boolean wilderness = RedProtect.get().cfgs.getGlobalFlag(p.getWorld().getName(),"deny-item-usage","allow-on-wilderness");
		
		ItemType item = ItemTypes.NONE;
		if (p.getItemInHand(HandTypes.MAIN_HAND).isPresent()){
			item = p.getItemInHand(HandTypes.MAIN_HAND).get().getItem();
        } else if (p.getItemInHand(HandTypes.OFF_HAND).isPresent()){
        	item = p.getItemInHand(HandTypes.OFF_HAND).get().getItem();
        }
		
		//deny item usage		
    	if (!RedProtect.get().ph.hasPerm(p, "RedProtect.get().bypass") && !item.equals(ItemTypes.NONE) && RedProtect.get().cfgs.getGlobalFlagList(p.getWorld().getName(),"deny-item-usage","items").contains(item.getType().getName())){
    		if (r != null && ((!claimRps && r.canBuild(p)) || (claimRps && !r.canBuild(p)))){
    			RPLang.sendMessage(p, "playerlistener.region.cantuse");
    			return false;
    		}
    		if (r == null && !wilderness){
    			RPLang.sendMessage(p, "playerlistener.region.cantuse");
    			return false;
    		}
        }
    	return true;
	}
}
