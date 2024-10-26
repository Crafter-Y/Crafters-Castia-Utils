package de.craftery.castiautils.compat;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.impl.WailaClientRegistration;

public class JadeIntegration implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // wait for tags to load
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            Registries.BLOCK.stream()
                    .filter(block -> block.getDefaultState().isIn(BlockTags.SIGNS))
                    .forEach(registration::hideTarget); // disable jade for signs

            WailaClientRegistration.instance().reloadIgnoreLists();
        });
    }
}