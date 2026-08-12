package net.minecraft.world.effect;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.Int2DoubleFunction;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.neoforge.common.extensions.IMobEffectExtension;

public class MobEffect implements FeatureElement, IMobEffectExtension {
	public static final Codec<Holder<MobEffect>> CODEC = BuiltInRegistries.MOB_EFFECT.holderByNameCodec();
	public static final StreamCodec<RegistryFriendlyByteBuf, Holder<MobEffect>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.MOB_EFFECT);
	private static final int AMBIENT_ALPHA = Mth.floor(38.25F);
	private final Map<Holder<Attribute>, MobEffect.AttributeTemplate> attributeModifiers = new Object2ObjectOpenHashMap<>();
	private final MobEffectCategory category;
	private final int color;
	private final Function<MobEffectInstance, ParticleOptions> particleFactory;
	@Nullable
	private String descriptionId;
	private int blendDurationTicks;
	private Optional<SoundEvent> soundOnAdded = Optional.empty();
	private FeatureFlagSet requiredFeatures = FeatureFlags.VANILLA_SET;

	protected MobEffect(MobEffectCategory arg, int i) {
		this.category = arg;
		this.color = i;
		this.particleFactory = argx -> {
			int ix = argx.isAmbient() ? AMBIENT_ALPHA : 255;
			return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, ARGB.color(ix, i));
		};
	}

	protected MobEffect(MobEffectCategory arg, int i, ParticleOptions arg2) {
		this.category = arg;
		this.color = i;
		this.particleFactory = arg2x -> arg2;
	}

	protected MobEffect(MobEffectCategory category, int color, Function<MobEffectInstance, ParticleOptions> particleFactory) {
		this.category = category;
		this.color = color;
		this.particleFactory = particleFactory;
	}

	public int getBlendDurationTicks() {
		return this.blendDurationTicks;
	}

	public boolean applyEffectTick(ServerLevel arg, LivingEntity arg2, int i) {
		return true;
	}

	public void applyInstantenousEffect(ServerLevel arg, @Nullable Entity arg2, @Nullable Entity arg3, LivingEntity arg4, int i, double d) {
		this.applyEffectTick(arg, arg4, i);
	}

	public boolean shouldApplyEffectTickThisTick(int i, int j) {
		return false;
	}

	public void onEffectStarted(LivingEntity arg, int i) {
	}

	public void onEffectAdded(LivingEntity arg, int i) {
		this.soundOnAdded.ifPresent(arg2 -> arg.level().playSound(null, arg.getX(), arg.getY(), arg.getZ(), arg2, arg.getSoundSource(), 1.0F, 1.0F));
	}

	public void onMobRemoved(ServerLevel arg, LivingEntity arg2, int i, Entity.RemovalReason arg3) {
	}

	public void onMobHurt(ServerLevel arg, LivingEntity arg2, int i, DamageSource arg3, float f) {
	}

	public boolean isInstantenous() {
		return false;
	}

	protected String getOrCreateDescriptionId() {
		if (this.descriptionId == null) {
			this.descriptionId = Util.makeDescriptionId("effect", BuiltInRegistries.MOB_EFFECT.getKey(this));
		}

		return this.descriptionId;
	}

	public String getDescriptionId() {
		return this.getOrCreateDescriptionId();
	}

	public Component getDisplayName() {
		return Component.translatable(this.getDescriptionId());
	}

	public MobEffectCategory getCategory() {
		return this.category;
	}

	public int getColor() {
		return this.color;
	}

	public MobEffect addAttributeModifier(Holder<Attribute> arg, ResourceLocation arg2, double d, AttributeModifier.Operation arg3) {
		this.attributeModifiers.put(arg, new MobEffect.AttributeTemplate(arg2, d, arg3));
		return this;
	}

	public MobEffect addAttributeModifier(Holder<Attribute> attribute, ResourceLocation id, AttributeModifier.Operation operation, Int2DoubleFunction curve) {
		this.attributeModifiers.put(attribute, new MobEffect.AttributeTemplate(id, curve.apply(0), operation, curve));
		return this;
	}

	public MobEffect setBlendDuration(int i) {
		this.blendDurationTicks = i;
		return this;
	}

	public void createModifiers(int i, BiConsumer<Holder<Attribute>, AttributeModifier> biConsumer) {
		this.attributeModifiers.forEach((arg, arg2) -> biConsumer.accept(arg, arg2.create(i)));
	}

	public void removeAttributeModifiers(AttributeMap arg) {
		for (Entry<Holder<Attribute>, MobEffect.AttributeTemplate> entry : this.attributeModifiers.entrySet()) {
			AttributeInstance attributeinstance = arg.getInstance((Holder<Attribute>)entry.getKey());
			if (attributeinstance != null) {
				attributeinstance.removeModifier(((MobEffect.AttributeTemplate)entry.getValue()).id());
			}
		}
	}

	public void addAttributeModifiers(AttributeMap arg, int i) {
		for (Entry<Holder<Attribute>, MobEffect.AttributeTemplate> entry : this.attributeModifiers.entrySet()) {
			AttributeInstance attributeinstance = arg.getInstance((Holder<Attribute>)entry.getKey());
			if (attributeinstance != null) {
				attributeinstance.removeModifier(((MobEffect.AttributeTemplate)entry.getValue()).id());
				attributeinstance.addPermanentModifier(((MobEffect.AttributeTemplate)entry.getValue()).create(i));
			}
		}
	}

	public boolean isBeneficial() {
		return this.category == MobEffectCategory.BENEFICIAL;
	}

	public ParticleOptions createParticleOptions(MobEffectInstance arg) {
		return (ParticleOptions)this.particleFactory.apply(arg);
	}

	public MobEffect withSoundOnAdded(SoundEvent arg) {
		this.soundOnAdded = Optional.of(arg);
		return this;
	}

	public MobEffect requiredFeatures(FeatureFlag... args) {
		this.requiredFeatures = FeatureFlags.REGISTRY.subset(args);
		return this;
	}

	@Override
	public FeatureFlagSet requiredFeatures() {
		return this.requiredFeatures;
	}

	public record AttributeTemplate(ResourceLocation id, double amount, AttributeModifier.Operation operation, @Nullable Int2DoubleFunction curve) {
		public AttributeTemplate(ResourceLocation id, double amount, AttributeModifier.Operation operation) {
			this(id, amount, operation, null);
		}

		public AttributeModifier create(int i) {
			return this.curve != null
				? new AttributeModifier(this.id, this.curve.apply(i), this.operation)
				: new AttributeModifier(this.id, this.amount * (i + 1), this.operation);
		}
	}
}
