package com.tpa;
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.tpa.TPAMod.server;

class PlayersWithoutSelfSuggestionProvider implements SuggestionProvider<ServerCommandSource>
{
	@Override
	public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder)
	{
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity self = source.getPlayer();
		
		// 如果命令源不是玩家，返回空建议
		if (self == null)
		{
			return Suggestions.empty();
		}
		
		String selfName = self.getName().getString();
		Collection<String> playerNames = source.getPlayerNames();
		for (String playerName : playerNames)
		{
			if(!playerName.equals(selfName))// 排除自身
			{
				builder.suggest(playerName);// 添加建议
			}
		}
		
		return builder.buildFuture();
	}
}

public class TpaCMD
{
	// 在类中添加静态方法
	private final static SuggestionProvider<ServerCommandSource> SUGGEST_PLAYERS_WITHOUT_SELF = new PlayersWithoutSelfSuggestionProvider();
	
	public TpaCMD(CommandDispatcher<ServerCommandSource> dispatcher)
	{
		/*
		/tpa [player]
		/tpask [player]
		
		/tphere [player]
		/tpahere [player]
		
		/tpaccept [player]/[]
		/tpaaccept [player]/[]
		
		/tpacancel [player]/[]
		/tpcancel [player]/[]
		
		/tpadeny [player]/[]
		/tpdeny [player]/[]
		*/
		
		LiteralArgumentBuilder<ServerCommandSource> tpaCommand = CommandManager.literal("tpa")
				.requires(ServerCommandSource::isExecutedByPlayer)
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.suggests(SUGGEST_PLAYERS_WITHOUT_SELF)
						.executes(context -> tpaPlayer(context, EntityArgumentType.getPlayer(context, "player"))));
		
		LiteralArgumentBuilder<ServerCommandSource> tpahereCommand = CommandManager.literal("tpahere")
				.requires(ServerCommandSource::isExecutedByPlayer)
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.suggests(SUGGEST_PLAYERS_WITHOUT_SELF)
						.executes(context -> tpaHere(context, EntityArgumentType.getPlayer(context, "player"))));
		
		LiteralArgumentBuilder<ServerCommandSource> calcelCommand = CommandManager.literal("tpacancel")
				.requires(ServerCommandSource::isExecutedByPlayer)
				.executes(context -> tpaCancel(context, null))
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.suggests(SUGGEST_PLAYERS_WITHOUT_SELF)
						.executes(context -> tpaCancel(context, EntityArgumentType.getPlayer(context, "player"))));
		
		LiteralArgumentBuilder<ServerCommandSource> acceptCommand = (CommandManager.literal("tpaccept")
				.requires(ServerCommandSource::isExecutedByPlayer)
				.executes(context -> tpaAccept(context, null)))
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.suggests(SUGGEST_PLAYERS_WITHOUT_SELF)
						.executes(context -> tpaAccept(context, EntityArgumentType.getPlayer(context, "player"))));
		
		LiteralArgumentBuilder<ServerCommandSource> denyCommand = (CommandManager.literal("tpadeny")
				.requires(ServerCommandSource::isExecutedByPlayer)
				.executes(context -> tpaDeny(context, null)))
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.suggests(SUGGEST_PLAYERS_WITHOUT_SELF)
						.executes(context -> tpaDeny(context, EntityArgumentType.getPlayer(context, "player"))));
		
		dispatcher.register(tpaCommand);
		dispatcher.register(tpahereCommand);
		dispatcher.register(calcelCommand);
		dispatcher.register(acceptCommand);
		dispatcher.register(denyCommand);
	}

	private static ServerPlayerEntity Find(CommandContext<ServerCommandSource> context, ServerPlayerEntity speFind, boolean pegging)
	{
		var find = TPARequest.FindLatestRequest(new PlayerData(speFind),pegging);
		if(find == null)
		{
			context.getSource().sendError(Text.literal("§c你没有待决的请求！"));
			return null;
		}
		
		var target = find.getServerPlayerEntity();
		if(target == null)
		{
			context.getSource().sendError(Text.literal(String.format("§c玩家%s不存在，可能已经离线！",find.getName())));
			return null;
		}
		
		return target;
	}
	
	private static int tpaPlayer(CommandContext<ServerCommandSource> context, ServerPlayerEntity target)
	{
		ServerPlayerEntity source = context.getSource().getPlayer();
		
		if(source == null || target == null)
		{
			context.getSource().sendError(Text.literal("§c错误的调用者或目标"));
			return 0;
		}
		
		if(source == target)
		{
			context.getSource().sendError(Text.literal("§c你不能传送到自己！"));
			return 0;
		}
		
		Text msg = TPARequest.AddRequest(new PlayerData(source), new PlayerData(target), TpDirection.TPA_MODE);
		if (msg != null)
		{
			context.getSource().sendError(msg);
			return 0;
		}
		
		source.sendMessage(Text.literal(String.format("§a已向%s发出传送请求", target.getName().getString())));
		target.sendMessage(Text.literal(String.format("§e%s请求传送到你身边", source.getName().getString())));
		
		return 1;
	}
	
	private static int tpaHere(CommandContext<ServerCommandSource> context, ServerPlayerEntity target)
	{
		ServerPlayerEntity source = context.getSource().getPlayer();
		
		if(source == null || target == null)
		{
			context.getSource().sendError(Text.literal("§c错误的调用者或目标"));
			return 0;
		}
		
		if(source == target)
		{
			context.getSource().sendError(Text.literal("§c你不能传送到自己！"));
			return 0;
		}
		
		Text msg = TPARequest.AddRequest(new PlayerData(source), new PlayerData(target), TpDirection.TPAHERE_MODE);
		if (msg != null)
		{
			context.getSource().sendError(msg);
			return 0;
		}
		
		source.sendMessage(Text.literal(String.format("§a已向%s发出传送请求", target.getName().getString())));
		target.sendMessage(Text.literal(String.format("§e%s请求你传送到他身边", source.getName().getString())));
		
		return 1;
	}
	
	private static int tpaCancel(CommandContext<ServerCommandSource> context, ServerPlayerEntity target)
	{
		//如果没有参数指定目标玩家，则默认取消最后一个请求（根据时间戳）
		ServerPlayerEntity source = context.getSource().getPlayer();
		
		if(source == null)
		{
			context.getSource().sendError(Text.literal("§c错误的调用者"));
			return 0;
		}
		
		if (target == null)
		{
			target = Find(context,source,false);//发送者查询自己
			if(target == null)
			{
				return 0;
			}
		}
		
		if (TPARequest.RmvRequest(new PlayerData(source), new PlayerData(target)) == null)//移除自己的
		{
			context.getSource().sendError(Text.literal("§c你不能取消不存在的请求！"));
			return 0;
		}
		
		source.sendMessage(Text.literal(String.format("§a已取消向%s发出的请求", target.getName().getString())));
		target.sendMessage(Text.literal(String.format("§e%s取消了传送请求", source.getName().getString())));
		
		return 1;
	}
	
	private static int tpaAccept(CommandContext<ServerCommandSource> context, ServerPlayerEntity target)
	{
		//如果没有参数指定目标玩家，则默认同意最后一个请求（根据时间戳）
		ServerPlayerEntity source = context.getSource().getPlayer();
		
		if(source == null)
		{
			context.getSource().sendError(Text.literal("§c错误的调用者"));
			return 0;
		}
		
		if (target == null)
		{
			target = Find(context,source,true);//执行者反查发送者
			if(target == null)
			{
				return 0;
			}
		}
		
		//验证玩家是否在线
		if (target.isDisconnected())
		{
			context.getSource().sendError(Text.literal(String.format("§c%s已离线，请稍后再试！", target.getName().getString())));
			return 0;
		}
		
		//移除请求并获取传送方向
		var dir = TPARequest.RmvRequest(new PlayerData(target), new PlayerData(source));
		if (dir == null)
		{
			context.getSource().sendError(Text.literal("§c你不能接受不存在的请求！"));
			return 0;
		}
		
		//根据传送方向初始化值
		ServerPlayerEntity from, to;
		if (dir == TpDirection.TPA_MODE)
		{
			from = source;
			to = target;
		}
		else if (dir == TpDirection.TPAHERE_MODE)
		{
			from = target;
			to = source;
		}
		else
		{
			context.getSource().sendError(Text.literal("§c未知的传送模式！"));
			return 0;
		}
		
		//进行传送
		ServerWorld targetWorld = to.getServer().getWorld(to.getWorld().getRegistryKey());
		Set<PositionFlag> positionFlags = new HashSet<>();
		positionFlags.add(PositionFlag.X);
		positionFlags.add(PositionFlag.Y);
		positionFlags.add(PositionFlag.Z);
		from.teleport(targetWorld, to.getX(), to.getY(), to.getZ(), positionFlags, from.getYaw(), from.getPitch());
		
		//发送消息
		from.sendMessage(Text.literal(String.format("已传送到%s身边", to.getName().getString())));
		to.sendMessage(Text.literal(String.format("%s已传送到你身边", from.getName().getString())));
		
		return 1;
	}
	
	private static int tpaDeny(CommandContext<ServerCommandSource> context, ServerPlayerEntity target)
	{
		//如果没有参数指定目标玩家，则默认拒绝最后一个请求（根据时间戳）
		ServerPlayerEntity source = context.getSource().getPlayer();
		
		if(source == null)
		{
			context.getSource().sendError(Text.literal("§c错误的调用者"));
			return 0;
		}
		
		if (target == null)
		{
			target = Find(context, source,true);//执行者反查发送者
			if(target == null)
			{
				return 0;
			}
		}
		
		if (TPARequest.RmvRequest(new PlayerData(target), new PlayerData(source)) == null)//移除他人的
		{
			context.getSource().sendError(Text.literal("§c你不能拒绝不存在的请求！"));
			return 0;
		}
		
		source.sendMessage(Text.literal(String.format("§a已拒绝%s向你发出的请求", target.getName().getString())));
		target.sendMessage(Text.literal(String.format("§e%s拒绝了你的传送请求", source.getName().getString())));
		
		return 1;
	}
}