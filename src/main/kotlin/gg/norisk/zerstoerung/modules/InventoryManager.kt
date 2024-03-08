package gg.norisk.zerstoerung.modules

import com.mojang.brigadier.arguments.IntegerArgumentType
import gg.norisk.zerstoerung.Destruction
import gg.norisk.zerstoerung.Zerstoerung
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.Items
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.silkmc.silk.commands.LiteralCommandBuilder
import net.silkmc.silk.core.server.players

object InventoryManager : Destruction("Inventory") {
    private val config = Config()

    @Serializable
    private data class Config(
        val disabledSlots: MutableMap<InventoryType, MutableSet<Int>> = mutableMapOf()
    )

    private enum class InventoryType(@Transient val intRange: IntRange) {
        PLAYER(0..45), STORAGE(0..53)
    }

    override fun loadConfig() {
        if (configFile.exists()) {
            runCatching {
                val loadedConfig = JSON.decodeFromString<Config>(configFile.readText())

                loadedConfig.disabledSlots.forEach { (type, slots) ->
                    config.disabledSlots.computeIfAbsent(type) { mutableSetOf() }.addAll(slots)
                }
                Zerstoerung.logger.info("disabled slots ${config.disabledSlots.values}")
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
                for (type in InventoryType.entries) {
                    literal(type.name.lowercase()) {
                        argument<Int>(
                            "slot",
                            IntegerArgumentType.integer(type.intRange.first, type.intRange.last)
                        ) { slot ->
                            runs {
                                toggleSlot(type, slot(), this.source.server)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onEnable(server: MinecraftServer) {
        super.onEnable(server)
        disablePlayerSlots(server)
    }

    private fun disablePlayerSlots(server: MinecraftServer) {
        val disabledSlots = config.disabledSlots[InventoryType.PLAYER] ?: return
        for (player in server.players) {
            for (disabledSlot in disabledSlots) {
                player.inventory.setStack(disabledSlot, Items.BARRIER.defaultStack)
            }
        }
    }

    private fun toggleSlot(type: InventoryType, slot: Int, server: MinecraftServer) {
        if (type == InventoryType.PLAYER) {
            for (player in server.players) {
                if (slot in 36..44) {
                    val realSlot = slot - 36
                    player.inventory.setStack(realSlot, Items.BARRIER.defaultStack)
                } else if (slot == 45) {
                    player.equipStack(EquipmentSlot.OFFHAND, Items.BARRIER.defaultStack)
                } else if (slot in 5..8) {
                    val slots = mapOf(
                        5 to EquipmentSlot.HEAD,
                        6 to EquipmentSlot.CHEST,
                        7 to EquipmentSlot.LEGS,
                        8 to EquipmentSlot.FEET
                    )
                    player.equipStack(slots[slot], Items.BARRIER.defaultStack)
                } else {
                    player.inventory.setStack(slot, Items.BARRIER.defaultStack)
                }
            }
            val disabledSlots = config.disabledSlots.computeIfAbsent(type) { mutableSetOf() }
            disabledSlots.add(slot)
        }
    }
}
