package me.basiqueevangelist.pingspam;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;

public class PingSpam implements ModInitializer {
	@Override
	public void onInitialize() {
		LogManager.getLogger("PingSpam").info("Several people are typing...");
	}
}
