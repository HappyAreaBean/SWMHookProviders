package cc.happyareabean.swmhook.provider;

import cc.happyareabean.swmhook.SWMHook;
import cc.happyareabean.swmhook.hook.ArenaProvider;
import cc.happyareabean.swmhook.hook.ArenaProviderManager;
import cc.happyareabean.swmhook.objects.SWMHWorld;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.serializer.LocationSerializer;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OITCArenaProvider extends ArenaProvider {

	@Override
	public void addArena(SWMHWorld world) {

		ArenaRegistry registry = getOITC().getArenaRegistry();
		List<Arena> arenas = registry.getArenas().stream().filter(Arena::isReady).collect(Collectors.toList());

		if (arenas.stream().noneMatch(a -> a.getId().equalsIgnoreCase(world.getTemplateName()))) {
			SWMHook.getInstance().getArenaProviderManager().addFailedWorld(world);
			return;
		}

		arenas.stream().filter(a -> a.getId().equalsIgnoreCase(world.getTemplateName())).findFirst().ifPresent(arena -> {

			// Check if the arena is finished setup
			if (!arena.isReady()) {
				ArenaProviderManager.errorWhenAdding(this, world, "Arena find but is not ready. Aborting...");
				return;
			}

			FileConfiguration config = ConfigUtils.getConfig(getOITC(), "arenas");
			Location oldLobby = LocationSerializer.fromString(config.getString(String.format("instances.%s.lobbyLocation", arena.getId()))).clone();
			Location oldEnd = LocationSerializer.fromString(config.getString(String.format("instances.%s.endLocation", arena.getId()))).clone();
			List<Location> oldSpawns = config.getStringList(String.format("instances.%s.playersSpawnPoints", arena.getId()))
					.stream().map(LocationSerializer::fromString).collect(Collectors.toList());

			for (int i = 0; i < world.getAmount(); i++) {
				int currentNumber = i + 1;
				String toBeGenerated = world.getWorldName() + currentNumber;
				info(String.format("Adding arena: [%s] from template world [%s] to provider %s...", toBeGenerated, world.getTemplateName(), getProviderName()));

				World locWorld = Bukkit.getWorld(toBeGenerated);

				Location lobby = oldLobby.clone();
				Location end = oldEnd.clone();
				List<Location> playerSpawn = new ArrayList<>();

				lobby.setWorld(locWorld);
				end.setWorld(locWorld);

				oldSpawns.forEach(l -> {
					Location loc = l.clone();
					loc.setWorld(locWorld);
					playerSpawn.add(loc);
				});

				Arena newArena = new Arena(toBeGenerated);
				newArena.setReady(true);
				newArena.setMapName(arena.getMapName());
				newArena.setMaximumPlayers(arena.getMaximumPlayers());
				newArena.setMinimumPlayers(arena.getMinimumPlayers());
				newArena.setLobbyLocation(lobby);
				newArena.setEndLocation(end);
				newArena.setPlayerSpawnPoints(playerSpawn);

				registry.registerArena(newArena);
				newArena.start();

				success(String.format("Added arena [%s].", toBeGenerated));
			}
		});
	}

	@Override
	public void removeArena(SWMHWorld world) {
		ArenaRegistry registry = getOITC().getArenaRegistry();
		if (registry.getArenas().stream().noneMatch(a -> a.getLobbyLocation().getWorld().getName().equals(world.getTemplateName()))) return;

		for (int i = 0; i < world.getAmount(); i++) {
			int currentNumber = i + 1;
			String formatted = world.getTemplateName() + currentNumber;

			info(String.format("Removing arena [%s] in provider %s...", formatted, getProviderName()));

			boolean remove = registry.getArenas().removeIf(a -> a.getLobbyLocation().getWorld().getName().equalsIgnoreCase(formatted));

			if (remove)
				success(String.format("Removed arena [%s] from provider %s!", formatted, getProviderName()));
			else
				log(String.format("Remove arena [%s] from provider %s failed!", formatted, getProviderName()));
		}
	}

	@Override
	public boolean isArena(World world) {
		SWMHWorld swmhWorld = SWMHook.getInstance().getWorldsList().getFromWorld(world);
		List<Arena> arenas = getOITC().getArenaRegistry().getArenas().stream().filter(Arena::isReady).collect(Collectors.toList());

		if (swmhWorld == null) return false;
		if (arenas.stream().noneMatch(a -> a.getLobbyLocation().getWorld().getName().equalsIgnoreCase(world.getName()))) return false;

		Arena arena = arenas.stream().filter(a -> a.getLobbyLocation().getWorld().getName().equalsIgnoreCase(world.getName())).findFirst().get();

		return arena.getLobbyLocation().getWorld().getName().equalsIgnoreCase(world.getName());
	}

	@Override
	public String getProviderName() {
		return "OITC";
	}

	@Override
	public String getProviderFileName() {
		return "arenas.yml";
	}

	@Override
	public String getProviderVersion() {
		return "1.0.1";
	}

	@Override
	public String getProviderAuthor() {
		return "HappyAreaBean";
	}

	@Override
	public String getRequiredPluginVersion() {
		return "2.4.5";
	}

	private Main getOITC() {
		return (Main) Bukkit.getPluginManager().getPlugin("OITC");
	}
}
