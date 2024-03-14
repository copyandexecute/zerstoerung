package gg.norisk.zerstoerung.modules

import com.mojang.brigadier.arguments.StringArgumentType
import gg.norisk.zerstoerung.Destruction
import gg.norisk.zerstoerung.Zerstoerung
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.world.WorldAccess
import net.minecraft.world.biome.BiomeKeys
import net.minecraft.world.chunk.Chunk
import net.silkmc.silk.commands.LiteralCommandBuilder
import net.silkmc.silk.core.text.literal
import net.silkmc.silk.core.text.literalText

object BiomeManager : Destruction("Biome") {
    private var config = Config()

    @Serializable
    private data class Config(
        val possibleBiomes: MutableSet<String> = mutableSetOf(),
        val disabledBiomes: MutableSet<String> = mutableSetOf()
    )

    override fun tickServerWorld(world: ServerWorld) {
        //TODO hab das nicht hinbekommen
        /*val disabledBiomes = config.disabledBiomes.toList()
        world.players.forEach { player ->
            val mutateChunks = mutableListOf<BlockPos>()
            Vec3i(
                player.blockX,
                player.blockY,
                player.blockZ
            ).produceFilledSpherePositions(ConfigManager.config.radius) { pos ->
                val biome = world.getBiome(pos)
                if (disabledBiomes.contains(biome.key.get().value.toString())) {
                    world.setBlockState(pos, Blocks.AIR.defaultState)
                    mutateChunks.add(pos)
                }
            }
            for (pos in mutateChunks) {
                val chunk = world.getChunk(pos)
                chunk.populateBiomes(
                    createBiomeSupplier(
                        chunk,
                        BlockBox(pos),
                        world.registryManager.get(RegistryKeys.BIOME).entryOf(BiomeKeys.THE_VOID),
                    ),
                    world.chunkManager.noiseConfig.multiNoiseSampler
                )
                chunk.setNeedsSaving(true)
                world.chunkManager.threadedAnvilChunkStorage.sendChunkBiomePackets(listOf(chunk))
            }
        }*/
    }

    override fun destroy() {
        val remainingBiomes = config.possibleBiomes.filter { !config.disabledBiomes.contains(it) }.toList()
        if (remainingBiomes.isNotEmpty()) {
            val randomBiome = remainingBiomes.random()
            config.disabledBiomes.add(randomBiome)
            broadcastDestruction(literalText {
                text(
                    randomBiome.split(":")[1].replace("_", " ")
                ) {
                    color = 0xff5733
                }
            })
        }
    }

    override fun onEnable(server: MinecraftServer) {
        super.onEnable(server)
        //Removing void as we abuse it for the method above
        config.possibleBiomes.addAll(server.registryManager.get(RegistryKeys.BIOME).ids
            .filter { it != BiomeKeys.THE_VOID.value }
            .map { it.toString() })
    }

    override fun loadConfig() {
        if (configFile.exists()) {
            runCatching {
                config = JSON.decodeFromString<Config>(configFile.readText())
                Zerstoerung.logger.info("Successfully loaded $name to config file")
            }.onFailure {
                it.printStackTrace()
            }
        } else {
            config = Config()
        }
    }

    override fun saveConfig() {
        runCatching {
            configFile.writeText(JSON.encodeToString<Config>(config))
            Zerstoerung.logger.info("Successfully saved $name to config file")
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun mutateChunk(chunk: Chunk, world: WorldAccess) {
        if (isEnabled) {
            chunk.forEachBlockMatchingPredicate({
                !it.isAir
            }) { blockPos, blockState ->
                val biome = world.getBiome(blockPos)
                if (config.disabledBiomes.contains(biome.key.get().value.toString())) {
                    world.setBlockState(blockPos, Blocks.AIR.defaultState, Block.FORCE_STATE)
                }
            }
        }
    }

    override fun commandCallback(literalCommandBuilder: LiteralCommandBuilder<ServerCommandSource>) {
        literalCommandBuilder.apply {
            literal("toggle") {
                argument<String>("biome", StringArgumentType.greedyString()) { biome ->
                    suggestList { context ->
                        context.source.world.registryManager.get(RegistryKeys.BIOME).ids.map { it.toString() }
                    }
                    runs {
                        config.disabledBiomes.add(biome())
                        this.source.sendMessage("Disabled ${biome()}".literal)
                    }
                }
            }
        }
    }


    //TODO das fuckt sachen ab
    fun mutateChunk(world: ServerWorld, state: BlockState, pos: BlockPos): BlockState {
        if (state.isAir) return state
        return if (isEnabled) {
            val biome = world.getBiome(pos)
            if (config.disabledBiomes.contains(biome.key.get().value.toString())) {
                Blocks.AIR.defaultState
            } else {
                state
            }
        } else {
            state
        }
    }
}
