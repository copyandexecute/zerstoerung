package gg.norisk.zerstoerung

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.api.ModInitializer
import org.apache.logging.log4j.LogManager

object Zerstoerung: ModInitializer, DedicatedServerModInitializer, ClientModInitializer {
    val logger = LogManager.getLogger("examplemod")

    override fun onInitialize() {
    }

    override fun onInitializeClient() {
    }

    override fun onInitializeServer() {
    }
}
