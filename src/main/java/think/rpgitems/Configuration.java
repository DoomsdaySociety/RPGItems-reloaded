package think.rpgitems;

import cat.nyaa.nyaacore.configuration.PluginConfigure;
import org.bukkit.plugin.java.JavaPlugin;

public class Configuration extends PluginConfigure {
    private final RPGItems plugin;

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }

    public Configuration(RPGItems plugin) {
        this.plugin = plugin;
    }

    @Serializable
    public String language = "en_US";

    @Serializable(name ="general.locale_inv", alias = "localeInv")
    public boolean localeInv = false;

    @Serializable(name ="command.list.item_per_page", alias = "itemperpage")
    public int itemPerPage = 9;

    @Serializable(name = "support.worldguard")
    public boolean useWorldGuard = true;

    @Serializable(name = "support.wgforcerefresh")
    public boolean wgForceRefresh = false;

    @Serializable(name = "give-perms")
    public boolean givePerms = false;

    @Serializable(name = "gist.token", alias = "githubToken")
    public String githubToken = "";

    @Serializable(name = "gist.publish", alias = "publishGist")
    public boolean publishGist = true;

    @Serializable(name = "item.defaults.numeric_bar", alias = "numericBar")
    public boolean numericBar = false;

    @Serializable(name = "item.defaults.force_bar", alias = "forceBar")
    public boolean forceBar = false;

    @Serializable(name = "item.defaults.license")
    public String defaultLicense;

    @Serializable(name = "item.defaults.note")
    public String defaultNote;

    @Serializable(name = "item.defaults.author")
    public String defaultAuthor;
}