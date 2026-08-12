package net.minecraft.world.item.consume_effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

public record TeleportRandomlyConsumeEffect(float diameter) implements ConsumeEffect {
	private static final float DEFAULT_DIAMETER = 16.0F;
	public static final MapCodec<TeleportRandomlyConsumeEffect> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("diameter", 16.0F).forGetter(TeleportRandomlyConsumeEffect::diameter))
			.apply(instance, TeleportRandomlyConsumeEffect::new)
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, TeleportRandomlyConsumeEffect> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.FLOAT, TeleportRandomlyConsumeEffect::diameter, TeleportRandomlyConsumeEffect::new
	);

	public TeleportRandomlyConsumeEffect() {
		this(16.0F);
	}

	@Override
	public ConsumeEffect.Type<TeleportRandomlyConsumeEffect> getType() {
		return ConsumeEffect.Type.TELEPORT_RANDOMLY;
	}

	@Override
	public boolean apply(Level arg, ItemStack arg2, LivingEntity arg3) {
		boolean flag = false;

		for (int i = 0; i < 16; i++) {
			double d0 = arg3.getX() + (arg3.getRandom().nextDouble() - 0.5) * this.diameter;
			double d1 = Mth.clamp(
				arg3.getY() + (arg3.getRandom().nextDouble() - 0.5) * this.diameter,
				(double)arg.getMinY(),
				(double)(arg.getMinY() + ((ServerLevel)arg).getLogicalHeight() - 1)
			);
			double d2 = arg3.getZ() + (arg3.getRandom().nextDouble() - 0.5) * this.diameter;
			if (arg3.isPassenger()) {
				arg3.stopRiding();
			}

			Vec3 vec3 = arg3.position();
			EntityTeleportEvent.ItemConsumption event = EventHooks.onItemConsumptionTeleport(arg3, arg2, d0, d1, d2);
			if (event.isCanceled()) {
				return false;
			}

			if (arg3.randomTeleport(event.getTargetX(), event.getTargetY(), event.getTargetZ(), true)) {
				arg.gameEvent(GameEvent.TELEPORT, vec3, GameEvent.Context.of(arg3));
				SoundSource soundsource;
				SoundEvent soundevent;
				if (arg3 instanceof Fox) {
					soundevent = SoundEvents.FOX_TELEPORT;
					soundsource = SoundSource.NEUTRAL;
				} else {
					soundevent = SoundEvents.CHORUS_FRUIT_TELEPORT;
					soundsource = SoundSource.PLAYERS;
				}

				arg.playSound(null, arg3.getX(), arg3.getY(), arg3.getZ(), soundevent, soundsource);
				arg3.resetFallDistance();
				flag = true;
				break;
			}
		}

		if (flag && arg3 instanceof Player player) {
			player.resetCurrentImpulseContext();
		}

		return flag;
	}
}
