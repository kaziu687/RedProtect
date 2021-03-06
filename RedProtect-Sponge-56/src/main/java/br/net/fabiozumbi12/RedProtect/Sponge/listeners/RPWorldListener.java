package br.net.fabiozumbi12.RedProtect.Sponge.listeners;

import java.util.List;
import java.util.Optional;

import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.monster.Monster;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.SpawnEntityEvent.ChunkLoad;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.event.world.UnloadWorldEvent;
import org.spongepowered.api.world.World;

import br.net.fabiozumbi12.RedProtect.Sponge.RedProtect;
import br.net.fabiozumbi12.RedProtect.Sponge.Region;

public class RPWorldListener {
    
    public RPWorldListener() {
        RedProtect.get().logger.debug("world","Loaded RPEntityListener...");
    }
    
    @Listener
    public void onWorldLoad(LoadWorldEvent e) {
        World w = e.getTargetWorld();
        try {
            RedProtect.get().rm.load(w);
            RedProtect.get().cfgs.loadPerWorlds(w);  
            RedProtect.get().logger.warning("World loaded: " + w.getName());
            
        }
        catch (Exception ex) {
        	RedProtect.get().logger.severe("redprotect problem on load world:");
            ex.printStackTrace();
        }
    }
    
    @Listener
    public void onWorldUnload(UnloadWorldEvent e) {
        World w = e.getTargetWorld();
        try {
            RedProtect.get().rm.unload(w);
            RedProtect.get().logger.warning("World unloaded: " + w.getName());
        }
        catch (Exception ex) {
        	RedProtect.get().logger.severe("redprotect problem on unload world:");
            ex.printStackTrace();
        }
    }   
    
    @Listener
    public void onChunkUnload(ChunkLoad e) {
    	Optional<Entity> wOpt = e.getEntities().stream().findFirst();
    	if (!wOpt.isPresent() || !RedProtect.get().cfgs.getGlobalFlag(wOpt.get().getWorld().getName(), "remove-entities-not-allowed-to-spawn")){
    		return;
    	}
    	World w = wOpt.get().getWorld();
    	List<Entity> ents = e.getEntities();
    	for (Entity ent:ents){
    		Region entr = RedProtect.get().rm.getTopRegion(ent.getLocation());
    		if (entr != null){
    			if (!entr.canSpawnMonsters() && ent instanceof Monster){
        			ent.remove();
        		}
    		} else {
    			if (ent instanceof Monster){
    				if (!RedProtect.get().cfgs.getGlobalFlag(w.getName(), "spawn-monsters")){
    					ent.remove();
    				}    			   				
    			}
    			else if (!RedProtect.get().cfgs.getGlobalFlag(w.getName(), "spawn-passives")){
    				if (ent.getCreator().isPresent()){
    					return;
    				}
    				ent.remove();
    			} 
    		}    		
    	}
    }
}
