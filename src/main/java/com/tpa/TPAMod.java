package com.tpa;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TPAMod implements ModInitializer
{
	public static final String MOD_ID = "tpa-mod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static MinecraftServer server;

	@Override
	public void onInitialize()
	{
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
		{
			new TpaCMD(dispatcher);
		});
		ServerLifecycleEvents.SERVER_STARTED.register((server) ->
		{
			TPAMod.server = server;
		});
		ServerLifecycleEvents.SERVER_STOPPING.register((server) ->
		{
			TPAMod.server = null;
		});
		
		//注册事件
		TPARequest.registerTimeoutCheck();
	}
}