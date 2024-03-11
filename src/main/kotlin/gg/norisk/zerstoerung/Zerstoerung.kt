package gg.norisk.zerstoerung

import gg.norisk.zerstoerung.mixin.world.PersistenStateManagerAccessor
import gg.norisk.zerstoerung.modules.BlockManager
import gg.norisk.zerstoerung.modules.InventoryManager
import gg.norisk.zerstoerung.modules.HeartManager
import gg.norisk.zerstoerung.modules.StructureManager
import gg.norisk.zerstoerung.registry.ItemRegistry
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import net.minecraft.world.Difficulty
import net.minecraft.world.GameRules
import net.silkmc.silk.commands.PermissionLevel
import net.silkmc.silk.commands.command
import org.apache.logging.log4j.LogManager
import java.io.File

object Zerstoerung : ModInitializer, DedicatedServerModInitializer, ClientModInitializer {
    val logger = LogManager.getLogger("zerstoerung")
    val modules = listOf(StructureManager, BlockManager, InventoryManager, HeartManager)
    lateinit var configFolder: File
    lateinit var configFile: File
    private var config = Config()

    @Serializable
    private data class Config(
        var enabledModules: MutableSet<String> = mutableSetOf(),
        var radius: Int = 15
    )

    fun radius(): Int {
        return config.radius
    }

    override fun onInitialize() {
        ItemRegistry.init()

        modules.forEach(Destruction::init)
        initServerCommands()
        ServerLifecycleEvents.SERVER_STARTED.register {
            logger.info("server started...")
            initConfig(it)
            //just for recording
            if (FabricLoader.getInstance().isDevelopmentEnvironment) {
                it.setDifficulty(Difficulty.PEACEFUL, true)
                it.overworld.timeOfDay = 6000
                it.gameRules.get(GameRules.DO_DAYLIGHT_CYCLE).set(false, it)
                it.gameRules.get(GameRules.DO_WEATHER_CYCLE).set(false, it)
            }
        }
        ServerLifecycleEvents.SERVER_STOPPED.register {
            saveConfig()
        }
    }

    override fun onInitializeClient() {
    }

    override fun onInitializeServer() {
    }

    private fun saveConfig() {
        runCatching {
            config.enabledModules = modules.filter { it.isEnabled }.map { it.name }.toMutableSet()
            configFile.writeText(Json.encodeToString(config))
            for (module in modules) {
                if (module.isEnabled) {
                    module.onDisable()
                } else {
                    module.saveConfig()
                }
            }
            logger.info("saved ${config.enabledModules.size} modules...")
        }
    }

    private fun initConfig(server: MinecraftServer) {
        val world = server.overworld
        configFolder = File(
            (world.persistentStateManager as PersistenStateManagerAccessor).directory.parentFile,
            "zerstoerung"
        ).apply { mkdirs() }
        logger.info("found config folder $configFolder")
        configFile = File(configFolder, "config.json")
        if (configFile.exists()) {
            config = Json.decodeFromString<Config>(configFile.readText())
            logger.info("loading ${modules.size} modules...")
            for (moduleName in config.enabledModules) {
                modules.find { it.name == moduleName }?.onEnable(server)
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
