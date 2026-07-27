$source = "C:\Users\User\Documents\sawitmod-1.20.1-fabric-like-forge-template"
$targets = @(
    @{ Dir="C:\Users\User\Documents\sawitmod-1.20.2-fabric-like-forge-template"; Modules=@("common", "fabric", "fabric-like", "forge", "quilt"); Name="1.20.2" },
    @{ Dir="C:\Users\User\Documents\sawitmod-1.20.4-fabric-like-neoforge-forge-template"; Modules=@("common", "fabric", "fabric-like", "forge", "neoforge"); Name="1.20.4" },
    @{ Dir="C:\Users\User\Documents\sawitmod-1.20.5-fabric-neoforge-template"; Modules=@("common", "fabric", "neoforge"); Name="1.20.5" },
    @{ Dir="C:\Users\User\Documents\sawitmod-1.20.6-fabric-like-neoforge-template"; Modules=@("common", "fabric", "fabric-like", "neoforge", "quilt"); Name="1.20.6" }
)

Write-Host "Syncing code from 1.20.1 to all versions..." -ForegroundColor Cyan

foreach ($targetInfo in $targets) {
    $target = $targetInfo.Dir
    $modules = $targetInfo.Modules
    $name = $targetInfo.Name
    
    Write-Host "Syncing to $name..." -ForegroundColor Yellow
    foreach ($module in $modules) {
        $srcMod = if ($module -eq "neoforge") { "forge" } else { $module }
        
        $sourceJava = Join-Path $source "$srcMod\src\main\java"
        $targetJava = Join-Path $target "$module\src\main\java"
        
        if (Test-Path $targetJava) {
            Remove-Item -Recurse -Force $targetJava
        }
        
        if (Test-Path $sourceJava) {
            # Make sure parent dir exists
            if (!(Test-Path (Join-Path $target "$module\src\main"))) {
                New-Item -ItemType Directory -Force -Path (Join-Path $target "$module\src\main") | Out-Null
            }
            Copy-Item -Path $sourceJava -Destination (Join-Path $target "$module\src\main") -Recurse -Force
            
            # NeoForge package renaming
            if ($module -eq "neoforge") {
                $forgeDir = Join-Path $targetJava "com\sawit\kotaklegend\forge"
                if (Test-Path $forgeDir) {
                    Rename-Item $forgeDir "neoforge"
                }
                
                $neoDir = Join-Path $targetJava "com\sawit\kotaklegend\neoforge"
                $forgeFile = Join-Path $neoDir "ExampleModForge.java"
                if (Test-Path $forgeFile) {
                    Rename-Item $forgeFile "ExampleModNeoForge.java"
                }
            }
            
            # Patch for API differences
            $javaFiles = Get-ChildItem -Path $targetJava -Recurse -Filter *.java
            foreach ($f in $javaFiles) {
                $content = Get-Content $f.FullName -Raw
                $modified = $false
                
                # 1.20.2+ Bonemeal
                if ($content -match "isValidBonemealTarget\(LevelReader\s+(.*?), BlockPos\s+(.*?), BlockState\s+(.*?), boolean\s+(.*?)\)") {
                    $content = $content -replace "isValidBonemealTarget\(LevelReader\s+([a-zA-Z0-9_]+),\s*BlockPos\s+([a-zA-Z0-9_]+),\s*BlockState\s+([a-zA-Z0-9_]+),\s*boolean\s+([a-zA-Z0-9_]+)\)", "isValidBonemealTarget(LevelReader `$1, BlockPos `$2, BlockState `$3)"
                    $modified = $true
                }
                if ($content -match "isValidBonemealTarget\((.*?),\s*(.*?),\s*(.*?),\s*(.*?)\)") {
                    $content = $content -replace "isValidBonemealTarget\((.*?),\s*(.*?),\s*(.*?),\s*(.*?)\)", "isValidBonemealTarget(`$1, `$2, `$3)"
                    $modified = $true
                }
                # 1.20.2+ Effect Tick
                if ($content -match "isDurationEffectTick") {
                    $content = $content -replace "isDurationEffectTick", "shouldApplyEffectTickThisTick"
                    $modified = $true
                }
                # 1.20.4+ Block Properties and methods
                if ($name -ge "1.20.4") {
                    if ($content -match "BlockBehaviour\.Properties\.copy\(") {
                        $content = $content -replace "BlockBehaviour\.Properties\.copy\(", "BlockBehaviour.Properties.ofFullCopy("
                        $modified = $true
                    }
                    if ($content -match "void\s+playerWillDestroy\(") {
                        $content = $content -replace "void\s+playerWillDestroy\(", "net.minecraft.world.level.block.state.BlockState playerWillDestroy("
                        $content = $content -replace "super\.playerWillDestroy\(", "return super.playerWillDestroy("
                        $modified = $true
                    }
                    if ($content -match "new net\.minecraft\.world\.level\.block\.PressurePlateBlock\(net\.minecraft\.world\.level\.block\.PressurePlateBlock\.Sensitivity\.EVERYTHING, (.*?), (.*?)\)") {
                        $content = $content -replace "new net\.minecraft\.world\.level\.block\.PressurePlateBlock\(net\.minecraft\.world\.level\.block\.PressurePlateBlock\.Sensitivity\.EVERYTHING, (.*?), (.*?)\)", "new net.minecraft.world.level.block.PressurePlateBlock(`$2, `$1)"
                        $modified = $true
                    }
                    if ($content -match "new net\.minecraft\.world\.level\.block\.DoorBlock\(BlockBehaviour\.Properties\.ofFullCopy\(Blocks\.OAK_DOOR\), (.*?)\)") {
                        $content = $content -replace "new net\.minecraft\.world\.level\.block\.DoorBlock\(BlockBehaviour\.Properties\.ofFullCopy\(Blocks\.OAK_DOOR\), (.*?)\)", "new net.minecraft.world.level.block.DoorBlock(`$1, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR))"
                        $modified = $true
                    }
                    if ($content -match "new net\.minecraft\.world\.level\.block\.TrapDoorBlock\(BlockBehaviour\.Properties\.ofFullCopy\(Blocks\.OAK_TRAPDOOR\), (.*?)\)") {
                        $content = $content -replace "new net\.minecraft\.world\.level\.block\.TrapDoorBlock\(BlockBehaviour\.Properties\.ofFullCopy\(Blocks\.OAK_TRAPDOOR\), (.*?)\)", "new net.minecraft.world.level.block.TrapDoorBlock(`$1, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR))"
                        $modified = $true
                    }
                    if ($content -match "new net\.minecraft\.world\.level\.block\.FenceGateBlock\(BlockBehaviour\.Properties\.ofFullCopy\(Blocks\.OAK_PLANKS\), (.*?)\)") {
                        $content = $content -replace "new net\.minecraft\.world\.level\.block\.FenceGateBlock\(BlockBehaviour\.Properties\.ofFullCopy\(Blocks\.OAK_PLANKS\), (.*?)\)", "new net.minecraft.world.level.block.FenceGateBlock(`$1, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS))"
                        $modified = $true
                    }
                    if ($content -match "new net\.minecraft\.world\.level\.block\.ButtonBlock\(BlockBehaviour\.Properties\.ofFullCopy\(Blocks\.OAK_BUTTON\), (.*?), (\d+), (.*?)\)") {
                        $content = $content -replace "new net\.minecraft\.world\.level\.block\.ButtonBlock\(BlockBehaviour\.Properties\.ofFullCopy\(Blocks\.OAK_BUTTON\), (.*?), (\d+), (.*?)\)", "new net.minecraft.world.level.block.ButtonBlock(`$1, `$2, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON))"
                        $modified = $true
                    }
                    if ($content -match "super\(properties, woodType\)") {
                        $content = $content -replace "super\(properties, woodType\)", "super(woodType, properties)"
                        $modified = $true
                    }
                    if ($content -match "public class (SawitBlock|SawitTrunkBlock|SawitTrunkDummyBlock) extends") {
                        if ($content -notmatch "MapCodec") {
                            $className = $matches[1]
                            $codecStr = "`n    @Override`n    protected com.mojang.serialization.MapCodec<`$2> codec() {`n        return simpleCodec(`$2::new);`n    }`n"
                            $content = $content -replace "(public class (SawitBlock|SawitTrunkBlock|SawitTrunkDummyBlock).*?\{)", "`$1$codecStr"
                            $modified = $true
                        }
                    }
                }
                
                # 1.20.5+ API changes
                if ($name -ge "1.20.5") {
                    # InteractionResult use -> useWithoutItem
                    if ($content -match "public InteractionResult use\(BlockState state, Level level, BlockPos pos, (.*?)Player player, InteractionHand hand, BlockHitResult hit\)") {
                        $content = $content -replace "public InteractionResult use\(BlockState state, Level level, BlockPos pos, (.*?)Player player, InteractionHand hand, BlockHitResult hit\)", "protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, `$1Player player, BlockHitResult hit)"
                        $modified = $true
                    }
                    if ($content -match "super\.use\(state, level, pos, player, hand, hit\)") {
                        $content = $content -replace "super\.use\(state, level, pos, player, hand, hit\)", "super.useWithoutItem(state, level, pos, player, hit)"
                        $modified = $true
                    }
                    if ($content -match "baseState\.getBlock\(\)\.use\(baseState, level, basePos, player, hand, hit\)") {
                        $content = $content -replace "baseState\.getBlock\(\)\.use\(baseState, level, basePos, player, hand, hit\)", "baseState.useWithoutItem(level, player, hit)"
                        $modified = $true
                    }
                    if ($content -match "player\.getItemInHand\(hand\)") {
                        $content = $content -replace "player\.getItemInHand\(hand\)", "player.getMainHandItem()"
                        $modified = $true
                    }
                    if ($content -match "this\.saveWithoutMetadata\(\)") {
                        # We will replace it inside getUpdateTag instead to pass provider
                    }
                    
                    # MobEffect applyEffectTick return boolean
                    if ($content -match "public void applyEffectTick\(LivingEntity (.*?), int (.*?)\)") {
                        $content = $content -replace "public void applyEffectTick\(LivingEntity (.*?), int (.*?)\)", "public boolean applyEffectTick(LivingEntity `$1, int `$2)"
                        $content = $content -replace "MobEffects\.CONFUSION, 100, amplifier, false, false, false\)\);`r?`n        }", "MobEffects.CONFUSION, 100, amplifier, false, false, false));`n        }`n        return true;"
                        $modified = $true
                    }
                    
                    # hurtAndBreak
                    if ($content -match "itemStack\.hurtAndBreak\((.*?), (.*?), \(p\) -> p\.broadcastBreakEvent\((.*?)\)\)") {
                        $content = $content -replace "itemStack\.hurtAndBreak\((.*?), (.*?), \(p\) -> p\.broadcastBreakEvent\((.*?)\)\)", "itemStack.hurtAndBreak(`$1, `$2, net.minecraft.world.entity.EquipmentSlot.MAINHAND)"
                        $modified = $true
                    }
                    
                    # TooltipContext
                    if ($content -match "public void appendHoverText\(ItemStack (.*?), @Nullable Level (.*?), List<Component> (.*?), TooltipFlag (.*?)\)") {
                        $content = $content -replace "public void appendHoverText\(ItemStack (.*?), @Nullable Level (.*?), List<Component> (.*?), TooltipFlag (.*?)\)", "public void appendHoverText(ItemStack `$1, net.minecraft.world.item.Item.TooltipContext `$2, List<Component> `$3, TooltipFlag `$4)"
                        $modified = $true
                    }
                    
                    # MobEffect Holder
                    if ($content -match "ModEffects\.KOLESTROL\.get\(\)") {
                        $content = $content -replace "ModEffects\.KOLESTROL\.get\(\)", "ModEffects.KOLESTROL"
                        $modified = $true
                    }
                    
                    # MerchantOffer ItemCost
                    if ($content -match "new net\.minecraft\.world\.item\.trading\.MerchantOffer\((price|priceA|priceB), (forSale), (maxTrades), (xp), (priceMult)\)") {
                        $content = $content -replace "new net\.minecraft\.world\.item\.trading\.MerchantOffer\((.*?),\s*(.*?),\s*(.*?),\s*(.*?),\s*(.*?)\)", "new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(`$1.getItem(), `$1.getCount()), `$2, `$3, `$4, `$5)"
                        $modified = $true
                    }
                    
                    # saturationMod -> saturationModifier
                    if ($content -match "\.saturationMod\(") {
                        $content = $content -replace "\.saturationMod\(", ".saturationModifier("
                        $modified = $true
                    }
                    
                    # BlockEntity load -> loadAdditional
                    if ($content -match "public void load\(net\.minecraft\.nbt\.CompoundTag (.*?)\)") {
                        $content = $content -replace "public void load\(net\.minecraft\.nbt\.CompoundTag (.*?)\)", "protected void loadAdditional(net.minecraft.nbt.CompoundTag `$1, net.minecraft.core.HolderLookup.Provider provider)"
                        $content = $content -replace "super\.load\((.*?)\)", "super.loadAdditional(`$1, provider)"
                        $modified = $true
                    }
                    
                    # BlockEntity saveAdditional
                    if ($content -match "protected void saveAdditional\(net\.minecraft\.nbt\.CompoundTag (.*?)\)") {
                        $content = $content -replace "protected void saveAdditional\(net\.minecraft\.nbt\.CompoundTag (.*?)\)", "protected void saveAdditional(net.minecraft.nbt.CompoundTag `$1, net.minecraft.core.HolderLookup.Provider provider)"
                        $content = $content -replace "super\.saveAdditional\((.*?)\)", "super.saveAdditional(`$1, provider)"
                        $modified = $true
                    }
                    
                    # getUpdateTag
                    if ($content -match "public net\.minecraft\.nbt\.CompoundTag getUpdateTag\(\)") {
                        $content = $content -replace "public net\.minecraft\.nbt\.CompoundTag getUpdateTag\(\)", "public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider provider)"
                        $content = $content -replace "this\.saveWithoutMetadata\(\)", "this.saveWithoutMetadata(provider)"
                        $modified = $true
                    }
                    
                    # ModVillagers: DeferredRegister and RegistrySupplier missing architectury imports
                    if ($f.FullName -match "ModVillagers\.java") {
                        if ($content -notmatch "import dev\.architectury\.registry\.registries") {
                            $content = $content -replace "import com\.sawit\.kotaklegend\.ExampleMod;", "import com.sawit.kotaklegend.ExampleMod;`nimport dev.architectury.registry.registries.DeferredRegister;`nimport dev.architectury.registry.registries.RegistrySupplier;`nimport dev.architectury.registry.level.entity.trade.TradeRegistry;"
                            $modified = $true
                        }
                    }
                    
                    # ModBlocks, ModItems, ModEffects, ModBlockEntities missing imports
                    if ($content -notmatch "import dev\.architectury\.registry\.registries") {
                        if ($content -match "DeferredRegister") {
                            $content = $content -replace "package (.*?);", "package `$1;`nimport dev.architectury.registry.registries.DeferredRegister;`nimport dev.architectury.registry.registries.RegistrySupplier;"
                            $modified = $true
                        }
                    }
                    # ExampleModFabric in 1.20.5 has no fabriclike
                    if ($module -eq "fabric" -and $f.FullName -match "ExampleModFabric\.java") {
                        $content = $content -replace "import com\.sawit\.kotaklegend\.fabriclike\.ExampleModFabricLike;", "import com.sawit.kotaklegend.ExampleMod;"
                        $content = $content -replace "ExampleModFabricLike\.init\(\);", "ExampleMod.init();"
                        $modified = $true
                    }
                    
                    # SawitOilItem fixes
                    if ($f.FullName -match "SawitOilItem\.java") {
                        $content = $content -replace "private final boolean isJelantah;", "public final boolean isJelantah;"
                        $modified = $true
                    }
                    
                    # AbstractFurnaceBlockEntityMixin fixes
                    if ($f.FullName -match "AbstractFurnaceBlockEntityMixin\.java") {
                        $content = $content -replace "\!fuelStack\.getItem\(\)\.hasCraftingRemainingItem\(\)", "((com.sawit.kotaklegend.item.SawitOilItem)fuelStack.getItem()).isJelantah"
                        $content = $content -replace "\!fuelStack\.hasCraftingRemainingItem\(\)", "((com.sawit.kotaklegend.item.SawitOilItem)fuelStack.getItem()).isJelantah"
                        $content = $content -replace "fuelStack\.getItem\(\) == com\.sawit\.kotaklegend\.registry\.ModItems\.JELANTAH_OIL\.get\(\)", "((com.sawit.kotaklegend.item.SawitOilItem)fuelStack.getItem()).isJelantah"
                        $content = $content -replace "\!result\.getItem\(\)\.isEdible\(\)", "!result.has(net.minecraft.core.component.DataComponents.FOOD)"
                        $modified = $true
                    }
                }
                
                # NeoForge patches
                if ($module -eq "neoforge") {
                    if ($f.FullName -match "ExampleModNeoForge\.java") {
                        $rlStr = if ($name -ge "1.21") { "net.minecraft.resources.ResourceLocation.parse(`"sawitmod:overworld`")" } else { "new net.minecraft.resources.ResourceLocation(`"sawitmod`", `"overworld`")" }
                        $content = @"
package com.sawit.kotaklegend.neoforge;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import com.sawit.kotaklegend.ExampleMod;
@Mod(ExampleMod.MOD_ID)
public final class ExampleModNeoForge {
    public ExampleModNeoForge(IEventBus modEventBus) {
        ExampleMod.init();
        modEventBus.addListener(this::setupTerraBlender);
    }
    private void setupTerraBlender(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            terrablender.api.Regions.register(new SawitRegion($rlStr, 2));
        });
    }
}
"@
                        $modified = $true
                    } else {
                        if ($content -match "net\.minecraftforge") {
                            $content = $content -replace "net\.minecraftforge", "net.neoforged"
                            $modified = $true
                        }
                        if ($content -match "package com\.sawit\.kotaklegend\.forge") {
                            $content = $content -replace "package com\.sawit\.kotaklegend\.forge", "package com.sawit.kotaklegend.neoforge"
                            $modified = $true
                        }
                    }
                }
                
                if ($modified) {
                    Set-Content $f.FullName $content
                }
            }
        }
        
        # For common module, we also sync resources
        if ($module -eq "common") {
            $sourceRes = Join-Path $source "common\src\main\resources"
            $targetRes = Join-Path $target "common\src\main\resources"
            
            if (Test-Path $targetRes) {
                Remove-Item -Recurse -Force $targetRes
            }
            if (Test-Path $sourceRes) {
                if (!(Test-Path (Join-Path $target "common\src\main"))) {
                    New-Item -ItemType Directory -Force -Path (Join-Path $target "common\src\main") | Out-Null
                }
                Copy-Item -Path $sourceRes -Destination (Join-Path $target "common\src\main") -Recurse -Force
            }
        }
    }
}

Write-Host "Code sync completed." -ForegroundColor Green

# Build 1.20.1
Write-Host "Building 1.20.1..." -ForegroundColor Cyan
Set-Location $source
.\gradlew.bat build
if ($LASTEXITCODE -ne 0) { Write-Error "1.20.1 build failed!"; exit 1 }

# Build all other targets
foreach ($targetInfo in $targets) {
    $target = $targetInfo.Dir
    $name = $targetInfo.Name
    
    Write-Host "Building $name..." -ForegroundColor Cyan
    Set-Location $target
    .\gradlew.bat build
    
    if ($LASTEXITCODE -ne 0) {
        Write-Error "$name build failed!"
        exit 1
    }
}

Write-Host "All versions built successfully!" -ForegroundColor Green
