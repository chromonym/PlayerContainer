package io.github.chromonym.playercontainer.items;

import io.github.chromonym.playercontainer.registries.ItemComponents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class NamedItem extends Item {
    
    public NamedItem(Settings settings) {
        super(settings);
    }

    @Override
    public Text getName(ItemStack stack) {
        String name = stack.get(ItemComponents.PLAYER_NAME);
        if (name != null) {
            return Text.translatable(this.getTranslationKey(stack)+".named", name);
        }
        return super.getName(stack);
    }

}
