package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class PostChain {
	public static final ResourceLocation MAIN_TARGET_ID = ResourceLocation.withDefaultNamespace("main");
	private final List<PostPass> passes;
	private final Map<ResourceLocation, PostChainConfig.InternalTarget> internalTargets;
	private final Set<ResourceLocation> externalTargets;

	private PostChain(List<PostPass> list, Map<ResourceLocation, PostChainConfig.InternalTarget> map, Set<ResourceLocation> set) {
		this.passes = list;
		this.internalTargets = map;
		this.externalTargets = set;
	}

	public static PostChain load(PostChainConfig arg, TextureManager arg2, ShaderManager arg3, Set<ResourceLocation> set) throws ShaderManager.CompilationException {
		Stream<ResourceLocation> stream = arg.passes().stream().flatMap(argx -> argx.inputs().stream()).flatMap(argx -> argx.referencedTargets().stream());
		Set<ResourceLocation> set2 = (Set<ResourceLocation>)stream.filter(arg2x -> !arg.internalTargets().containsKey(arg2x)).collect(Collectors.toSet());
		Set<ResourceLocation> set3 = Sets.<ResourceLocation>difference(set2, set);
		if (!set3.isEmpty()) {
			throw new ShaderManager.CompilationException("Referenced external targets are not available in this context: " + set3);
		} else {
			Builder<PostPass> builder = ImmutableList.builder();

			for (PostChainConfig.Pass pass : arg.passes()) {
				builder.add(createPass(arg2, arg3, pass));
			}

			return new PostChain(builder.build(), arg.internalTargets(), set2);
		}
	}

	private static PostPass createPass(TextureManager arg, ShaderManager arg2, PostChainConfig.Pass arg3) throws ShaderManager.CompilationException {
		ResourceLocation resourceLocation = arg3.program();
		CompiledShaderProgram compiledShaderProgram = arg2.getProgramForLoading(
			new ShaderProgram(resourceLocation, DefaultVertexFormat.POSITION, ShaderDefines.EMPTY)
		);

		for (PostChainConfig.Uniform uniform : arg3.uniforms()) {
			String string = uniform.name();
			if (compiledShaderProgram.getUniform(string) == null) {
				throw new ShaderManager.CompilationException("Uniform '" + string + "' does not exist for " + resourceLocation);
			}
		}

		String string2 = resourceLocation.toString();
		PostPass postPass = new PostPass(string2, compiledShaderProgram, arg3.outputTarget(), arg3.uniforms());

		for (PostChainConfig.Input input : arg3.inputs()) {
			switch (input) {
				case PostChainConfig.TextureInput(String var35, ResourceLocation var36, int var37, int var38, boolean var39):
					AbstractTexture abstractTexture = arg.getTexture(var36.withPath((UnaryOperator<String>)(stringx -> "textures/effect/" + stringx + ".png")));
					abstractTexture.setFilter(var39, false);
					postPass.addInput(new PostPass.TextureInput(var35, abstractTexture, var37, var38));
					break;
				case PostChainConfig.TargetInput(String var22, ResourceLocation var41, boolean var42, boolean var43):
					postPass.addInput(new PostPass.TargetInput(var22, var41, var42, var43));
					break;
				default:
					throw new MatchException(null, null);
			}
		}

		return postPass;
	}

	public void addToFrame(FrameGraphBuilder arg, int i, int j, PostChain.TargetBundle arg2) {
		Matrix4f matrix4f = new Matrix4f().setOrtho(0.0F, i, 0.0F, j, 0.1F, 1000.0F);
		Map<ResourceLocation, ResourceHandle<RenderTarget>> map = new HashMap(this.internalTargets.size() + this.externalTargets.size());

		for (ResourceLocation resourceLocation : this.externalTargets) {
			map.put(resourceLocation, arg2.getOrThrow(resourceLocation));
		}

		for (Entry<ResourceLocation, PostChainConfig.InternalTarget> entry : this.internalTargets.entrySet()) {
			ResourceLocation resourceLocation2 = (ResourceLocation)entry.getKey();

			RenderTargetDescriptor renderTargetDescriptor = switch ((PostChainConfig.InternalTarget)entry.getValue()) {
				case PostChainConfig.FixedSizedTarget(int var25, int var26) -> new RenderTargetDescriptor(var25, var26, true);
				case PostChainConfig.FullScreenTarget var16 -> new RenderTargetDescriptor(i, j, true);
				default -> throw new MatchException(null, null);
			};
			map.put(resourceLocation2, arg.createInternal(resourceLocation2.toString(), renderTargetDescriptor));
		}

		for (PostPass postPass : this.passes) {
			postPass.addToFrame(arg, map, matrix4f);
		}

		for (ResourceLocation resourceLocation : this.externalTargets) {
			arg2.replace(resourceLocation, (ResourceHandle<RenderTarget>)map.get(resourceLocation));
		}
	}

	@Deprecated
	public void process(RenderTarget arg, GraphicsResourceAllocator arg2) {
		FrameGraphBuilder frameGraphBuilder = new FrameGraphBuilder();
		PostChain.TargetBundle targetBundle = PostChain.TargetBundle.of(MAIN_TARGET_ID, frameGraphBuilder.importExternal("main", arg));
		this.addToFrame(frameGraphBuilder, arg.width, arg.height, targetBundle);
		frameGraphBuilder.execute(arg2);
	}

	public void setUniform(String string, float f) {
		for (PostPass postPass : this.passes) {
			postPass.getShader().safeGetUniform(string).set(f);
		}
	}

	@OnlyIn(Dist.CLIENT)
	public interface TargetBundle {
		static PostChain.TargetBundle of(ResourceLocation arg, ResourceHandle<RenderTarget> arg2) {
			return new PostChain.TargetBundle() {
				private ResourceHandle<RenderTarget> handle = arg2;

				@Override
				public void replace(ResourceLocation arg, ResourceHandle<RenderTarget> arg2) {
					if (arg.equals(arg)) {
						this.handle = arg2;
					} else {
						throw new IllegalArgumentException("No target with id " + arg);
					}
				}

				@Nullable
				@Override
				public ResourceHandle<RenderTarget> get(ResourceLocation arg) {
					return arg.equals(arg) ? this.handle : null;
				}
			};
		}

		void replace(ResourceLocation arg, ResourceHandle<RenderTarget> arg2);

		@Nullable
		ResourceHandle<RenderTarget> get(ResourceLocation arg);

		default ResourceHandle<RenderTarget> getOrThrow(ResourceLocation arg) {
			ResourceHandle<RenderTarget> resourceHandle = this.get(arg);
			if (resourceHandle == null) {
				throw new IllegalArgumentException("Missing target with id " + arg);
			} else {
				return resourceHandle;
			}
		}
	}
}
