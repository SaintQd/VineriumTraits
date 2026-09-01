package placeholders

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.saintqd.vineriumtraits.VineriumTraits
import java.util.*

class VinTraitsPlaceholders(val plugin : VineriumTraits) : PlaceholderExpansion() {

    companion object {
        var instance : VinTraitsPlaceholders? = null
    }

    private val placeholders = hashMapOf<String, (VineriumTraits, OfflinePlayer) -> String>()

    override fun persist(): Boolean {
        return true
    }

    override fun canRegister(): Boolean {
        return true
    }

    override fun getIdentifier(): String {
        return "vineriumtraits"
    }

    override fun getAuthor(): String {
        return plugin.pluginMeta.authors.toString()
    }

    override fun getVersion(): String {
        return plugin.pluginMeta.version
    }

    override fun onPlaceholderRequest(player: Player?, identifier: String): String? {
        return if (player == null) {
            ""
        } else placeholders[identifier.lowercase(Locale.getDefault())]?.invoke(plugin,player)
    }

    fun registerPlaceholders() {

    }

    fun registerPlaceholder(identifier: String, function : (VineriumTraits, OfflinePlayer) -> String) {
        placeholders[identifier] = function
    }
}