package gg.norisk.zerstoerung.modules

import com.mojang.brigadier.arguments.StringArgumentType
import gg.norisk.zerstoerung.Destruction
import gg.norisk.zerstoerung.Zerstoerung
import gg.norisk.zerstoerung.Zerstoerung.logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.fluid.Fluids
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i
import net.silkmc.silk.commands.LiteralCommandBuilder
import net.silkmc.silk.core.math.geometry.produceFilledSpherePositions
import net.silkmc.silk.core.text.literal
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

object BlockManager : Destruction("Blocks") {
    private val config = Config()

    @Serializable
    private data class Config(
        val disabledBlocks: MutableSet<String> = mutableSetOf()
    )

    override fun tickServerWorld(world: ServerWorld) {
        val disabledBlocks = config.disabledBlocks.toList()
        world.players.forEach { player ->
            Vec3i(
                player.blockX,
                player.blockY,
                player.blockZ
            ).produceFilledSpherePositions(Zerstoerung.radius()) { pos ->
                for (blockName in disabledBlocks) {
                    val block = world.getBlockState(pos).block
                    val id = Registries.BLOCK.getId(block)
                    val flag = config.disabledBlocks.contains(id.toString())
                    if (flag) {
                        destroyBlock(world, pos)
                    }
                }
            }
        }
    }

    private fun destroyBlock(world: ServerWorld, pos: BlockPos) {
        val blockState = world.getBlockState(pos)

        if (config.disabledBlocks.contains(Registries.BLOCK.getId(Blocks.WATER).toString())) {
            if (blockState.contains(Properties.WATERLOGGED)) {
                world.setBlockState(pos, blockState.with(Properties.WATERLOGGED, false))
            } else if (blockState.fluidState.isOf(Fluids.WATER) || blockState.fluidState.isOf(Fluids.FLOWING_WATER)) {
                world.setBlockState(pos, Blocks.AIR.defaultState, Block.NOTIFY_LISTENERS)
            }
        }

        world.setBlockState(pos, Blocks.AIR.defaultState)
    }

    override fun loadConfig() {
        if (configFile.exists()) {
            runCatching {
                val loadedConfig = JSON.decodeFromString<Config>(configFile.readText())
                config.disabledBlocks.addAll(loadedConfig.disabledBlocks)
                logger.info("disabled blocks ${config.disabledBlocks}")
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

    private fun toggleBlock(block: String, player: PlayerEntity?) {
        if (config.disabledBlocks.contains(block)) {
            config.disabledBlocks.remove(block)
            player?.sendMessage("$block has been removed".literal)
        } else {
            config.disabledBlocks.add(block)
            player?.sendMessage("$block has been added".literal)
        }
    }

    override fun commandCallback(literalCommandBuilder: LiteralCommandBuilder<ServerCommandSource>) {
        literalCommandBuilder.apply {
            literal("toggle") {
                argument<String>("block", StringArgumentType.greedyString()) { block ->
                    suggestList { context ->
                        context.source.world.registryManager.get(RegistryKeys.BLOCK).ids.map { it.toString() }
                    }
                    runs {
                        toggleBlock(block(), this.source.player)
                    }
                }
            }
            literal("test") {
                runs {
                }
            }
        }
    }

    fun handleSetBlockState(
        blockPos: BlockPos,
        blockState: BlockState,
        bl: Boolean,
        cir: CallbackInfoReturnable<BlockState>
    ) {
        val id = Registries.BLOCK.getId(blockState.block)
        val flag = config.disabledBlocks.contains(id.toString())
        if (flag) {
            //logger.info("Not generating $blockPos $blockState")
            cir.returnValue = null
        }
    }

    fun handleSetBlockState(
        i: Int,
        j: Int,
        k: Int,
        blockState: BlockState,
        bl: Boolean,
        cir: CallbackInfoReturnable<BlockState>
    ) {
        val id = Registries.BLOCK.getId(blockState.block)
        val flag = config.disabledBlocks.contains(id.toString())
        if (flag) {
            //logger.info("Not generating $blockState")
            cir.returnValue = null
        }
    }
}
