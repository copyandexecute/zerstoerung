package gg.norisk.zerstoerung.modules

import com.mojang.brigadier.arguments.StringArgumentType
import gg.norisk.zerstoerung.Destruction
import gg.norisk.zerstoerung.Zerstoerung.logger
import gg.norisk.zerstoerung.config.ConfigManager
import gg.norisk.zerstoerung.modules.EntityManager.destroy
import gg.norisk.zerstoerung.registry.ItemRegistry
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import net.minecraft.entity.EntityType
import net.minecraft.entity.ItemEntity
import net.minecraft.item.Items
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
import java.util.function.Predicate

object ItemManager : Destruction("Item") {
    private var config = Config()

    @Serializable
    private data class Config(
        val possibleItems: MutableSet<String> = Registries.ITEM.ids.map { Registries.ITEM[it] }.asSequence()
            .filter { it != ItemRegistry.INVISIBLE }.filter { it != Items.AIR }.map { Registries.ITEM.getId(it) }
            .map { it.toString() }.toMutableSet(), val disabledItems: MutableSet<String> = mutableSetOf()
    )

    override fun onEnable(server: MinecraftServer) {
        super.onEnable(server)
    }

    override fun tickServerWorld(world: ServerWorld) {
        val disabledItems = config.disabledItems.toList()
        world.players.forEach { player ->
            //remove from inventory
            val amount = player.inventory.remove(Predicate {
                return@Predicate disabledItems.contains(Registries.ITEM.getEntry(it.item).key?.get()?.value.toString())
            }, -1, player.inventory)

            if (amount > 0) {
                world.playSound(
                    null,
                    player.blockPos,
                    SoundEvents.ENTITY_BREEZE_SHOOT,
                    SoundCategory.HOSTILE,
                    0.5f,
                    0.5f
                )
                player.currentScreenHandler.sendContentUpdates()
                player.playerScreenHandler.onContentChanged(player.inventory)
            }

            for (itemEntity in player.world.getOtherEntities(
                player, Box.from(player.pos).expand(ConfigManager.config.radius.toDouble())
            ) {
                return@getOtherEntities it.type == EntityType.ITEM
            }.filterIsInstance<ItemEntity>()) {
                //WTF
                if (disabledItems.contains(Registries.ITEM.getEntry(itemEntity.stack.item).key?.get()?.value.toString())) {
                    itemEntity.destroy()
                }
            }
        }
    }


    override fun destroy() {
        val remainingItems = config.possibleItems.filter { !config.disabledItems.contains(it) }.toList()
        if (remainingItems.isNotEmpty()) {
            val randomFeature = remainingItems.random()
            config.disabledItems.add(randomFeature)
            broadcastDestruction(literalText {
                text(Registries.ITEM[Identifier(randomFeature)].name) {
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
                argument<String>("item", StringArgumentType.greedyString()) { entity ->
                    suggestList {
                        config.possibleItems
                    }
                    runs {
                        config.disabledItems.add(entity())
                    }
                }
            }
        }
    }
}
