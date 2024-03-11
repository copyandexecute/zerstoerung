package gg.norisk.zerstoerung.registry

import gg.norisk.zerstoerung.Zerstoerung.toId
import gg.norisk.zerstoerung.item.InvisibleItem
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry

object ItemRegistry {
    val INVISIBLE: Item = registerItem("invisible", InvisibleItem(Item.Settings()))

    fun init() {}

    private fun <I : Item> registerItem(name: String, item: I): I {
        return Registry.register(Registries.ITEM, name.toId(), item)
    }
}
