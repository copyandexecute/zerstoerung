package gg.norisk.zerstoerung

import gg.norisk.zerstoerung.modules.StructureManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
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
    private val configFile = File(configFolder, "config.json")

    override fun onInitialize() {
        modules.forEach(Destruction::init)
        initServerCommands()
        initConfig()
        ServerLifecycleEvents.SERVER_STOPPING.register {
            saveConfig()
        }
    }

    override fun onInitializeClient() {
    }

    override fun onInitializeServer() {
    }

    private fun saveConfig() {
        runCatching {
            val modules = modules.filter { it.isEnabled }
            configFile.writeText(Json.encodeToString(modules.map { it.name }))
            logger.info("saved ${modules.size} modules")
            modules.forEach(Destruction::onDisable)
        }
    }

    private fun initConfig() {
        if (configFile.exists()) {
            for (moduleName in Json.decodeFromString<List<String>>(configFile.readText())) {
                modules.find { it.name == moduleName }?.onEnable()
            }
        }
    }

    private fun initServerCommands() {
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
}
