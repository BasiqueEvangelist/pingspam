package me.basiqueevangelist.pingspam;

import me.basiqueevangelist.pingspam.commands.PingSpamCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import org.apache.logging.log4j.LogManager;

public class PingSpam implements ModInitializer {
	@Override
	public void onInitialize() {
		LogManager.getLogger("PingSpam").info("Several people are typing...");

		CommandRegistrationCallback.EVENT.register(PingSpamCommands::register);
	}
}
