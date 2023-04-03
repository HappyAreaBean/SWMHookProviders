package cc.happyareabean.swmhook.provider;

import cc.happyareabean.swmhook.hook.ArenaProvider;
import cc.happyareabean.swmhook.objects.SWMHWorld;

public class TestArenaProvider extends ArenaProvider {

	@Override
	public void addArena(SWMHWorld world) {

	}

	@Override
	public void removeArena(SWMHWorld world) {

	}

	@Override
	public String getProviderName() {
		return "TestArenaProvider";
	}

	@Override
	public String getProviderFileName() {
		return "arenas.yml";
	}
}
