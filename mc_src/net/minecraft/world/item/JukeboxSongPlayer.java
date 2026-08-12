package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class JukeboxSongPlayer {
	public static final int PLAY_EVENT_INTERVAL_TICKS = 20;
	private long ticksSinceSongStarted;
	@Nullable
	private Holder<JukeboxSong> song;
	private final BlockPos blockPos;
	private final JukeboxSongPlayer.OnSongChanged onSongChanged;

	public JukeboxSongPlayer(JukeboxSongPlayer.OnSongChanged arg, BlockPos arg2) {
		this.onSongChanged = arg;
		this.blockPos = arg2;
	}

	public boolean isPlaying() {
		return this.song != null;
	}

	@Nullable
	public JukeboxSong getSong() {
		return this.song == null ? null : this.song.value();
	}

	public long getTicksSinceSongStarted() {
		return this.ticksSinceSongStarted;
	}

	public void setSongWithoutPlaying(Holder<JukeboxSong> arg, long l) {
		if (!((JukeboxSong)arg.value()).hasFinished(l)) {
			this.song = arg;
			this.ticksSinceSongStarted = l;
		}
	}

	public void play(LevelAccessor arg, Holder<JukeboxSong> arg2) {
		this.song = arg2;
		this.ticksSinceSongStarted = 0L;
		int i = arg.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).getId(this.song.value());
		arg.levelEvent(null, 1010, this.blockPos, i);
		this.onSongChanged.notifyChange();
	}

	public void stop(LevelAccessor arg, @Nullable BlockState arg2) {
		if (this.song != null) {
			this.song = null;
			this.ticksSinceSongStarted = 0L;
			arg.gameEvent(GameEvent.JUKEBOX_STOP_PLAY, this.blockPos, GameEvent.Context.of(arg2));
			arg.levelEvent(1011, this.blockPos, 0);
			this.onSongChanged.notifyChange();
		}
	}

	public void tick(LevelAccessor arg, @Nullable BlockState arg2) {
		if (this.song != null) {
			if (this.song.value().hasFinished(this.ticksSinceSongStarted)) {
				this.stop(arg, arg2);
			} else {
				if (this.shouldEmitJukeboxPlayingEvent()) {
					arg.gameEvent(GameEvent.JUKEBOX_PLAY, this.blockPos, GameEvent.Context.of(arg2));
					spawnMusicParticles(arg, this.blockPos);
				}

				this.ticksSinceSongStarted++;
			}
		}
	}

	private boolean shouldEmitJukeboxPlayingEvent() {
		return this.ticksSinceSongStarted % 20L == 0L;
	}

	private static void spawnMusicParticles(LevelAccessor arg, BlockPos arg2) {
		if (arg instanceof ServerLevel serverLevel) {
			Vec3 vec3 = Vec3.atBottomCenterOf(arg2).add(0.0, 1.2F, 0.0);
			float f = arg.getRandom().nextInt(4) / 24.0F;
			serverLevel.sendParticles(ParticleTypes.NOTE, vec3.x(), vec3.y(), vec3.z(), 0, f, 0.0, 0.0, 1.0);
		}
	}

	@FunctionalInterface
	public interface OnSongChanged {
		void notifyChange();
	}
}
