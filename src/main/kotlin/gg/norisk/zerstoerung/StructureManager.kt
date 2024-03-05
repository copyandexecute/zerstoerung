package gg.norisk.zerstoerung

import gg.norisk.zerstoerung.Zerstoerung.logger
import kotlinx.serialization.encodeToString
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i
import net.silkmc.silk.commands.LiteralCommandBuilder
import net.silkmc.silk.core.math.geometry.produceFilledSpherePositions

object StructureManager : Destruction("Structure") {
    var structureBlocks = mutableSetOf<BlockPos>()

    override fun init() {
        super.init()
    }

    override fun tickServerWorld(world: ServerWorld) {
        world.players.forEach { player ->
            Vec3i(player.blockX, player.blockY, player.blockZ).produceFilledSpherePositions(25) { pos ->
                if (structureBlocks.contains(pos)) {
                    world.setBlockState(pos, Blocks.AIR.defaultState, Block.NOTIFY_LISTENERS)
                    structureBlocks.remove(pos)
                }
            }
        }
    }

    override fun onEnable() {
        super.onEnable()
        if (configFile.exists()) {
            runCatching {
                structureBlocks = JSON.decodeFromString<MutableSet<BlockPos>>(configFile.readText())
                logger.info("Successfully loaded blocks ${structureBlocks.size} from config file")
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
                    for (structureBlock in structureBlocks) {
                        player.world.setBlockState(structureBlock, Blocks.DIAMOND_BLOCK.defaultState)
                    }
                }
            }
        }
    }
}
