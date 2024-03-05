package gg.norisk.zerstoerung

import gg.norisk.zerstoerung.serialization.BlockPosSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.silkmc.silk.commands.LiteralCommandBuilder
import java.io.File

abstract class Destruction(val name: String) {
    protected var isEnabled = false
    protected val configFile: File = File(Zerstoerung.configFolder, "$name.json")

    open fun init() {
        ServerTickEvents.END_WORLD_TICK.register(ServerTickEvents.EndWorldTick {
            if (isEnabled) tickServerWorld(it)
        })
    }

    open fun tickServerWorld(world: ServerWorld) {

    }

    open fun onEnable() {
        isEnabled = true
    }

    open fun onDisable() {
        isEnabled = false
    }

    open fun commandCallback(literalCommandBuilder: LiteralCommandBuilder<ServerCommandSource>) {}

    companion object {
        val JSON = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            serializersModule = SerializersModule {
                contextual(BlockPos::class, BlockPosSerializer)
            }
        }
    }
}
