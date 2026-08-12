package net.minecraft.world.item.trading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class MerchantOffer {
	public static final Codec<MerchantOffer> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				ItemCost.CODEC.fieldOf("buy").forGetter(arg -> arg.baseCostA),
				ItemCost.CODEC.lenientOptionalFieldOf("buyB").forGetter(arg -> arg.costB),
				ItemStack.CODEC.fieldOf("sell").forGetter(arg -> arg.result),
				Codec.INT.lenientOptionalFieldOf("uses", 0).forGetter(arg -> arg.uses),
				Codec.INT.lenientOptionalFieldOf("maxUses", 4).forGetter(arg -> arg.maxUses),
				Codec.BOOL.lenientOptionalFieldOf("rewardExp", true).forGetter(arg -> arg.rewardExp),
				Codec.INT.lenientOptionalFieldOf("specialPrice", 0).forGetter(arg -> arg.specialPriceDiff),
				Codec.INT.lenientOptionalFieldOf("demand", 0).forGetter(arg -> arg.demand),
				Codec.FLOAT.lenientOptionalFieldOf("priceMultiplier", 0.0F).forGetter(arg -> arg.priceMultiplier),
				Codec.INT.lenientOptionalFieldOf("xp", 1).forGetter(arg -> arg.xp)
			)
			.apply(instance, MerchantOffer::new)
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, MerchantOffer> STREAM_CODEC = StreamCodec.of(
		MerchantOffer::writeToStream, MerchantOffer::createFromStream
	);
	private final ItemCost baseCostA;
	private final Optional<ItemCost> costB;
	private final ItemStack result;
	private int uses;
	private final int maxUses;
	private final boolean rewardExp;
	private int specialPriceDiff;
	private int demand;
	private final float priceMultiplier;
	private final int xp;

	private MerchantOffer(ItemCost arg, Optional<ItemCost> optional, ItemStack arg2, int i, int j, boolean bl, int k, int l, float f, int m) {
		this.baseCostA = arg;
		this.costB = optional;
		this.result = arg2;
		this.uses = i;
		this.maxUses = j;
		this.rewardExp = bl;
		this.specialPriceDiff = k;
		this.demand = l;
		this.priceMultiplier = f;
		this.xp = m;
	}

	public MerchantOffer(ItemCost arg, ItemStack arg2, int i, int j, float f) {
		this(arg, Optional.empty(), arg2, i, j, f);
	}

	public MerchantOffer(ItemCost arg, Optional<ItemCost> optional, ItemStack arg2, int i, int j, float f) {
		this(arg, optional, arg2, 0, i, j, f);
	}

	public MerchantOffer(ItemCost arg, Optional<ItemCost> optional, ItemStack arg2, int i, int j, int k, float f) {
		this(arg, optional, arg2, i, j, k, f, 0);
	}

	public MerchantOffer(ItemCost arg, Optional<ItemCost> optional, ItemStack arg2, int i, int j, int k, float f, int l) {
		this(arg, optional, arg2, i, j, true, 0, l, f, k);
	}

	private MerchantOffer(MerchantOffer arg) {
		this(arg.baseCostA, arg.costB, arg.result.copy(), arg.uses, arg.maxUses, arg.rewardExp, arg.specialPriceDiff, arg.demand, arg.priceMultiplier, arg.xp);
	}

	public ItemStack getBaseCostA() {
		return this.baseCostA.itemStack();
	}

	public ItemStack getCostA() {
		return this.baseCostA.itemStack().copyWithCount(this.getModifiedCostCount(this.baseCostA));
	}

	private int getModifiedCostCount(ItemCost arg) {
		int i = arg.count();
		int j = Math.max(0, Mth.floor(i * this.demand * this.priceMultiplier));
		return Mth.clamp(i + j + this.specialPriceDiff, 1, arg.itemStack().getMaxStackSize());
	}

	public ItemStack getCostB() {
		return (ItemStack)this.costB.map(ItemCost::itemStack).orElse(ItemStack.EMPTY);
	}

	public ItemCost getItemCostA() {
		return this.baseCostA;
	}

	public Optional<ItemCost> getItemCostB() {
		return this.costB;
	}

	public ItemStack getResult() {
		return this.result;
	}

	public void updateDemand() {
		this.demand = this.demand + this.uses - (this.maxUses - this.uses);
	}

	public ItemStack assemble() {
		return this.result.copy();
	}

	public int getUses() {
		return this.uses;
	}

	public void resetUses() {
		this.uses = 0;
	}

	public int getMaxUses() {
		return this.maxUses;
	}

	public void increaseUses() {
		this.uses++;
	}

	public int getDemand() {
		return this.demand;
	}

	public void addToSpecialPriceDiff(int i) {
		this.specialPriceDiff += i;
	}

	public void resetSpecialPriceDiff() {
		this.specialPriceDiff = 0;
	}

	public int getSpecialPriceDiff() {
		return this.specialPriceDiff;
	}

	public void setSpecialPriceDiff(int i) {
		this.specialPriceDiff = i;
	}

	public float getPriceMultiplier() {
		return this.priceMultiplier;
	}

	public int getXp() {
		return this.xp;
	}

	public boolean isOutOfStock() {
		return this.uses >= this.maxUses;
	}

	public void setToOutOfStock() {
		this.uses = this.maxUses;
	}

	public boolean needsRestock() {
		return this.uses > 0;
	}

	public boolean shouldRewardExp() {
		return this.rewardExp;
	}

	public boolean satisfiedBy(ItemStack arg, ItemStack arg2) {
		if (!this.baseCostA.test(arg) || arg.getCount() < this.getModifiedCostCount(this.baseCostA)) {
			return false;
		} else {
			return !this.costB.isPresent() ? arg2.isEmpty() : ((ItemCost)this.costB.get()).test(arg2) && arg2.getCount() >= ((ItemCost)this.costB.get()).count();
		}
	}

	public boolean take(ItemStack arg, ItemStack arg2) {
		if (!this.satisfiedBy(arg, arg2)) {
			return false;
		} else {
			arg.shrink(this.getCostA().getCount());
			if (!this.getCostB().isEmpty()) {
				arg2.shrink(this.getCostB().getCount());
			}

			return true;
		}
	}

	public MerchantOffer copy() {
		return new MerchantOffer(this);
	}

	private static void writeToStream(RegistryFriendlyByteBuf arg, MerchantOffer arg2) {
		ItemCost.STREAM_CODEC.encode(arg, arg2.getItemCostA());
		ItemStack.STREAM_CODEC.encode(arg, arg2.getResult());
		ItemCost.OPTIONAL_STREAM_CODEC.encode(arg, arg2.getItemCostB());
		arg.writeBoolean(arg2.isOutOfStock());
		arg.writeInt(arg2.getUses());
		arg.writeInt(arg2.getMaxUses());
		arg.writeInt(arg2.getXp());
		arg.writeInt(arg2.getSpecialPriceDiff());
		arg.writeFloat(arg2.getPriceMultiplier());
		arg.writeInt(arg2.getDemand());
	}

	public static MerchantOffer createFromStream(RegistryFriendlyByteBuf arg) {
		ItemCost itemCost = ItemCost.STREAM_CODEC.decode(arg);
		ItemStack itemStack = ItemStack.STREAM_CODEC.decode(arg);
		Optional<ItemCost> optional = ItemCost.OPTIONAL_STREAM_CODEC.decode(arg);
		boolean bl = arg.readBoolean();
		int i = arg.readInt();
		int j = arg.readInt();
		int k = arg.readInt();
		int l = arg.readInt();
		float f = arg.readFloat();
		int m = arg.readInt();
		MerchantOffer merchantOffer = new MerchantOffer(itemCost, optional, itemStack, i, j, k, f, m);
		if (bl) {
			merchantOffer.setToOutOfStock();
		}

		merchantOffer.setSpecialPriceDiff(l);
		return merchantOffer;
	}
}
