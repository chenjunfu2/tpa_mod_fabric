package com.tpa;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Objects;
import java.util.UUID;

import static com.tpa.TPAMod.server;

public class PlayerData
{
	private final String name;
	private final UUID uuid;
	
	PlayerData(String name,UUID uuid)
	{
		this.name = name;
		this.uuid = uuid;
	}
	
	PlayerData(ServerPlayerEntity spe)
	{
		this.name = spe.getName().getString();
		this.uuid = spe.getUuid();
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getUuid()
	{
		return name;
	}
	
	public ServerPlayerEntity getServerPlayerEntity()
	{
		return server.getPlayerManager().getPlayer(uuid);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(name, uuid);
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}
		
		var r = (PlayerData)o;
		return name.equals(r.name) && uuid.equals(r.uuid);
	}
}
