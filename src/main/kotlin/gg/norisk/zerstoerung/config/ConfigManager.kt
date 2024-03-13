package gg.norisk.zerstoerung.config

import gg.norisk.zerstoerung.Destruction
import gg.norisk.zerstoerung.Zerstoerung
import gg.norisk.zerstoerung.Zerstoerung.modules
import gg.norisk.zerstoerung.mixin.world.PersistenStateManagerAccessor
import gg.norisk.zerstoerung.util.RandomCollection
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import net.minecraft.world.Difficulty
import net.minecraft.world.GameRules
import java.io.File

object ConfigManager {
    var config = Config()

    lateinit var configFolder: File
    lateinit var configFile: File

    var weightedModules = RandomCollection<Destruction>()

    @Serializable
    data class Config(
        val possibleModules: List<String> = modules.map { it.name },
        var moduleWeight: List<Pair<Double, String>> = buildList {
            val weight = 1.0 / possibleModules.size
            for (possibleModule in possibleModules) {
                add(Pair(weight, possibleModule))
            }
        },
        var radius: Int = 15
    )

    fun init() {
        ServerLifecycleEvents.SERVER_STOPPED.register {
            saveConfig()
        }
        ServerLifecycleEvents.SERVER_STARTED.register {
            Zerstoerung.logger.info("server started...")
            initConfig(it)
            //just for recording
            if (FabricLoader.getInstance().isDevelopmentEnvironment) {
                it.setDifficulty(Difficulty.PEACEFUL, true)
                it.overworld.timeOfDay = 6000
                it.gameRules.get(GameRules.DO_DAYLIGHT_CYCLE).set(false, it)
                it.gameRules.get(GameRules.DO_WEATHER_CYCLE).set(false, it)
            }
        }
    }

    fun reload() {
        config = if (configFile.exists()) {
            Destruction.JSON.decodeFromString<Config>(configFile.readText())
        } else {
            Config()
        }
        reloadWeightedModules()
    }

    private fun reloadWeightedModules() {
        weightedModules = RandomCollection()
        for ((weight, moduleName) in config.moduleWeight) {
            val module = modules.find { it.name.equals(moduleName, true) } ?: continue
            weightedModules.add(weight, module)
        }
    }

    private fun initConfig(server: MinecraftServer) {
        configFolder = File(
            (server.overworld.persistentStateManager as PersistenStateManagerAccessor).directory.parentFile,
            "zerstoerung"
        ).apply { mkdirs() }
        configFile = File(configFolder, "config.json")
        reload()
        saveConfig()
    }

    fun saveConfig() {
        runCatching {
            configFile.writeText(Destruction.JSON.encodeToString(config))
            for (module in modules) {
                module.onDisable()
            }
        }
    }
}
