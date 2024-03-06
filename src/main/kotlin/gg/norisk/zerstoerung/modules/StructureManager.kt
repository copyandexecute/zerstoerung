package gg.norisk.zerstoerung.modules

import gg.norisk.zerstoerung.Destruction
import gg.norisk.zerstoerung.Zerstoerung.logger
import kotlinx.serialization.encodeToString
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.particle.BlockStateParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i
import net.silkmc.silk.commands.LiteralCommandBuilder
import net.silkmc.silk.core.kotlin.ticks
import net.silkmc.silk.core.math.geometry.produceFilledSpherePositions
import net.silkmc.silk.core.task.mcCoroutineTask

object StructureManager : Destruction("Structure") {
    var structureBlocks = mutableSetOf<BlockPos>()

    override fun init() {
        super.init()
    }

    override fun tickServerWorld(world: ServerWorld) {
        world.players.forEach { player ->
            Vec3i(player.blockX, player.blockY, player.blockZ).produceFilledSpherePositions(15) { pos ->
                if (structureBlocks.contains(pos)) {
                    destroyBlock(world, pos)
                }
            }
        }
    }

    private fun destroyBlock(world: ServerWorld, pos: BlockPos) {
        val blockState = world.getBlockState(pos)
        world.breakBlock(pos,false)
        structureBlocks.remove(pos)
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

    override fun onEnable() {
        super.onEnable()
        if (configFile.exists()) {
            runCatching {
                val currentSize = structureBlocks.size
                val blocks = JSON.decodeFromString<MutableSet<BlockPos>>(configFile.readText())
                structureBlocks.addAll(blocks)
                logger.info("Successfully loaded blocks ${blocks.size} from config file and had current size of $currentSize")
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    override fun onDisable() {
        super.onDisable()
        runCatching {
            configFile.writeText(JSON.encodeToString<MutableSet<BlockPos>>(structureBlocks))
            logger.info("Successfully saved blocks ${structureBlocks.size} to config file")
        }.onFailure {
            it.printStackTrace()
        }
    }

    override fun commandCallback(literalCommandBuilder: LiteralCommandBuilder<ServerCommandSource>) {
        literalCommandBuilder.apply {
            literal("test") {
                runs {
                    val player = this.source.playerOrThrow
                    for (structureBlock in structureBlocks.filter { it.isWithinDistance(player.pos, 100.0) }) {
                        mcCoroutineTask(delay = 1.ticks) {
                            destroyBlock(player.serverWorld, structureBlock)
                        }
                    }
                }
            }
        }
    }
}
