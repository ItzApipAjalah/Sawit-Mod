package net.minecraft.world.item;

import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ServerItemCooldowns extends ItemCooldowns {
	private final ServerPlayer player;

	public ServerItemCooldowns(ServerPlayer arg) {
		this.player = arg;
	}

	@Override
	protected void onCooldownStarted(ResourceLocation arg, int i) {
		super.onCooldownStarted(arg, i);
		this.player.connection.send(new ClientboundCooldownPacket(arg, i));
	}

	@Override
	protected void onCooldownEnded(ResourceLocation arg) {
		super.onCooldownEnded(arg);
		this.player.connection.send(new ClientboundCooldownPacket(arg, 0));
	}
}
