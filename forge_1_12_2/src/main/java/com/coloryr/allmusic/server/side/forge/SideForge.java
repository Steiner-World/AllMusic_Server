package com.coloryr.allmusic.server.side.forge;

import com.coloryr.allmusic.server.AllMusicForge;
import com.coloryr.allmusic.server.TaskItem;
import com.coloryr.allmusic.server.Tasks;
import com.coloryr.allmusic.server.codec.PacketCodec;
import com.coloryr.allmusic.server.core.AllMusic;
import com.coloryr.allmusic.server.core.objs.enums.ComType;
import com.coloryr.allmusic.server.core.objs.music.MusicObj;
import com.coloryr.allmusic.server.core.objs.music.SongInfoObj;
import com.coloryr.allmusic.server.core.side.BaseSide;
import com.coloryr.allmusic.server.side.forge.event.MusicAddEvent;
import com.coloryr.allmusic.server.side.forge.event.MusicPlayEvent;
import io.netty.buffer.ByteBuf;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

public class SideForge extends BaseSide {
    @Override
    public void runTask(Runnable run) {
        AllMusicForge.server.addScheduledTask(run);
    }

    @Override
    public void runTask(Runnable run1, int delay) {
        Tasks.add(new TaskItem() {{
            tick = delay;
            run = run1;
        }});
    }

    @Override
    public boolean checkPermission(Object player, String permission) {
        return checkPermission(player);
    }

    @Override
    public boolean checkPermission(Object player) {
        if (player instanceof MinecraftServer) {
            return true;
        }
        if (player instanceof EntityPlayerMP) {
            return ((EntityPlayerMP) player).canUseCommand(2, "music");
        }

        return false;
    }

    @Override
    public boolean isPlayer(Object source) {
        return source instanceof EntityPlayerMP;
    }

    @Override
    public boolean needPlay(boolean islist) {
        for (EntityPlayerMP player : AllMusicForge.server.getPlayerList().getPlayers()) {
            if (!AllMusic.isSkip(player.getName(), null, false, islist)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<?> getPlayers() {
        return AllMusicForge.server.getPlayerList().getPlayers();
    }

    @Override
    public String getPlayerName(Object player) {
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP player1 = (EntityPlayerMP) player;
            return player1.getName();
        }

        return null;
    }

    @Override
    public String getPlayerServer(Object player) {
        return null;
    }

    @Override
    public void send(Object player, ComType type, String data, int data1) {
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP player1 = (EntityPlayerMP) player;
            send(player1, PacketCodec.pack(type, data, data1));
        }
    }

    @Override
    public Object getPlayer(String player) {
        return AllMusicForge.server.getPlayerList().getPlayerByUsername(player);
    }

    @Override
    public void sendBar(Object player, String data) {
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP player1 = (EntityPlayerMP) player;
            ForgeApi.sendBar(player1, data);
        }
    }

    @Override
    public File getFolder() {
        return new File(String.format(Locale.ROOT, "config/%s/", "allmusic"));
    }

    @Override
    public void broadcast(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        for (EntityPlayerMP player : AllMusicForge.server.getPlayerList().getPlayers()) {
            if (!AllMusic.isSkip(player.getName(), null, false)) {
                player.sendMessage(new TextComponentString(message));
            }
        }
    }

    @Override
    public void broadcastWithRun(String message, String end, String command) {
        if (message == null || message.isEmpty()) {
            return;
        }
        ForgeApi.sendMessageBqRun(message, end, command);
    }

    @Override
    public void sendMessage(Object obj, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        ICommandSender sender = (ICommandSender) obj;
        sender.sendMessage(new TextComponentString(message));
    }

    @Override
    public void sendMessageRun(Object obj, String message, String end, String command) {
        if (message == null || message.isEmpty()) {
            return;
        }
        ForgeApi.sendMessageRun(obj, message, end, command);
    }

    @Override
    public void sendMessageSuggest(Object obj, String message, String end, String command) {
        if (message == null || message.isEmpty()) {
            return;
        }
        ForgeApi.sendMessageSuggest(obj, message, end, command);
    }

    @Override
    public boolean onMusicPlay(SongInfoObj obj) {
        MusicPlayEvent event = new MusicPlayEvent(obj);
        return MinecraftForge.EVENT_BUS.post(event);
    }

    @Override
    public boolean onMusicAdd(Object obj, MusicObj music) {
        MusicAddEvent event = new MusicAddEvent(music, (ICommandSender) obj);
        return MinecraftForge.EVENT_BUS.post(event);
    }

    private void send(EntityPlayerMP players, ByteBuf data) {
        if (players == null)
            return;
        try {
            FMLProxyPacket packet = new FMLProxyPacket(new PacketBuffer(data), "allmusic:channel");
            packet.setTarget(Side.CLIENT);
            runTask(() -> AllMusicForge.channel.sendTo(packet, players));
        } catch (Exception e) {
            AllMusic.log.warning("§c数据发送发生错误");
            e.printStackTrace();
        }
    }
}

