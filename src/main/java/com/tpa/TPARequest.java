package com.tpa;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.text.Text;
import java.util.HashMap;

import static com.tpa.TPAMod.server;

class TPAData
{
	Long time;
	TpDirection direction;
	
	public TPAData(Long time,TpDirection direction)
	{
		this.time=time;
		this.direction=direction;
	}
}

public class TPARequest
{
	//key是发出请求的玩家，val是所有接受请求玩家，
	//一个玩家可以给多个玩家发出请求，所以这么设计
	//而接受请求的玩家同意请求的对象是发送者，所以
	//以发送者作为key
	//val存储一个用于查询传送的目标对象和创建时间戳
	//要求全局单例
	//请求表，用于记录哪些玩家发出了请求
	private static HashMap<PlayerData, HashMap<PlayerData,TPAData>> request = new HashMap<>();
	//反查表，用于查询哪些玩家tp了自己
	private static HashMap<PlayerData, HashMap<PlayerData,Long>> search = new HashMap<>();
	
	private static void AddSearch(PlayerData source, PlayerData target, Long time)
	{
		var s = search.computeIfAbsent(target, k -> new HashMap<>());
		s.put(source,time);
	}
	
	private static void RmvSearch(PlayerData source, PlayerData target)
	{
		var s = search.get(target);
		if(s == null)
		{
			return;
		}
		
		s.remove(source);
		if(s.isEmpty())
		{
			search.remove(target);
		}
	}
	
	
	// 超时时间（2分钟）
	private static final long TIMEOUT_MS = 2 * 60 * 1000;
	// 每个玩家的请求上限 （10个）
	private static final long REQUEST_LIMIT = 10;
	
	//返回null正常，否则返回报错信息
	public static Text AddRequest(PlayerData source, PlayerData target,TpDirection dir)
	{
		var t = request.computeIfAbsent(source, k -> new HashMap<>());
		
		//否则查找此玩家是否对目标发过请求
		if(t.containsKey(target))
		{
			//发过了，报错返回
			return Text.literal(String.format("§c你已经向%s发过请求了！",target.getName()));
		}
		
		if(t.size() < REQUEST_LIMIT)
		{
			//没发过且没到上限，塞一个
			var time = System.currentTimeMillis();
			t.put(target,new TPAData(time,dir));
			AddSearch(source,target,time);//加入反查表
		}
		else
		{
			return Text.literal(String.format("§c你发送的请求已超过%d个！", REQUEST_LIMIT));
		}
		
		return null;
	}
	
	//返回null失败
	public static TpDirection RmvRequest(PlayerData source, PlayerData target)
	{
		//检查发送者
		var t = request.get(source);
		if(t == null)
		{
			return null;
		}
		
		//移除
		TPAData ret = t.remove(target);
		RmvSearch(source,target);//移出反查表
		
		if(t.isEmpty())//此玩家没有发给别人的request了
		{
			//移除
			request.remove(source);
		}
		
		return ret.direction;
	}
	
	//pegging = true：从find查询有谁给自己发了请求
	//pegging = false：从find查询自己给谁发了请求
	//返回null失败
	public static PlayerData FindLatestRequest(PlayerData find, boolean pegging)
	{
		if (pegging)//反查，find其实是dir而不是source
		{
			var s = search.get(find);
			if(s == null)
			{
				return null;
			}
			
			//遍历s找到时间最大的那个
			var it = s.entrySet().iterator();
			if(!it.hasNext())
			{
				search.remove(find);
				return null;
			}
			
			//赋值为第一个
			var latestEntry = it.next();
			var latestTime = latestEntry.getValue();
			
			//往下找，找到一个最大的，一直替换
			while(it.hasNext())
			{
				var entry = it.next();
				if(entry.getValue() > latestTime)
				{
					latestTime = entry.getValue();
					latestEntry = entry;
				}
			}
			
			return latestEntry.getKey();
		}
		else
		{
			var t = request.get(find);
			if(t == null)
			{
				return null;
			}
			
			var it = t.entrySet().iterator();
			if(!it.hasNext())//至少得有一个
			{
				//既然都没有了，就可以删了
				request.remove(find);
				return null;
			}
			
			//赋值为第一个
			var latestEntry = it.next();
			var latestTime = latestEntry.getValue().time;
			
			
			//往下找，找到一个最大的，一直替换
			while(it.hasNext())
			{
				var entry = it.next();
				if(entry.getValue().time > latestTime)
				{
					latestTime = entry.getValue().time;
					latestEntry = entry;
				}
			}
			
			//返回
			return latestEntry.getKey();
		}
	}
	
	public static void registerTimeoutCheck()//mod初始化注册
	{
		ServerTickEvents.END_SERVER_TICK.register(
		server ->
		{
			// 每 tick 执行一次检测
			var it = request.entrySet().iterator();
			while (it.hasNext())
			{
				var entry = it.next();
				var it2 = entry.getValue().entrySet().iterator();
				while(it2.hasNext())
				{
					var entry2 = it2.next();
					long elapsed = System.currentTimeMillis() - entry2.getValue().time;
					
					if (elapsed >= TIMEOUT_MS)
					{
						// 执行超时处理
						handleTimeout(entry.getKey(), entry2.getKey());
						//从反查表删除
						RmvSearch(entry.getKey(), entry2.getKey());
						it2.remove();//删除请求
					}
				}
				
				//如果这个玩家发给所有玩家的请求都没了，则删掉
				if(entry.getValue().isEmpty())
				{
					it.remove();
				}
			}
		});
	}
	
	private static void handleTimeout(PlayerData source,PlayerData target)
	{
		//获取玩家实体
		var ps = source.getServerPlayerEntity();
		var pt = target.getServerPlayerEntity();
		
		//服务器打印
		server.sendMessage(Text.literal(String.format("%s发给%s的请求已超时过期",source.getName(),target.getName())));
		
		// 通知双方玩家
		if (ps != null && !ps.isDisconnected())
		{
			ps.sendMessage(Text.literal(String.format("§c你向%s发送的传送请求已超时",target.getName())));
		}
		if (pt != null && !pt.isDisconnected())
		{
			pt.sendMessage(Text.literal(String.format("§c%s向你发送的传送请求已过期",source.getName())));
		}
	}
	
}
