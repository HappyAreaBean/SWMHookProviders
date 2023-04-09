package cc.happyareabean.swmhook.provider;

import cc.happyareabean.swmhook.SWMHook;
import cc.happyareabean.swmhook.constants.Constants;
import cc.happyareabean.swmhook.hook.ArenaProvider;
import cc.happyareabean.swmhook.objects.SWMHWorld;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import ro.Fr33styler.TheLab.Experiment.Experiment;
import ro.Fr33styler.TheLab.Handler.Game;
import ro.Fr33styler.TheLab.HandlerUtils.Selection;
import ro.Fr33styler.TheLab.Main;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class TheLabArenaProvider extends ArenaProvider {

	@Override
	public void addArena(SWMHWorld world) {
		List<Game> games = getTheLab().getManager().getGames().stream().filter(a -> a.getLobby() != null).toList();

		if (games.stream().noneMatch(g -> g.getLobby().getWorld().getName().equalsIgnoreCase(world.getTemplateName()))) {
			SWMHook.getInstance().getArenaProviderManager().addFailedWorld(world);
			return;
		}

		games.stream().filter(g -> g.getLobby().getWorld().getName().equalsIgnoreCase(world.getTemplateName())).findFirst().ifPresent(game -> {

			for (int i = 0; i < world.getAmount(); i++) {
				int currentNumber = i + 1;
				String toBeGenerated = world.getWorldName() + currentNumber;
				info(String.format("Adding arena: [%s] from template world [%s] to provider %s...", toBeGenerated, world.getTemplateName(), getProviderName()));
				World bukkitWorld = Bukkit.getWorld(toBeGenerated);

				try {
					Field doorField = game.getClass().getDeclaredField("door");
					Field zukField = game.getClass().getDeclaredField("dr_zukLoc");
					Field zukPathsField = game.getClass().getDeclaredField("zuk");
					Field itemsField = game.getClass().getDeclaredField("items");
					Field lobbysField = game.getClass().getDeclaredField("lobbys");
					Field endlobbysField = game.getClass().getDeclaredField("endlobbys");
					doorField.setAccessible(true);
					zukField.setAccessible(true);
					zukPathsField.setAccessible(true);
					itemsField.setAccessible(true);
					lobbysField.setAccessible(true);
					endlobbysField.setAccessible(true);

					Location lobby = game.getLobby().clone();
					Selection doorSelection = (Selection) doorField.get(game);
					Location zukLoc = ((Location) zukField.get(game)).clone();
					List<Location> zukPaths = new ArrayList<>((List<Location>) zukPathsField.get(game));
					List<Location> items = new ArrayList<>((List<Location>) itemsField.get(game));
					List<Location> lobbys = new ArrayList<>((List<Location>) lobbysField.get(game));
					List<Location> endlobbys = new ArrayList<>((List<Location>) endlobbysField.get(game));
					List<Experiment> experiments = new ArrayList<>(game.getExperiments());

					lobby.setWorld(bukkitWorld);
					zukLoc.setWorld(bukkitWorld);
					zukPaths.forEach(l -> l.setWorld(bukkitWorld));
					items.forEach(l -> l.setWorld(bukkitWorld));
					lobbys.forEach(l -> l.setWorld(bukkitWorld));
					endlobbys.forEach(l -> l.setWorld(bukkitWorld));
					experiments.forEach(e -> {
						e.getSpawns().forEach(s -> s.setWorld(bukkitWorld));
					});

					Location doorMin = doorSelection.getMin().clone();
					Location doorMax = doorSelection.getMax().clone();
					doorMin.setWorld(bukkitWorld);
					doorMax.setWorld(bukkitWorld);

					Selection newSelection = new Selection(doorMin, doorMax);

					Game newGame = new Game(
							getTheLab(),
							currentNumber,
							lobby,
							zukLoc,
							newSelection,
							lobbys,
							endlobbys,
							items,
							zukPaths,
							game.getMinPlayers()
					);

					Field exper = newGame.getClass().getDeclaredField("experiments");
					exper.setAccessible(true);
					((List<Experiment>) exper.get(newGame)).addAll(experiments);

					getTheLab().getManager().addGame(newGame);

					success(String.format("Added arena [%s].", toBeGenerated));
				} catch (Throwable ex) {
					ex.printStackTrace();
					info(String.format("Error occur when adding arena [%s].", toBeGenerated));
				}
			}
		});
	}

	@Override
	public void removeArena(SWMHWorld world) {
		if (getTheLab().getManager().getGames().stream().noneMatch(g -> g.getLobby().getWorld().getName().equals(world.getTemplateName()))) return;

		info(String.format("Removing arena [%s] in provider %s...", world.getTemplateName(), getProviderName()));

		boolean remove = getTheLab().getManager().getGames().removeIf(g -> g.getLobby().getWorld().getName().equalsIgnoreCase(world.getTemplateName()));
		if (remove)
			success(String.format("Removed arena [%s] from provider %s!", world.getTemplateName(), getProviderName()));
		else
			log(String.format("Remove arena [%s] from provider %s failed!", world.getTemplateName(), getProviderName()));
	}

	@Override
	@SneakyThrows
	public boolean isArena(World world) {
		SWMHWorld swmhWorld = SWMHook.getInstance().getWorldsList().getFromWorld(world);
		Main tl = (Main) Bukkit.getPluginManager().getPlugin("TheLab");
		List<Game> games = tl.getManager().getGames();

		if (swmhWorld == null) return false;
		if (games.stream().noneMatch(g -> g.getLobby().getWorld().getName().equalsIgnoreCase(world.getName()))) return false;

		Game game = games.stream().filter(g -> g.getLobby().getWorld().getName().equalsIgnoreCase(world.getName())).findFirst().get();

		return game.getLobby().getWorld().getName().equalsIgnoreCase(world.getName());
	}

	@Override
	public void onInitialization() {
		if (!getTheLab().getDescription().getDepend().contains(Constants.SWM)) {
			log("====================================================================");
			log(String.format("TheLab's plugin.yml does not include %s as a dependency.", Constants.SWM));
			log("This may cause problems when loading arena in TheLab plugin.");
			log("If you have problem loading arena, please manually add '" + Constants.SWM + "' as a dependency in TheLab plugin.yml.");
			log("====================================================================");
		}
	}

	@Override
	public String getProviderName() {
		return "TheLab";
	}

	@Override
	public String getProviderFileName() {
		return "database.yml";
	}

	@Override
	public String getProviderVersion() {
		return "1.0.0";
	}

	@Override
	public String getProviderAuthor() {
		return "HappyAreaBean";
	}

	private Main getTheLab() {
		return (Main) Bukkit.getPluginManager().getPlugin("TheLab");
	}
}
