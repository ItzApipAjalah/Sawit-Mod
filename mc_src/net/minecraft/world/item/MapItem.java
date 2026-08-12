package net.minecraft.world.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class MapItem extends Item {
	public static final int IMAGE_WIDTH = 128;
	public static final int IMAGE_HEIGHT = 128;

	public MapItem(Item.Properties arg) {
		super(arg);
	}

	public static ItemStack create(Level arg, int i, int j, byte b, boolean bl, boolean bl2) {
		ItemStack itemstack = new ItemStack(Items.FILLED_MAP);
		MapId mapid = createNewSavedData(arg, i, j, b, bl, bl2, arg.dimension());
		itemstack.set(DataComponents.MAP_ID, mapid);
		return itemstack;
	}

	@Nullable
	public static MapItemSavedData getSavedData(@Nullable MapId arg, Level arg2) {
		return arg == null ? null : arg2.getMapData(arg);
	}

	@Nullable
	public static MapItemSavedData getSavedData(ItemStack arg, Level arg2) {
		Item map = arg.getItem();
		return map instanceof MapItem ? ((MapItem)map).getCustomMapData(arg, arg2) : null;
	}

	@Nullable
	protected MapItemSavedData getCustomMapData(ItemStack arg, Level arg2) {
		MapId mapid = arg.get(DataComponents.MAP_ID);
		return getSavedData(mapid, arg2);
	}

	private static MapId createNewSavedData(Level arg, int i, int j, int k, boolean bl, boolean bl2, ResourceKey<Level> arg2) {
		MapItemSavedData mapitemsaveddata = MapItemSavedData.createFresh(i, j, (byte)k, bl, bl2, arg2);
		MapId mapid = arg.getFreeMapId();
		arg.setMapData(mapid, mapitemsaveddata);
		return mapid;
	}

	public void update(Level arg, Entity arg2, MapItemSavedData arg3) {
		if (arg.dimension() == arg3.dimension && arg2 instanceof Player) {
			int i = 1 << arg3.scale;
			int j = arg3.centerX;
			int k = arg3.centerZ;
			int l = Mth.floor(arg2.getX() - j) / i + 64;
			int i1 = Mth.floor(arg2.getZ() - k) / i + 64;
			int j1 = 128 / i;
			if (arg.dimensionType().hasCeiling()) {
				j1 /= 2;
			}

			MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer = arg3.getHoldingPlayer((Player)arg2);
			mapitemsaveddata$holdingplayer.step++;
			BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
			BlockPos.MutableBlockPos blockpos$mutableblockpos1 = new BlockPos.MutableBlockPos();
			boolean flag = false;

			for (int k1 = l - j1 + 1; k1 < l + j1; k1++) {
				if ((k1 & 15) == (mapitemsaveddata$holdingplayer.step & 15) || flag) {
					flag = false;
					double d0 = 0.0;

					for (int l1 = i1 - j1 - 1; l1 < i1 + j1; l1++) {
						if (k1 >= 0 && l1 >= -1 && k1 < 128 && l1 < 128) {
							int i2 = Mth.square(k1 - l) + Mth.square(l1 - i1);
							boolean flag1 = i2 > (j1 - 2) * (j1 - 2);
							int j2 = (j / i + k1 - 64) * i;
							int k2 = (k / i + l1 - 64) * i;
							Multiset<MapColor> multiset = LinkedHashMultiset.create();
							LevelChunk levelchunk = arg.getChunk(SectionPos.blockToSectionCoord(j2), SectionPos.blockToSectionCoord(k2));
							if (!levelchunk.isEmpty()) {
								int l2 = 0;
								double d1 = 0.0;
								if (arg.dimensionType().hasCeiling()) {
									int i3 = j2 + k2 * 231871;
									i3 = i3 * i3 * 31287121 + i3 * 11;
									if ((i3 >> 20 & 1) == 0) {
										multiset.add(Blocks.DIRT.defaultBlockState().getMapColor(arg, BlockPos.ZERO), 10);
									} else {
										multiset.add(Blocks.STONE.defaultBlockState().getMapColor(arg, BlockPos.ZERO), 100);
									}

									d1 = 100.0;
								} else {
									for (int i4 = 0; i4 < i; i4++) {
										for (int j3 = 0; j3 < i; j3++) {
											blockpos$mutableblockpos.set(j2 + i4, 0, k2 + j3);
											int k3 = levelchunk.getHeight(Heightmap.Types.WORLD_SURFACE, blockpos$mutableblockpos.getX(), blockpos$mutableblockpos.getZ()) + 1;
											BlockState blockstate;
											if (k3 <= arg.getMinY() + 1) {
												blockstate = Blocks.BEDROCK.defaultBlockState();
											} else {
												do {
													blockpos$mutableblockpos.setY(--k3);
													blockstate = levelchunk.getBlockState(blockpos$mutableblockpos);
												} while (blockstate.getMapColor(arg, blockpos$mutableblockpos) == MapColor.NONE && k3 > arg.getMinY());

												if (k3 > arg.getMinY() && !blockstate.getFluidState().isEmpty()) {
													int l3 = k3 - 1;
													blockpos$mutableblockpos1.set(blockpos$mutableblockpos);

													BlockState blockstate1;
													do {
														blockpos$mutableblockpos1.setY(l3--);
														blockstate1 = levelchunk.getBlockState(blockpos$mutableblockpos1);
														l2++;
													} while (l3 > arg.getMinY() && !blockstate1.getFluidState().isEmpty());

													blockstate = this.getCorrectStateForFluidBlock(arg, blockstate, blockpos$mutableblockpos);
												}
											}

											arg3.checkBanners(arg, blockpos$mutableblockpos.getX(), blockpos$mutableblockpos.getZ());
											d1 += (double)k3 / (i * i);
											multiset.add(blockstate.getMapColor(arg, blockpos$mutableblockpos));
										}
									}
								}

								l2 /= i * i;
								MapColor mapcolor = Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MapColor.NONE);
								MapColor.Brightness mapcolor$brightness;
								if (mapcolor == MapColor.WATER) {
									double d2 = l2 * 0.1 + (k1 + l1 & 1) * 0.2;
									if (d2 < 0.5) {
										mapcolor$brightness = MapColor.Brightness.HIGH;
									} else if (d2 > 0.9) {
										mapcolor$brightness = MapColor.Brightness.LOW;
									} else {
										mapcolor$brightness = MapColor.Brightness.NORMAL;
									}
								} else {
									double d3 = (d1 - d0) * 4.0 / (i + 4) + ((k1 + l1 & 1) - 0.5) * 0.4;
									if (d3 > 0.6) {
										mapcolor$brightness = MapColor.Brightness.HIGH;
									} else if (d3 < -0.6) {
										mapcolor$brightness = MapColor.Brightness.LOW;
									} else {
										mapcolor$brightness = MapColor.Brightness.NORMAL;
									}
								}

								d0 = d1;
								if (l1 >= 0 && i2 < j1 * j1 && (!flag1 || (k1 + l1 & 1) != 0)) {
									flag |= arg3.updateColor(k1, l1, mapcolor.getPackedId(mapcolor$brightness));
								}
							}
						}
					}
				}
			}
		}
	}

	private BlockState getCorrectStateForFluidBlock(Level arg, BlockState arg2, BlockPos arg3) {
		FluidState fluidstate = arg2.getFluidState();
		return !fluidstate.isEmpty() && !arg2.isFaceSturdy(arg, arg3, Direction.UP) ? fluidstate.createLegacyBlock() : arg2;
	}

	private static boolean isBiomeWatery(boolean[] bls, int i, int j) {
		return bls[j * 128 + i];
	}

	public static void renderBiomePreviewMap(ServerLevel arg, ItemStack arg2) {
		MapItemSavedData mapitemsaveddata = getSavedData(arg2, arg);
		if (mapitemsaveddata != null && arg.dimension() == mapitemsaveddata.dimension) {
			int i = 1 << mapitemsaveddata.scale;
			int j = mapitemsaveddata.centerX;
			int k = mapitemsaveddata.centerZ;
			boolean[] aboolean = new boolean[16384];
			int l = j / i - 64;
			int i1 = k / i - 64;
			BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

			for (int j1 = 0; j1 < 128; j1++) {
				for (int k1 = 0; k1 < 128; k1++) {
					Holder<Biome> holder = arg.getBiome(blockpos$mutableblockpos.set((l + k1) * i, 0, (i1 + j1) * i));
					aboolean[j1 * 128 + k1] = holder.is(BiomeTags.WATER_ON_MAP_OUTLINES);
				}
			}

			for (int j2 = 1; j2 < 127; j2++) {
				for (int k2 = 1; k2 < 127; k2++) {
					int l2 = 0;

					for (int l1 = -1; l1 < 2; l1++) {
						for (int i2 = -1; i2 < 2; i2++) {
							if ((l1 != 0 || i2 != 0) && isBiomeWatery(aboolean, j2 + l1, k2 + i2)) {
								l2++;
							}
						}
					}

					MapColor.Brightness mapcolor$brightness = MapColor.Brightness.LOWEST;
					MapColor mapcolor = MapColor.NONE;
					if (isBiomeWatery(aboolean, j2, k2)) {
						mapcolor = MapColor.COLOR_ORANGE;
						if (l2 > 7 && k2 % 2 == 0) {
							switch ((j2 + (int)(Mth.sin(k2 + 0.0F) * 7.0F)) / 8 % 5) {
								case 0:
								case 4:
									mapcolor$brightness = MapColor.Brightness.LOW;
									break;
								case 1:
								case 3:
									mapcolor$brightness = MapColor.Brightness.NORMAL;
									break;
								case 2:
									mapcolor$brightness = MapColor.Brightness.HIGH;
							}
						} else if (l2 > 7) {
							mapcolor = MapColor.NONE;
						} else if (l2 > 5) {
							mapcolor$brightness = MapColor.Brightness.NORMAL;
						} else if (l2 > 3) {
							mapcolor$brightness = MapColor.Brightness.LOW;
						} else if (l2 > 1) {
							mapcolor$brightness = MapColor.Brightness.LOW;
						}
					} else if (l2 > 0) {
						mapcolor = MapColor.COLOR_BROWN;
						if (l2 > 3) {
							mapcolor$brightness = MapColor.Brightness.NORMAL;
						} else {
							mapcolor$brightness = MapColor.Brightness.LOWEST;
						}
					}

					if (mapcolor != MapColor.NONE) {
						mapitemsaveddata.setColor(j2, k2, mapcolor.getPackedId(mapcolor$brightness));
					}
				}
			}
		}
	}

	@Override
	public void inventoryTick(ItemStack arg, Level arg2, Entity arg3, int i, boolean bl) {
		if (!arg2.isClientSide) {
			MapItemSavedData mapitemsaveddata = getSavedData(arg, arg2);
			if (mapitemsaveddata != null) {
				if (arg3 instanceof Player player) {
					mapitemsaveddata.tickCarriedBy(player, arg);
				}

				if (!mapitemsaveddata.locked && (bl || arg3 instanceof Player && ((Player)arg3).getOffhandItem() == arg)) {
					this.update(arg2, arg3, mapitemsaveddata);
				}
			}
		}
	}

	@Override
	public void onCraftedPostProcess(ItemStack arg, Level arg2) {
		MapPostProcessing mappostprocessing = arg.remove(DataComponents.MAP_POST_PROCESSING);
		if (mappostprocessing != null) {
			switch (mappostprocessing) {
				case LOCK:
					lockMap(arg2, arg);
					break;
				case SCALE:
					scaleMap(arg, arg2);
			}
		}
	}

	private static void scaleMap(ItemStack arg, Level arg2) {
		MapItemSavedData mapitemsaveddata = getSavedData(arg, arg2);
		if (mapitemsaveddata != null) {
			MapId mapid = arg2.getFreeMapId();
			arg2.setMapData(mapid, mapitemsaveddata.scaled());
			arg.set(DataComponents.MAP_ID, mapid);
		}
	}

	public static void lockMap(Level arg, ItemStack arg2) {
		MapItemSavedData mapitemsaveddata = getSavedData(arg2, arg);
		if (mapitemsaveddata != null) {
			MapId mapid = arg.getFreeMapId();
			MapItemSavedData mapitemsaveddata1 = mapitemsaveddata.locked();
			arg.setMapData(mapid, mapitemsaveddata1);
			arg2.set(DataComponents.MAP_ID, mapid);
		}
	}

	@Override
	public void appendHoverText(ItemStack arg, Item.TooltipContext arg2, List<Component> list, TooltipFlag arg3) {
		MapId mapid = arg.get(DataComponents.MAP_ID);
		MapItemSavedData mapitemsaveddata = mapid != null ? arg2.mapData(mapid) : null;
		MapPostProcessing mappostprocessing = arg.get(DataComponents.MAP_POST_PROCESSING);
		if (mapitemsaveddata != null && (mapitemsaveddata.locked || mappostprocessing == MapPostProcessing.LOCK)) {
			list.add(Component.translatable("filled_map.locked", mapid.id()).withStyle(ChatFormatting.GRAY));
		}

		if (arg3.isAdvanced()) {
			if (mapitemsaveddata != null) {
				if (mappostprocessing == null) {
					list.add(getTooltipForId(mapid));
				}

				int i = mappostprocessing == MapPostProcessing.SCALE ? 1 : 0;
				int j = Math.min(mapitemsaveddata.scale + i, 4);
				list.add(Component.translatable("filled_map.scale", 1 << j).withStyle(ChatFormatting.GRAY));
				list.add(Component.translatable("filled_map.level", j, 4).withStyle(ChatFormatting.GRAY));
			} else {
				list.add(Component.translatable("filled_map.unknown").withStyle(ChatFormatting.GRAY));
			}
		}
	}

	public static Component getTooltipForId(MapId arg) {
		return Component.translatable("filled_map.id", arg.id()).withStyle(ChatFormatting.GRAY);
	}

	@Override
	public InteractionResult useOn(UseOnContext arg) {
		BlockState blockstate = arg.getLevel().getBlockState(arg.getClickedPos());
		if (blockstate.is(BlockTags.BANNERS)) {
			if (!arg.getLevel().isClientSide) {
				MapItemSavedData mapitemsaveddata = getSavedData(arg.getItemInHand(), arg.getLevel());
				if (mapitemsaveddata != null && !mapitemsaveddata.toggleBanner(arg.getLevel(), arg.getClickedPos())) {
					return InteractionResult.FAIL;
				}
			}

			return InteractionResult.SUCCESS;
		} else {
			return super.useOn(arg);
		}
	}
}
