package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record SummonEntityEffect(HolderSet<EntityType<?>> entityTypes, boolean joinTeam) implements EnchantmentEntityEffect {
	public static final MapCodec<SummonEntityEffect> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
				RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).fieldOf("entity").forGetter(SummonEntityEffect::entityTypes),
				Codec.BOOL.optionalFieldOf("join_team", false).forGetter(SummonEntityEffect::joinTeam)
			)
			.apply(instance, SummonEntityEffect::new)
	);

	@Override
	public void apply(ServerLevel arg, int i, EnchantedItemInUse arg2, Entity arg3, Vec3 arg4) {
		BlockPos blockPos = BlockPos.containing(arg4);
		if (Level.isInSpawnableBounds(blockPos)) {
			Optional<Holder<EntityType<?>>> optional = this.entityTypes().getRandomElement(arg.getRandom());
			if (!optional.isEmpty()) {
				Entity entity = ((EntityType)((Holder)optional.get()).value()).spawn(arg, blockPos, EntitySpawnReason.TRIGGERED);
				if (entity != null) {
					if (entity instanceof LightningBolt lightningBolt && arg2.owner() instanceof ServerPlayer serverPlayer) {
						lightningBolt.setCause(serverPlayer);
					}

					if (this.joinTeam && arg3.getTeam() != null) {
						arg.getScoreboard().addPlayerToTeam(entity.getScoreboardName(), arg3.getTeam());
					}

					entity.moveTo(arg4.x, arg4.y, arg4.z, entity.getYRot(), entity.getXRot());
				}
			}
		}
	}

	@Override
	public MapCodec<SummonEntityEffect> codec() {
		return CODEC;
	}
}
