package gg.norisk.zerstoerung

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.api.ModInitializer
import net.silkmc.silk.commands.PermissionLevel
import net.silkmc.silk.commands.command
import org.apache.logging.log4j.LogManager
import java.io.File

object Zerstoerung : ModInitializer, DedicatedServerModInitializer, ClientModInitializer {
    val logger = LogManager.getLogger("zerstoerung")
    val configFolder = File("config", "zerstoerung").apply {
        mkdirs()
    }
    val modules = listOf(StructureManager)

    override fun onInitialize() {
        modules.forEach(Destruction::init)
        command("zerstoerung") {
            requiresPermissionLevel(PermissionLevel.OWNER)
            for (module in modules) {
                literal(module.name) {
                    module.commandCallback(this)
                    literal("onEnable") {
                        runs {
                            module.onEnable()
                        }
                    }
                    literal("onDisable") {
                        runs {
                            module.onDisable()
                        }
                    }
                }
            }
        }
    }

    override fun onInitializeClient() {
    }

    override fun onInitializeServer() {
    }
}
