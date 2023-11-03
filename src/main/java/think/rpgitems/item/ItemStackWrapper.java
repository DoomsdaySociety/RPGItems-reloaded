package think.rpgitems.item;

import cat.nyaa.nyaacore.utils.ItemTagUtils;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

import static think.rpgitems.item.RPGItem.*;

public class ItemStackWrapper {
    ItemStack handle;
    Optional<String> itemUuid;

    private static HashMap<String, ItemStackWrapper> wrapperMap = new HashMap<>();

    public static ItemStackWrapper of(ItemStack itemStack){
        if (itemStack == null){
            throw new NullPointerException();
        }
        Optional<String> itemUuid = ItemTagUtils.getString(itemStack, NBT_ITEM_UUID);
        if (itemUuid.isEmpty()){
            return new ItemStackWrapper(itemStack);
        }
        return wrapperMap.computeIfAbsent(itemUuid.get(), (u) -> new ItemStackWrapper(itemStack));
    }

    private ItemStackWrapper(ItemStack itemStack){
        handle = itemStack;
        itemUuid = ItemTagUtils.getString(itemStack, NBT_ITEM_UUID);
    }

    @Override
    public int hashCode() {
        return itemUuid.map(String::hashCode).orElseGet(() -> handle.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        Optional<String> toCmpUuid;
        if ((obj instanceof ItemStack toCmp)){
            toCmpUuid = ItemTagUtils.getString(toCmp, NBT_ITEM_UUID);
        }else if(obj instanceof ItemStackWrapper){
            toCmpUuid = ((ItemStackWrapper) obj).itemUuid;
        }else {
            return false;
        }

        if (itemUuid.isPresent()){
            if (toCmpUuid.isEmpty()){
                return false;
            }
            String uuid = itemUuid.get();
            return Objects.equals(uuid, toCmpUuid.get());
        }
        return Objects.equals(handle, obj);
    }
}
