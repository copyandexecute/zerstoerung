package gg.norisk.zerstoerung.modules

import com.mojang.brigadier.arguments.StringArgumentType
import gg.norisk.zerstoerung.Destruction
import gg.norisk.zerstoerung.Zerstoerung.logger
import gg.norisk.zerstoerung.config.ConfigManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.particle.ParticleTypes
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Identifier
import net.minecraft.util.math.Box
import net.silkmc.silk.commands.LiteralCommandBuilder
import net.silkmc.silk.core.text.literalText
import java.util.*

object EntityManager : Destruction("Entity") {
    private var config = Config()

    @Serializable
    private data class Config(
        val possibleEntities: MutableSet<String> = Registries.ENTITY_TYPE.ids.map { Registries.ENTITY_TYPE[it] }
            .asSequence()
            .filter { it != EntityType.PLAYER }
            .filter { it != EntityType.ITEM }
            .filter { it != EntityType.ENDER_DRAGON }
            .map { Registries.ENTITY_TYPE.getId(it) }
            .map { it.toString() }.toMutableSet(),
        val disabledEntities: MutableSet<String> = mutableSetOf()
    )

    override fun onEnable(server: MinecraftServer) {
        super.onEnable(server)
    }

    override fun tickServerWorld(world: ServerWorld) {
        val disabledEntites = config.disabledEntities.toList()
        world.players.forEach { player ->
            for (entity in player.world.getOtherEntities(
                player,
                Box.from(player.pos).expand(ConfigManager.config.radius.toDouble())
            ) {
                return@getOtherEntities (disabledEntites.contains(Registries.ENTITY_TYPE.getId(it.type).toString()))
            }) {
                entity.destroy()
            }
        }
    }

    fun Entity.destroy() {
        val random = Random()
        val serverWorld = world as? ServerWorld?
        serverWorld?.playSound(
            null,
            blockPos,
            SoundEvents.ENTITY_BREEZE_SHOOT,
            SoundCategory.HOSTILE,
            0.5f,
            0.5f
        )
        repeat(10) {
            serverWorld?.spawnParticles(
                ParticleTypes.GUST_DUST,
                blockPos.x.toDouble() + 0.5 + random.nextDouble() / 3.0 * (if (random.nextBoolean()) 1 else -1).toDouble(),
                blockPos.y.toDouble() + random.nextDouble() + random.nextDouble(),
                blockPos.z.toDouble() + 0.5 + random.nextDouble() / 3.0 * (if (random.nextBoolean()) 1 else -1).toDouble(),
                0,
                0.0, 0.0, 0.0, 1.0
            )
        }
        discard()
    }

    override fun destroy() {
        val remainingEntities = config.possibleEntities.filter { !config.disabledEntities.contains(it) }.toList()
        if (remainingEntities.isNotEmpty()) {
            val randomEntity = remainingEntities.random()
            config.disabledEntities.add(randomEntity)
            broadcastDestruction(literalText {
                text(Registries.ENTITY_TYPE[Identifier(randomEntity)].name) {
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

    override fun commandCallback(literalCommandBuilder: LiteralCommandBuilder<ServerCommandSource>) {
        literalCommandBuilder.apply {
            literal("toggle") {
                argument<String>("entity", StringArgumentType.greedyString()) { entity ->
                    suggestList {
                        config.possibleEntities
                    }
                    runs {
                        config.disabledEntities.add(entity())
                    }
                }
            }
        }
    }
}
