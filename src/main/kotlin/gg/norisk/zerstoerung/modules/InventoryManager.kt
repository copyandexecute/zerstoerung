package gg.norisk.zerstoerung.modules

import com.mojang.brigadier.arguments.IntegerArgumentType
import gg.norisk.zerstoerung.Destruction
import gg.norisk.zerstoerung.Zerstoerung
import gg.norisk.zerstoerung.Zerstoerung.toId
import gg.norisk.zerstoerung.registry.ItemRegistry
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.CraftingScreen
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.render.RenderLayer
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.silkmc.silk.commands.LiteralCommandBuilder
import net.silkmc.silk.core.Silk.server
import net.silkmc.silk.core.server.players
import net.silkmc.silk.core.text.literalText
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

object InventoryManager : Destruction("Inventory") {
    private var config = Config()

    @Serializable
    private data class Config(
        val disabledSlots: MutableMap<InventoryType, MutableSet<Int>> = mutableMapOf()
    )

    val blockedItem = ItemRegistry.INVISIBLE

    private enum class InventoryType(@Transient val intRange: IntRange) {
        PLAYER(0..45), STORAGE(0..53), CRAFTING(0..9)
    }

    val LEFT = "textures/hotbar_left.png".toId()
    val NORMAL = "textures/hotbar_normal.png".toId()
    val RIGHT = "textures/hotbar_right.png".toId()

    override fun loadConfig() {
        if (configFile.exists()) {
            runCatching {
                config = JSON.decodeFromString<Config>(configFile.readText())
                Zerstoerung.logger.info("disabled slots ${config.disabledSlots.values}")
                Zerstoerung.logger.info("Successfully loaded $name to config file")
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    override fun saveConfig() {
        runCatching {
            configFile.writeText(JSON.encodeToString<Config>(config))
            Zerstoerung.logger.info("Successfully saved $name to config file")
        }.onFailure {
            it.printStackTrace()
        }
    }

    override fun destroy() {
        val randomType = getRemainingType() ?: return
        val disabledSlots = config.disabledSlots.computeIfAbsent(randomType) { mutableSetOf() }
        val possibleSlots = randomType.intRange.toList()
        val remainingSlots = possibleSlots.filter { !disabledSlots.contains(it) }.toList()
        if (remainingSlots.isNotEmpty()) {
            val randomSlot = remainingSlots.random()
            server?.let { toggleSlot(randomType, randomSlot, it) }
            broadcastDestruction(literalText {
                text(
                    "${randomType.name}[$randomSlot]"
                ) {
                    color = 0xff5733
                }
            })
        }
    }

    private fun getRemainingType(): InventoryType? {
        val possibleTypes = mutableListOf<InventoryType>()
        for (type in InventoryType.entries) {
            val disabledSlots = config.disabledSlots.computeIfAbsent(type) { mutableSetOf() }
            val possibleSlots = type.intRange.toList()
            val remainingSlots = possibleSlots.filter { !disabledSlots.contains(it) }.toList()
            if (remainingSlots.isNotEmpty()) {
                possibleTypes += type
            }
        }

        return possibleTypes.randomOrNull()
    }

    override fun commandCallback(literalCommandBuilder: LiteralCommandBuilder<ServerCommandSource>) {
        literalCommandBuilder.apply {
            literal("reset") {
                runs {
                    config.disabledSlots.clear()
                    saveConfig()
                    loadConfig()
                }
            }
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
                player.inventory.setStack(disabledSlot, blockedItem.defaultStack)
            }
        }
    }

    private fun toggleSlot(type: InventoryType, slot: Int, server: MinecraftServer) {
        if (type == InventoryType.PLAYER) {
            for (player in server.players) {
                if (slot in 36..44) {
                    val realSlot = slot - 36
                    player.inventory.setStack(realSlot, blockedItem.defaultStack)
                } else if (slot == 45) {
                    player.equipStack(EquipmentSlot.OFFHAND, blockedItem.defaultStack)
                } else if (slot in 5..8) {
                    val slots = mapOf(
                        5 to EquipmentSlot.HEAD,
                        6 to EquipmentSlot.CHEST,
                        7 to EquipmentSlot.LEGS,
                        8 to EquipmentSlot.FEET
                    )
                    player.equipStack(slots[slot], blockedItem.defaultStack)
                } else if (slot in 0..4) {
                    //nichts machen
                } else {
                    player.inventory.setStack(slot, blockedItem.defaultStack)
                }
            }
        }
        val disabledSlots = config.disabledSlots.computeIfAbsent(type) { mutableSetOf() }
        disabledSlots.add(slot)
    }

    fun isSlotBlocked(handledScreen: HandledScreen<ScreenHandler>, slot: Slot?): Boolean {
        if (slot != null) {
            val itemStack = slot.stack

            val shouldBlock = when (handledScreen) {
                is InventoryScreen -> config.disabledSlots[InventoryType.PLAYER]?.contains(slot.id) ?: false
                is CraftingScreen -> config.disabledSlots[InventoryType.CRAFTING]?.contains(slot.id) ?: false
                else -> false
            }

            return (itemStack.isOf(blockedItem) || shouldBlock) && isEnabled
        }

        return false
    }

    fun drawSlot(handledScreen: HandledScreen<ScreenHandler>, drawContext: DrawContext, slot: Slot, ci: CallbackInfo) {
        val i = slot.x - 1
        val j = slot.y - 1

        if (FabricLoader.getInstance().isDevelopmentEnvironment) {
            drawContext.drawText(
                MinecraftClient.getInstance().textRenderer,
                Text.of(slot.id.toString()),
                i,
                j,
                -1,
                true
            )
        }

        if (isSlotBlocked(handledScreen, slot) && isEnabled) {
            drawContext.fillGradient(RenderLayer.getGuiOverlay(), i, j, i + 18, j + 18, -3750202, -3750202, 0)
            ci.cancel()
        }
    }

    fun isHotbarSlotBlocked(slot: Int, player: PlayerEntity): Boolean {
        return player.inventory.getStack(slot)
            .isOf(blockedItem) || (config.disabledSlots[InventoryType.PLAYER]?.contains(slot + 36) == true) && isEnabled
    }

    fun renderHotbar(drawContext: DrawContext, scaledWidth: Int, scaledHeight: Int) {
        var x: Int = scaledWidth / 2 - 91
        val y: Int = scaledHeight - 22

        val player = MinecraftClient.getInstance().player ?: return

        repeat(9) {
            val texture = when (it) {
                0 -> LEFT
                8 -> RIGHT
                else -> NORMAL
            }

            val size = when (texture) {
                LEFT, RIGHT -> 21
                else -> 20
            }

            if (!isHotbarSlotBlocked(it, player)) {
                drawContext.drawTexture(texture, x, y, size, 22, 0f, 0f, size, 22, size, 22)
            }
            x += size
        }
    }
}
