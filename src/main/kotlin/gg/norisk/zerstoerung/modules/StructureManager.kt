package gg.norisk.zerstoerung.modules

import com.mojang.brigadier.arguments.StringArgumentType
import gg.norisk.zerstoerung.Destruction
import gg.norisk.zerstoerung.Zerstoerung.logger
import gg.norisk.zerstoerung.config.ConfigManager
import gg.norisk.zerstoerung.serialization.BlockPosSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.particle.BlockStateParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.structure.StructureStart
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i
import net.minecraft.world.StructureWorldAccess
import net.silkmc.silk.commands.LiteralCommandBuilder
import net.silkmc.silk.core.math.geometry.produceFilledSpherePositions
import net.silkmc.silk.core.text.literal
import net.silkmc.silk.core.text.literalText
import java.util.function.Consumer

object StructureManager : Destruction("Structure") {
    private var config = Config()
    var currentStructure: Identifier? = null

    @Serializable
    private data class Config(
        val possibleStructures: MutableSet<String> = mutableSetOf(),
        val structureBlocks: MutableMap<String, MutableMap<String, MutableSet<@Serializable(with = BlockPosSerializer::class) BlockPos>>> = mutableMapOf(),
        val disabledStructures: MutableSet<String> = mutableSetOf()
    )

    override fun onEnable(server: MinecraftServer) {
        super.onEnable(server)
        config.possibleStructures.addAll(server.registryManager.get(RegistryKeys.STRUCTURE).ids.map { it.toString() })
    }

    override fun tickServerWorld(world: ServerWorld) {
        val structures = config.disabledStructures.toList()
        world.players.forEach { player ->
            Vec3i(
                player.blockX,
                player.blockY,
                player.blockZ
            ).produceFilledSpherePositions(ConfigManager.config.radius) { pos ->
                for (disabledStructure in structures) {
                    val flag = config.structureBlocks[world.registryKey.value.toString()]
                        ?.get(disabledStructure)
                        ?.contains(pos) ?: false
                    if (flag) {
                        destroyBlock(world, pos, disabledStructure)
                    }
                }
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
            currentStructure = id
            consumer.accept(structureStart)
            currentStructure = null
        }
    }

    private fun destroyBlock(world: ServerWorld, pos: BlockPos, structure: String) {
        val blockState = world.getBlockState(pos)
        world.breakBlock(pos, false)
        if (blockState.isLiquid) {
            world.setBlockState(pos, Blocks.AIR.defaultState)
        }
        config.structureBlocks[world.registryKey.value.toString()]?.get(structure)?.remove(pos)
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
        if (currentStructure != null) {
            logger.bug("Adding $pos in ${world.registryKey.value} of ${currentStructure.toString()}")
            val worldStructures =
                config.structureBlocks.computeIfAbsent(world.registryKey.value.toString()) { mutableMapOf() }
            val structureTypeBlocks = worldStructures.computeIfAbsent(currentStructure.toString()) { mutableSetOf() }
            structureTypeBlocks.add(pos.toImmutable())
        }
    }

    override fun destroy() {
        val remainingStructures = config.possibleStructures.filter { !config.disabledStructures.contains(it) }.toList()
        if (remainingStructures.isNotEmpty()) {
            val randomStructure = remainingStructures.random()
            config.disabledStructures.add(randomStructure)
            broadcastDestruction(literalText {
                text(
                    randomStructure.split(":")[1].replace("_", " ")
                ) {
                    color = 0xff5733
                }
            })
        }
    }

    override fun loadConfig() {
        if (configFile.exists()) {
            runCatching {
                config = JSON.decodeFromString<Config>(configFile.readText())
                logger.info("Successfully loaded $name to config file")
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
            logger.info("Successfully saved $name to config file")
        }.onFailure {
            it.printStackTrace()
        }
    }

    private fun toggleStructure(structure: String, player: PlayerEntity?) {
        if (config.disabledStructures.contains(structure)) {
            config.disabledStructures.remove(structure)
            player?.sendMessage("$structure has been removed".literal)
        } else {
            config.disabledStructures.add(structure)
            player?.sendMessage("$structure has been added".literal)
        }
    }

    override fun commandCallback(literalCommandBuilder: LiteralCommandBuilder<ServerCommandSource>) {
        literalCommandBuilder.apply {
            literal("toggle") {
                argument<String>("structure", StringArgumentType.greedyString()) { structure ->
                    suggestList { context ->
                        context.source.world.registryManager.get(RegistryKeys.STRUCTURE).ids.map { it.toString() }
                    }
                    runs {
                        toggleStructure(structure(), this.source.player)
                    }
                }
            }
            literal("test") {
                runs {
                    val playerOrThrow = this.source.playerOrThrow
                    loadConfig()
                    val world = config.structureBlocks[playerOrThrow.world.registryKey.value.toString()]
                    val blocks = mutableSetOf<BlockPos>()
                    for (blockPosList in world?.values ?: emptyList()) {
                        for (blockPos in blockPosList) {
                            if (blockPos.isWithinDistance(playerOrThrow.pos, 100.0)) {
                                blocks += blockPos
                            }
                        }
                    }
                    for (block in blocks) {
                        playerOrThrow.world.setBlockState(block, Blocks.DIAMOND_BLOCK.defaultState)
                    }
                }
            }
        }
    }
}
