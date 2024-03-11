package gg.norisk.zerstoerung.modules

import com.mojang.brigadier.arguments.DoubleArgumentType
import gg.norisk.zerstoerung.Destruction
import gg.norisk.zerstoerung.Zerstoerung
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.DefaultAttributeRegistry
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.silkmc.silk.commands.LiteralCommandBuilder

object HeartManager : Destruction("Heart") {
    private var config = Config()

    @Serializable
    private data class Config(
        var disabledHearts: Double = 0.0
    )

    override fun loadConfig() {
        if (configFile.exists()) {
            runCatching {
                val loadedConfig = JSON.decodeFromString<Config>(configFile.readText())
                config = loadedConfig
                Zerstoerung.logger.info("Successfully loaded $name to config file")
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    override fun saveConfig() {
        runCatching {
            loadConfig()
            configFile.writeText(JSON.encodeToString<Config>(config))
            Zerstoerung.logger.info("Successfully saved $name to config file")
        }.onFailure {
            it.printStackTrace()
        }
    }

    override fun commandCallback(literalCommandBuilder: LiteralCommandBuilder<ServerCommandSource>) {
        literalCommandBuilder.apply {
            literal("toggle") {
                argument<Double>("hearts", DoubleArgumentType.doubleArg(0.0, 20.0)) { heartArg ->
                    runs {
                        toggleHearts(heartArg(), this.source.server)
                    }
                }
            }
        }
    }

    fun toggleHearts(amount: Double, server: MinecraftServer) {
        config.disabledHearts = amount
        for (world in server.worlds) {
            for (livingEntity in world.iterateEntities().filterIsInstance<LivingEntity>()) {
                val defaultAttributeContainer =
                    DefaultAttributeRegistry.get(livingEntity.type as EntityType<LivingEntity>)
                if (defaultAttributeContainer.has(EntityAttributes.GENERIC_MAX_HEALTH)) {
                    val baseHealth = defaultAttributeContainer.getBaseValue(EntityAttributes.GENERIC_MAX_HEALTH)
                    val attribute = livingEntity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH) ?: continue
                    attribute.baseValue = baseHealth - amount
                }
            }
        }
    }
}
