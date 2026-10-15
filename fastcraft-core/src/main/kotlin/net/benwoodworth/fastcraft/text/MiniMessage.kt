package net.benwoodworth.fastcraft.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder

/** Parse plugin templates with literal values and native item components. */
fun miniMessage(
    template: String,
    unparsed: Map<String, String> = emptyMap(),
    components: Map<String, Component> = emptyMap(),
): Component {
    require(unparsed.keys.intersect(components.keys).isEmpty()) { "Duplicate MiniMessage placeholder" }
    val literals = unparsed.map { (name, value) -> Placeholder.unparsed(name, value) }
    val items = components.map { (name, value) -> Placeholder.component(name, value) }
    return MiniMessage.miniMessage().deserialize(template, *(literals + items).toTypedArray())
        .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
}
