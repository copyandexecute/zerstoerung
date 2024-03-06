package gg.norisk.zerstoerung.modules

import gg.norisk.zerstoerung.Destruction
import gg.norisk.zerstoerung.Zerstoerung.logger
import gg.norisk.zerstoerung.serialization.BlockPosSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import net.minecraft.particle.BlockStateParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.structure.StructureStart
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i
import net.minecraft.world.StructureWorldAccess
import net.silkmc.silk.commands.LiteralCommandBuilder
import net.silkmc.silk.core.math.geometry.produceFilledSpherePositions
import java.util.function.Consumer

object StructureManager : Destruction("Structure") {
    private val config = Config()
    var currentStructure: Identifier? = null

    @Serializable
    private data class Config(
        val structureBlocks: MutableMap<String, MutableMap<String, MutableSet<@Serializable(with = BlockPosSerializer::class) BlockPos>>> = mutableMapOf(),
        val disabledStructures: MutableSet<String> = mutableSetOf()
    )

    override fun init() {
        super.init()
    }

    override fun tickServerWorld(world: ServerWorld) {
        world.players.forEach { player ->
            Vec3i(player.blockX, player.blockY, player.blockZ).produceFilledSpherePositions(15) { pos ->
                val blocks = config.structureBlocks[world.registryKey.value.toString()]
                /*if (blocks?.contains(pos) == true) {
                    destroyBlock(world, pos)
                }*/
            }
        }
    }

    fun handleStructureGeneration(
        instance: MutableList<StructureStart>,
        consumer: Consumer<StructureStart>,
        world: StructureWorldAccess
    ) {
        for (structureStart in instance) {
            val id = world.registryManager.get(RegistryKeys.STRUCTURE).getId(structureStart.structure) ?: continue

            val bypass = listOf("mineshaft")

            if (bypass.none { it == id.path }) {
                logger.info("current structure generation: $id")
                currentStructure = id
            }

            consumer.accept(structureStart)
        }
    }

    private fun destroyBlock(world: ServerWorld, pos: BlockPos) {
        val blockState = world.getBlockState(pos)
        world.breakBlock(pos, false)
        //structureBlocks[world.registryKey.value.toString()]?.remove(pos)
        world.spawnParticles(
            BlockStateParticleEffect(ParticleTypes.BLOCK, blockState),
            pos.toCenterPos().x,
            pos.toCenterPos().y,
            pos.toCenterPos().z,
            2,
            (1 / 4.0f).toDouble(),
            (1 / 4.0f).toDouble(),
            (1 / 4.0f).toDouble(),
            0.05
        )
    }

    fun addBlockPos(world: ServerWorld, pos: BlockPos) {
        logger.info("Adding $pos in ${world.registryKey.value} of ${currentStructure.toString()}")
        val worldStructures =
            config.structureBlocks.computeIfAbsent(world.registryKey.value.toString()) { mutableMapOf() }
        val structureTypeBlocks = worldStructures.computeIfAbsent(currentStructure.toString()) { mutableSetOf() }
        structureTypeBlocks.add(pos)
    }

    override fun onEnable() {
        super.onEnable()
        loadConfig()
    }

    override fun loadConfig() {
        if (configFile.exists()) {
            runCatching {
                val loadedConfig = JSON.decodeFromString<Config>(configFile.readText())

                config.disabledStructures.addAll(loadedConfig.disabledStructures)

                for (worldStructures in loadedConfig.structureBlocks) {
                    worldStructures.value.forEach { (structure, blocks) ->
                        config.structureBlocks
                            .computeIfAbsent(worldStructures.key) { mutableMapOf() }
                            .computeIfAbsent(structure) { mutableSetOf() }
                            .addAll(blocks)
                    }
                }

                logger.info("Successfully loaded $name to config file")
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    override fun saveConfig() {
        runCatching {
            loadConfig()
            configFile.writeText(JSON.encodeToString<Config>(config))
            logger.info("Successfully saved $name to config file")
        }.onFailure {
            it.printStackTrace()
        }
    }

    override fun onDisable() {
        super.onDisable()
        saveConfig()
    }

    override fun commandCallback(literalCommandBuilder: LiteralCommandBuilder<ServerCommandSource>) {
        literalCommandBuilder.apply {
            literal("test") {
                runs {
                }
            }
        }
    }
}
