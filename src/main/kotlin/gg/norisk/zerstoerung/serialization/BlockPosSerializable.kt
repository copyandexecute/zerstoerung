package gg.norisk.zerstoerung.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.util.math.BlockPos
import kotlin.properties.Delegates

// Definition des Serialisierers für die BlockPos-Klasse
object BlockPosSerializer : KSerializer<BlockPos> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("BlockPos") {
            element<Int>("x")
            element<Int>("y")
            element<Int>("z")
        }

    override fun serialize(encoder: Encoder, value: BlockPos) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeIntElement(descriptor, 0, value.x)
        composite.encodeIntElement(descriptor, 1, value.y)
        composite.encodeIntElement(descriptor, 2, value.z)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): BlockPos {
        val composite = decoder.beginStructure(descriptor)
        var x by Delegates.notNull<Int>()
        var y by Delegates.notNull<Int>()
        var z by Delegates.notNull<Int>()
        loop@ while (true) {
            when (val index = composite.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> x = composite.decodeIntElement(descriptor, index)
                1 -> y = composite.decodeIntElement(descriptor, index)
                2 -> z = composite.decodeIntElement(descriptor, index)
                else -> throw SerializationException("Unknown index: $index")
            }
        }
        composite.endStructure(descriptor)
        return BlockPos(x, y, z)
    }
}
