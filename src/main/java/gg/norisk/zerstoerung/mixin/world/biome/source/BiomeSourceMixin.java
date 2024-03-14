package gg.norisk.zerstoerung.mixin.world.biome.source;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.BiomeSupplier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;
import java.util.function.Supplier;

@Mixin(value = BiomeSource.class, priority = 500)
public abstract class BiomeSourceMixin implements BiomeSupplier {
    @Shadow
    @Final
    private Supplier<Set<RegistryEntry<Biome>>> biomes;

    //TODO das ist glaube nicht die Lösung
    /*
     @Overwrite public Set<RegistryEntry<Biome>> getBiomes() {
     System.out.println("Called getBiomes()");
     for (RegistryEntry<Biome> biomeRegistryEntry : this.biomes.get()) {
     //System.out.println("#" + biomeRegistryEntry.getKey().get().getValue());
     }
     Stream<RegistryEntry<Biome>> filteredBiomes = this.biomes.get().stream().filter(biomeRegistryEntry -> {
     var key = biomeRegistryEntry.getKey().get().getValue();
     return key.toString().contains("ocean");
     });
     return filteredBiomes.collect(Collectors.toSet());
     } */
}
