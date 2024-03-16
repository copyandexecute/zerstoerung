package gg.norisk.zerstoerung.modules

import com.mojang.brigadier.arguments.StringArgumentType
import gg.norisk.zerstoerung.Destruction
import gg.norisk.zerstoerung.Zerstoerung.logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.random.Random
import net.minecraft.world.StructureWorldAccess
import net.minecraft.world.gen.chunk.ChunkGenerator
import net.minecraft.world.gen.feature.PlacedFeature
import net.silkmc.silk.commands.LiteralCommandBuilder
import net.silkmc.silk.core.text.literalText

object FeatureManager : Destruction("Feature") {
    private var config = Config()

    @Serializable
    private data class Config(
        val possibleFeatures: MutableSet<String> = mutableSetOf(),
        val disablesFeatures: MutableSet<String> = mutableSetOf()
    )

    override fun onEnable(server: MinecraftServer) {
        super.onEnable(server)
        config.possibleFeatures.addAll(server.registryManager.get(RegistryKeys.CONFIGURED_FEATURE).ids.map { it.toString() })
    }

    override fun destroy() {
        val remainingFeatures = config.possibleFeatures.filter { !config.disablesFeatures.contains(it) }.toList()
        if (remainingFeatures.isNotEmpty()) {
            val randomFeature = remainingFeatures.random()
            config.disablesFeatures.add(randomFeature)
            broadcastDestruction(literalText {
                text(
                    randomFeature.split(":")[1].replace("_", " ")
                ) {
                    color = 0xff5733
                }
            })
        }
    }

    fun PlacedFeature.handleFeatureGeneration(
        world: StructureWorldAccess,
        generator: ChunkGenerator,
        random: Random,
        pos: BlockPos
    ): Boolean {
        if (feature.key.isPresent) {
            if (config.disablesFeatures.any { it == feature.key.get().value.toString() } && isEnabled) {
                return false
            }
        }
        return generate(world, generator, random, pos)
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

    override fun commandCallback(literalCommandBuilder: LiteralCommandBuilder<ServerCommandSource>) {
        literalCommandBuilder.apply {
            literal("toggle") {
                argument<String>("feature", StringArgumentType.greedyString()) { feature ->
                    suggestList { context ->
                        context.source.world.registryManager.get(RegistryKeys.CONFIGURED_FEATURE).ids.map { it.toString() }
                    }
                    runs {
                        config.disablesFeatures.add(feature())
                    }
                }
            }
        }
    }
}
