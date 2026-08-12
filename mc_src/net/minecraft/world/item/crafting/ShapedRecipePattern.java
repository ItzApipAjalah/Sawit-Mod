package net.minecraft.world.item.crafting;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.chars.CharArraySet;
import it.unimi.dsi.fastutil.chars.CharSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

public final class ShapedRecipePattern {
	@Deprecated
	private static final int MAX_SIZE = 3;
	public static final char EMPTY_SLOT = ' ';
	static int maxWidth = 3;
	static int maxHeight = 3;
	public static final MapCodec<ShapedRecipePattern> MAP_CODEC = ShapedRecipePattern.Data.MAP_CODEC
		.flatXmap(
			ShapedRecipePattern::unpack, arg -> (DataResult)arg.data.map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Cannot encode unpacked recipe"))
		);
	public static final StreamCodec<RegistryFriendlyByteBuf, ShapedRecipePattern> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		arg -> arg.width,
		ByteBufCodecs.VAR_INT,
		arg -> arg.height,
		Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()),
		arg -> arg.ingredients,
		ShapedRecipePattern::createFromNetwork
	);
	private final int width;
	private final int height;
	private final List<Optional<Ingredient>> ingredients;
	private final Optional<ShapedRecipePattern.Data> data;
	private final int ingredientCount;
	private final boolean symmetrical;

	public static int getMaxWidth() {
		return maxWidth;
	}

	public static int getMaxHeight() {
		return maxHeight;
	}

	public static void setCraftingSize(int width, int height) {
		if (maxWidth < width) {
			maxWidth = width;
		}

		if (maxHeight < height) {
			maxHeight = height;
		}
	}

	public ShapedRecipePattern(int i, int j, List<Optional<Ingredient>> list, Optional<ShapedRecipePattern.Data> optional) {
		this.width = i;
		this.height = j;
		this.ingredients = list;
		this.data = optional;
		this.ingredientCount = (int)list.stream().flatMap(Optional::stream).count();
		this.symmetrical = Util.isSymmetrical(i, j, list);
	}

	private static ShapedRecipePattern createFromNetwork(Integer integer, Integer integer2, List<Optional<Ingredient>> list) {
		return new ShapedRecipePattern(integer, integer2, list, Optional.empty());
	}

	public static ShapedRecipePattern of(Map<Character, Ingredient> map, String... strings) {
		return of(map, List.of(strings));
	}

	public static ShapedRecipePattern of(Map<Character, Ingredient> map, List<String> list) {
		ShapedRecipePattern.Data shapedrecipepattern$data = new ShapedRecipePattern.Data(map, list);
		return unpack(shapedrecipepattern$data).getOrThrow();
	}

	private static DataResult<ShapedRecipePattern> unpack(ShapedRecipePattern.Data arg) {
		String[] astring = shrink(arg.pattern);
		int i = astring[0].length();
		int j = astring.length;
		List<Optional<Ingredient>> list = new ArrayList(i * j);
		CharSet charset = new CharArraySet(arg.key.keySet());

		for (String s : astring) {
			for (int k = 0; k < s.length(); k++) {
				char c0 = s.charAt(k);
				Optional<Ingredient> optional;
				if (c0 == ' ') {
					optional = Optional.empty();
				} else {
					Ingredient ingredient = (Ingredient)arg.key.get(c0);
					if (ingredient == null) {
						return DataResult.error(() -> "Pattern references symbol '" + c0 + "' but it's not defined in the key");
					}

					optional = Optional.of(ingredient);
				}

				charset.remove(c0);
				list.add(optional);
			}
		}

		return !charset.isEmpty()
			? DataResult.error(() -> "Key defines symbols that aren't used in pattern: " + charset)
			: DataResult.success(new ShapedRecipePattern(i, j, list, Optional.of(arg)));
	}

	@VisibleForTesting
	static String[] shrink(List<String> list) {
		int i = Integer.MAX_VALUE;
		int j = 0;
		int k = 0;
		int l = 0;

		for (int i1 = 0; i1 < list.size(); i1++) {
			String s = (String)list.get(i1);
			i = Math.min(i, firstNonEmpty(s));
			int j1 = lastNonEmpty(s);
			j = Math.max(j, j1);
			if (j1 < 0) {
				if (k == i1) {
					k++;
				}

				l++;
			} else {
				l = 0;
			}
		}

		if (list.size() == l) {
			return new String[0];
		} else {
			String[] astring = new String[list.size() - l - k];

			for (int k1 = 0; k1 < astring.length; k1++) {
				astring[k1] = ((String)list.get(k1 + k)).substring(i, j + 1);
			}

			return astring;
		}
	}

	private static int firstNonEmpty(String string) {
		int i = 0;

		while (i < string.length() && string.charAt(i) == ' ') {
			i++;
		}

		return i;
	}

	private static int lastNonEmpty(String string) {
		int i = string.length() - 1;

		while (i >= 0 && string.charAt(i) == ' ') {
			i--;
		}

		return i;
	}

	public boolean matches(CraftingInput arg) {
		if (arg.ingredientCount() != this.ingredientCount) {
			return false;
		} else {
			if (arg.width() == this.width && arg.height() == this.height) {
				if (!this.symmetrical && this.matches(arg, true)) {
					return true;
				}

				if (this.matches(arg, false)) {
					return true;
				}
			}

			return false;
		}
	}

	private boolean matches(CraftingInput arg, boolean bl) {
		for (int i = 0; i < this.height; i++) {
			for (int j = 0; j < this.width; j++) {
				Optional<Ingredient> optional;
				if (bl) {
					optional = (Optional<Ingredient>)this.ingredients.get(this.width - j - 1 + i * this.width);
				} else {
					optional = (Optional<Ingredient>)this.ingredients.get(j + i * this.width);
				}

				ItemStack itemstack = arg.getItem(j, i);
				if (!Ingredient.testOptionalIngredient(optional, itemstack)) {
					return false;
				}
			}
		}

		return true;
	}

	public int width() {
		return this.width;
	}

	public int height() {
		return this.height;
	}

	public List<Optional<Ingredient>> ingredients() {
		return this.ingredients;
	}

	public record Data(Map<Character, Ingredient> key, List<String> pattern) {
		private static final Codec<List<String>> PATTERN_CODEC = Codec.STRING.listOf().comapFlatMap(list -> {
			if (list.size() > ShapedRecipePattern.maxHeight) {
				return DataResult.error(() -> "Invalid pattern: too many rows, %s is maximum".formatted(ShapedRecipePattern.maxHeight));
			} else if (list.isEmpty()) {
				return DataResult.error(() -> "Invalid pattern: empty pattern not allowed");
			} else {
				int i = ((String)list.getFirst()).length();

				for (String s : list) {
					if (s.length() > ShapedRecipePattern.maxWidth) {
						return DataResult.error(() -> "Invalid pattern: too many columns, %s is maximum".formatted(ShapedRecipePattern.maxWidth));
					}

					if (i != s.length()) {
						return DataResult.error(() -> "Invalid pattern: each row must be the same width");
					}
				}

				return DataResult.success(list);
			}
		}, Function.identity());
		private static final Codec<Character> SYMBOL_CODEC = Codec.STRING.comapFlatMap(string -> {
			if (string.length() != 1) {
				return DataResult.error(() -> "Invalid key entry: '" + string + "' is an invalid symbol (must be 1 character only).");
			} else {
				return " ".equals(string) ? DataResult.error(() -> "Invalid key entry: ' ' is a reserved symbol.") : DataResult.success(string.charAt(0));
			}
		}, String::valueOf);
		public static final MapCodec<ShapedRecipePattern.Data> MAP_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					ExtraCodecs.strictUnboundedMap(SYMBOL_CODEC, Ingredient.CODEC).fieldOf("key").forGetter(arg -> arg.key),
					PATTERN_CODEC.fieldOf("pattern").forGetter(arg -> arg.pattern)
				)
				.apply(instance, ShapedRecipePattern.Data::new)
		);
	}
}
