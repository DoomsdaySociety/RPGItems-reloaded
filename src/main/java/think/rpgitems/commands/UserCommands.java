package think.rpgitems.commands;

import think.rpgitems.RPGItems;
import think.rpgitems.utils.nyaacore.LanguageRepository;
import think.rpgitems.utils.nyaacore.cmdreceiver.Arguments;
import think.rpgitems.utils.nyaacore.cmdreceiver.BadCommandException;
import think.rpgitems.utils.nyaacore.cmdreceiver.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Completion;
import think.rpgitems.power.RPGCommandReceiver;

import java.util.Optional;

import static think.rpgitems.item.RPGItem.TAG_IS_MODEL;
import static think.rpgitems.item.RPGItem.TAG_META;
import static think.rpgitems.utils.ItemTagUtils.getTag;
import static think.rpgitems.utils.ItemTagUtils.optBoolean;

public class UserCommands extends RPGCommandReceiver {
    private final RPGItems plugin;

    public UserCommands(RPGItems plugin, LanguageRepository i18n) {
        super(plugin, i18n);
        this.plugin = plugin;
    }

    @Override
    public String getHelpPrefix() {
        return "";
    }

    @SubCommand(value = "info", permission = "info")
    @Completion("command")
    public void info(CommandSender sender, Arguments args) {
        Player p = asPlayer(sender);
        RPGItem item = getItem(sender, false);
        ItemStack itemStack = p.getInventory().getItemInMainHand();
        item.print(sender, false);
        PersistentDataContainer tagContainer = itemStack.getItemMeta().getPersistentDataContainer();
        PersistentDataContainer metaTag = getTag(tagContainer, TAG_META);
        Optional<Boolean> optIsModel = optBoolean(metaTag, TAG_IS_MODEL);
        if (optIsModel.orElse(false)) {
            msg(p, "message.model.is");
        }
    }

    @SubCommand(value = "tomodel", permission = "tomodel")
    @Completion("command")
    public void toModel(CommandSender sender, Arguments args) {
        Player p = asPlayer(sender);
        RPGItem item = getItem(sender);
        ItemStack itemStack = p.getInventory().getItemInMainHand();
        item.toModel(itemStack);
        p.getInventory().setItemInMainHand(itemStack);
        msg(p, "message.model.to");
    }

    private RPGItem getItem(CommandSender sender) {
        return getItem(sender, true);
    }

    private RPGItem getItem(CommandSender sender, boolean ignoreModel) {
        Player p = (Player) sender;
        Optional<RPGItem> item = ItemManager.toRPGItem(p.getInventory().getItemInMainHand(), ignoreModel);
        if (item.isPresent()) {
            return item.get();
        } else {
            throw new BadCommandException("message.error.iteminhand");
        }
    }
}
