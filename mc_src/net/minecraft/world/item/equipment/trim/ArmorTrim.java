package net.minecraft.world.item.equipment.trim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.equipment.EquipmentModel;

public record ArmorTrim(Holder<TrimMaterial> material, Holder<TrimPattern> pattern, boolean showInTooltip) implements TooltipProvider {
	public static final Codec<ArmorTrim> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				TrimMaterial.CODEC.fieldOf("material").forGetter(ArmorTrim::material),
				TrimPattern.CODEC.fieldOf("pattern").forGetter(ArmorTrim::pattern),
				Codec.BOOL.optionalFieldOf("show_in_tooltip", true).forGetter(arg -> arg.showInTooltip)
			)
			.apply(instance, ArmorTrim::new)
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, ArmorTrim> STREAM_CODEC = StreamCodec.composite(
		TrimMaterial.STREAM_CODEC, ArmorTrim::material, TrimPattern.STREAM_CODEC, ArmorTrim::pattern, ByteBufCodecs.BOOL, arg -> arg.showInTooltip, ArmorTrim::new
	);
	private static final Component UPGRADE_TITLE = Component.translatable(
			Util.makeDescriptionId("item", ResourceLocation.withDefaultNamespace("smithing_template.upgrade"))
		)
		.withStyle(ChatFormatting.GRAY);

	public ArmorTrim(Holder<TrimMaterial> arg, Holder<TrimPattern> arg2) {
		this(arg, arg2, true);
	}

	private static String getColorPaletteSuffix(Holder<TrimMaterial> arg, ResourceLocation arg2) {
		String string = (String)((TrimMaterial)arg.value()).overrideArmorMaterials().get(arg2);
		return string != null ? string : ((TrimMaterial)arg.value()).assetName();
	}

	public boolean hasPatternAndMaterial(Holder<TrimPattern> arg, Holder<TrimMaterial> arg2) {
		return arg.equals(this.pattern) && arg2.equals(this.material);
	}

	public ResourceLocation getTexture(EquipmentModel.LayerType arg, ResourceLocation arg2) {
		ResourceLocation resourceLocation = this.pattern.value().assetId();
		String string = getColorPaletteSuffix(this.material, arg2);
		return resourceLocation.withPath((UnaryOperator<String>)(string2 -> "trims/entity/" + arg.getSerializedName() + "/" + string2 + "_" + string));
	}

	@Override
	public void addToTooltip(Item.TooltipContext arg, Consumer<Component> consumer, TooltipFlag arg2) {
		if (this.showInTooltip) {
			consumer.accept(UPGRADE_TITLE);
			consumer.accept(CommonComponents.space().append(this.pattern.value().copyWithStyle(this.material)));
			consumer.accept(CommonComponents.space().append(this.material.value().description()));
		}
	}

	public ArmorTrim withTooltip(boolean bl) {
		return new ArmorTrim(this.material, this.pattern, bl);
	}
}
