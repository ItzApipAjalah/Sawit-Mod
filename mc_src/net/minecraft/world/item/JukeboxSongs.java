package net.minecraft.world.item;

import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public interface JukeboxSongs {
	ResourceKey<JukeboxSong> THIRTEEN = create("13");
	ResourceKey<JukeboxSong> CAT = create("cat");
	ResourceKey<JukeboxSong> BLOCKS = create("blocks");
	ResourceKey<JukeboxSong> CHIRP = create("chirp");
	ResourceKey<JukeboxSong> FAR = create("far");
	ResourceKey<JukeboxSong> MALL = create("mall");
	ResourceKey<JukeboxSong> MELLOHI = create("mellohi");
	ResourceKey<JukeboxSong> STAL = create("stal");
	ResourceKey<JukeboxSong> STRAD = create("strad");
	ResourceKey<JukeboxSong> WARD = create("ward");
	ResourceKey<JukeboxSong> ELEVEN = create("11");
	ResourceKey<JukeboxSong> WAIT = create("wait");
	ResourceKey<JukeboxSong> PIGSTEP = create("pigstep");
	ResourceKey<JukeboxSong> OTHERSIDE = create("otherside");
	ResourceKey<JukeboxSong> FIVE = create("5");
	ResourceKey<JukeboxSong> RELIC = create("relic");
	ResourceKey<JukeboxSong> PRECIPICE = create("precipice");
	ResourceKey<JukeboxSong> CREATOR = create("creator");
	ResourceKey<JukeboxSong> CREATOR_MUSIC_BOX = create("creator_music_box");

	private static ResourceKey<JukeboxSong> create(String string) {
		return ResourceKey.create(Registries.JUKEBOX_SONG, ResourceLocation.withDefaultNamespace(string));
	}

	private static void register(BootstrapContext<JukeboxSong> arg, ResourceKey<JukeboxSong> arg2, Holder.Reference<SoundEvent> arg3, int i, int j) {
		arg.register(arg2, new JukeboxSong(arg3, Component.translatable(Util.makeDescriptionId("jukebox_song", arg2.location())), i, j));
	}

	static void bootstrap(BootstrapContext<JukeboxSong> arg) {
		register(arg, THIRTEEN, SoundEvents.MUSIC_DISC_13, 178, 1);
		register(arg, CAT, SoundEvents.MUSIC_DISC_CAT, 185, 2);
		register(arg, BLOCKS, SoundEvents.MUSIC_DISC_BLOCKS, 345, 3);
		register(arg, CHIRP, SoundEvents.MUSIC_DISC_CHIRP, 185, 4);
		register(arg, FAR, SoundEvents.MUSIC_DISC_FAR, 174, 5);
		register(arg, MALL, SoundEvents.MUSIC_DISC_MALL, 197, 6);
		register(arg, MELLOHI, SoundEvents.MUSIC_DISC_MELLOHI, 96, 7);
		register(arg, STAL, SoundEvents.MUSIC_DISC_STAL, 150, 8);
		register(arg, STRAD, SoundEvents.MUSIC_DISC_STRAD, 188, 9);
		register(arg, WARD, SoundEvents.MUSIC_DISC_WARD, 251, 10);
		register(arg, ELEVEN, SoundEvents.MUSIC_DISC_11, 71, 11);
		register(arg, WAIT, SoundEvents.MUSIC_DISC_WAIT, 238, 12);
		register(arg, PIGSTEP, SoundEvents.MUSIC_DISC_PIGSTEP, 149, 13);
		register(arg, OTHERSIDE, SoundEvents.MUSIC_DISC_OTHERSIDE, 195, 14);
		register(arg, FIVE, SoundEvents.MUSIC_DISC_5, 178, 15);
		register(arg, RELIC, SoundEvents.MUSIC_DISC_RELIC, 218, 14);
		register(arg, PRECIPICE, SoundEvents.MUSIC_DISC_PRECIPICE, 299, 13);
		register(arg, CREATOR, SoundEvents.MUSIC_DISC_CREATOR, 176, 12);
		register(arg, CREATOR_MUSIC_BOX, SoundEvents.MUSIC_DISC_CREATOR_MUSIC_BOX, 73, 11);
	}
}
