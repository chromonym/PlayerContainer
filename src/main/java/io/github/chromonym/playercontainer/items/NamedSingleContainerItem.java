package io.github.chromonym.playercontainer.items;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.github.chromonym.playercontainer.containers.AbstractContainer;
import io.github.chromonym.playercontainer.containers.ContainerInstance;
import io.github.chromonym.playercontainer.registries.ItemComponents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class NamedSingleContainerItem<C extends AbstractContainer> extends SimpleContainerItem<C> {

    public NamedSingleContainerItem(C container, Settings settings) {
        super(container, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        // Don't show the name of the player(s) contained
        UUID cont = stack.get(ItemComponents.CONTAINER_ID);
        if (cont != null && type.isAdvanced()) {
            tooltip.add(Text.literal("ID: "+cont.toString()).formatted(Formatting.DARK_GRAY));
        }
    }

    @Override
    public Text getName(ItemStack stack) {
        if (stack.getItem() instanceof AbstractContainerItem<?> aci) {
            ContainerInstance<?> ci = aci.getOrMakeContainerInstance(stack, null, true);
            if (ci != null && ci.getPlayerCount(true) != 0) {
                Set<GameProfile> players = ci.getPlayers(true);
                if (!players.isEmpty()) {
                    GameProfile prof = players.iterator().next();
                    return Text.translatable(this.getTranslationKey(stack)+".named", prof.getName());
                }
			}
        }
        return super.getName(stack);
    }
}
