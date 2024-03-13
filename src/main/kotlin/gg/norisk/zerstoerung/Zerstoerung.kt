package gg.norisk.zerstoerung

import com.mojang.brigadier.arguments.IntegerArgumentType
import gg.norisk.zerstoerung.config.ConfigManager
import gg.norisk.zerstoerung.modules.BlockManager
import gg.norisk.zerstoerung.modules.InventoryManager
import gg.norisk.zerstoerung.modules.StructureManager
import gg.norisk.zerstoerung.registry.ItemRegistry
import kotlinx.coroutines.Job
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.api.ModInitializer
import net.minecraft.util.Identifier
import net.silkmc.silk.commands.PermissionLevel
import net.silkmc.silk.commands.command
import net.silkmc.silk.core.kotlin.ticks
import net.silkmc.silk.core.task.infiniteMcCoroutineTask
import net.silkmc.silk.core.text.literal
import org.apache.logging.log4j.LogManager

object Zerstoerung : ModInitializer, DedicatedServerModInitializer, ClientModInitializer {
    val logger = LogManager.getLogger("zerstoerung")
    val modules = listOf(StructureManager, BlockManager, InventoryManager)
    var shuffleTimer: Job? = null

    override fun onInitialize() {
        ItemRegistry.init()
        ConfigManager.init()
        modules.forEach(Destruction::init)
        initServerCommands()
    }

    override fun onInitializeClient() {
    }

    override fun onInitializeServer() {
    }

    fun startShuffleTimer(period: Int) {
        shuffleTimer?.cancel()
        shuffleTimer = infiniteMcCoroutineTask(period = period.ticks) {
            shuffle()
        }
    }

    private fun shuffle() {
        val destruction = modules.random()
        destruction.destroy()
    }

    private fun initServerCommands() {
        command("zerstoerung") {
            requiresPermissionLevel(PermissionLevel.OWNER)
            literal("reload") {
                runs {
                    ConfigManager.reload()
                    this.source.sendMessage("Reloaded config...".literal)
                }
            }
            literal("start") {
                argument<Int>("period", IntegerArgumentType.integer(0)) { period ->
                    runs {
                        startShuffleTimer(period())
                    }
                }
            }
            literal("stop") {
                runs {
                    shuffleTimer?.cancel()
                }
            }
            for (module in modules) {
                literal(module.name) {
                    module.commandCallback(this)
                    literal("destroy") {
                        runs {
                            module.destroy()
                        }
                    }
                    literal("onEnable") {
                        runs {
                            module.onEnable(this.source.server)
                        }
                    }
                    literal("onDisable") {
                        runs {
                            module.onDisable()
                        }
                    }
                    literal("saveConfig") {
                        runs {
                            module.saveConfig()
                        }
                    }
                    literal("loadConfig") {
                        runs {
                            module.loadConfig()
                        }
                    }
                }
            }
        }
    }

    fun String.toId(): Identifier {
        return Identifier("zerstoerung", this)
    }
}
