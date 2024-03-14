package gg.norisk.zerstoerung.mixin.world.biome.source;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Stream;

@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin extends BiomeSource {

    /*
    //Lustiger Random Biome Code
    @Override
    public RegistryEntry<Biome> getBiome(int i, int j, int k, MultiNoiseUtil.MultiNoiseSampler multiNoiseSampler) {
        var biomes = this.biomeStream().toList();
        RegistryEntry<Biome> randomBiome = biomes.get(new Random().nextInt(biomes.size()));
        return randomBiome;
    }
     */

    @Shadow
    protected abstract Stream<RegistryEntry<Biome>> biomeStream();

    /*@Inject(method = "getBiome", at = @At("RETURN"), cancellable = true)
    private void injected(int i, int j, int k, MultiNoiseUtil.MultiNoiseSampler multiNoiseSampler, CallbackInfoReturnable<RegistryEntry<Biome>> cir) {
        RegistryEntry<Biome> biomeEntry = cir.getReturnValue();
        String biomeName = biomeEntry.getKey().get().getValue().toString();
        if (biomeName.contains("ocean")) {
            var desert = this.biomeStream().filter(biomeRegistryEntry -> {
                return biomeRegistryEntry.getKey().get().getValue().toString().contains("desert");
            }).findFirst().get();
            System.out.println("REPLACED " + biomeName + " with " + desert.getKey().get().getValue().toString());
            cir.setReturnValue(desert);
        }
    }*/
}
