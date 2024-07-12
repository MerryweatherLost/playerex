package com.bibireden.playerex.factory

import com.bibireden.opc.api.OfflinePlayerCacheAPI
import com.bibireden.opc.cache.OfflinePlayerCache
import com.bibireden.playerex.api.PlayerEXAPI
import com.bibireden.playerex.api.PlayerEXCachedKeys
import com.bibireden.playerex.api.attribute.PlayerEXAttributes
import eu.pb4.placeholders.api.PlaceholderHandler
import eu.pb4.placeholders.api.PlaceholderResult
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper

object PlaceholderFactory {
    public val STORE: MutableMap<Identifier, PlaceholderHandler> = mutableMapOf();

    private fun nameLevelPair(server: MinecraftServer, namesIn: Collection<String>, indexIn: Int): Pair<String, Int>
    {
        val cache = OfflinePlayerCache.get(server);

        if (cache !== null) {
            val names: ArrayList<Pair<String, Int>> = arrayListOf();

            var i = 0

            for (name: String in namesIn) {
                val cachedData = cache.get(server, name, PlayerEXCachedKeys.LEVEL_KEY) ?: continue

                names[i] = Pair(name, cachedData)
                i++
            }

            names.sortWith(Comparator.comparing { (_, level) -> level })

            val j = MathHelper.clamp(indexIn, 1, names.size)

            return names[names.size - j]
        }

        return Pair("", 0)
    }

    private fun top(stringFunction: (Pair<String, Int>) -> String): PlaceholderHandler
    {
        return PlaceholderHandler { ctx, arg ->
            val server = ctx.server()
            val cache = OfflinePlayerCache.get(server) ?: return@PlaceholderHandler PlaceholderResult.invalid("Improper cache")

            var index: Int = 1;

            val names: Collection<String> = cache.usernames(server)

            if (arg !== null)
            {
                try {
                    val i: Int = arg.toInt();
                    index = Math.max(1, i);
                } catch (e: NumberFormatException)
                {
                    return@PlaceholderHandler PlaceholderResult.invalid("Invalid argument!")
                }
            }

            if (index > names.size) return@PlaceholderHandler PlaceholderResult.value("")
            val pair: Pair<String, Int> = this.nameLevelPair(server, names, index)
            return@PlaceholderHandler PlaceholderResult.value(stringFunction.invoke(pair));
        }
    }

    init {
        // Implement Store adds
        STORE.put(Identifier.of(), {ctx, args ->
            val player: ServerPlayerEntity = ctx.player

            player ?: return@put PlaceholderResult.value("No player!");
        })
    }
}