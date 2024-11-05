package de.craftery.castiautils.compat;

import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import red.jackf.chesttracker.api.ChestTrackerPlugin;
import red.jackf.chesttracker.api.memory.CommonKeys;
import red.jackf.chesttracker.api.providers.MemoryBuilder;
import red.jackf.chesttracker.api.providers.MemoryKeyIcon;
import red.jackf.chesttracker.api.providers.defaults.DefaultIcons;
import red.jackf.chesttracker.api.providers.defaults.DefaultProviderCommandSent;
import red.jackf.chesttracker.api.providers.defaults.DefaultProviderScreenClose;
import red.jackf.jackfredlib.api.base.ResultHolder;


import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ChestTrackerIntegration implements ChestTrackerPlugin {
    private static final Identifier TOWN_VAULT = Identifier.of("castiautils", "town_vault");
    private static final Identifier PRIVATE_VAULT = Identifier.of("castiautils", "private_vault");

    private static final Set<String> townVaultCommands = Set.of("town vault", "town v", "t vault", "t v");

    AtomicInteger townVault = new AtomicInteger(0);
    AtomicInteger privateVault = new AtomicInteger(0);

    @Override
    public void load() {
        DefaultIcons.registerIconBelow(CommonKeys.ENDER_CHEST_KEY, new MemoryKeyIcon(TOWN_VAULT, Items.TRAPPED_CHEST.getDefaultStack()));
        DefaultIcons.registerIconBelow(TOWN_VAULT, new MemoryKeyIcon(PRIVATE_VAULT, Items.BARREL.getDefaultStack()));

        DefaultProviderScreenClose.EVENT.register((provider, context) -> {
            String title = context.getScreen().getTitle().getString();

            if (title.length() == 2 && title.charAt(0) > 50000 & title.charAt(1) > 50000) return ResultHolder.empty();

            if (title.equals("Town Vault")) {
                if (townVault.get() == 0) {
                    // this is the case when the user types /town vault and then selected the vault manually
                    // TODO: handle inventory clicks to determine vault
                    return ResultHolder.empty();
                }
                return ResultHolder.value(MemoryBuilder.create(context.getItems()).toResult(TOWN_VAULT, new BlockPos(townVault.get(), 0, 0)));
            }
            if (title.startsWith("Vault #")) {
                if (townVault.get() == 0) {
                    return ResultHolder.empty();
                }
                return ResultHolder.value(MemoryBuilder.create(context.getItems()).toResult(PRIVATE_VAULT, new BlockPos(privateVault.get(), 0, 0)));
            }

            return ResultHolder.pass();
        });

        DefaultProviderCommandSent.EVENT.register((provider, command) -> {
            if (townVaultCommands.stream().anyMatch(command::startsWith)) {
                String[] parts = command.split(" ");
                if (parts.length == 3) {
                    townVault.set(Integer.parseInt(parts[2]));
                } else {
                    townVault.set(0);
                }
            } else if (command.startsWith("pv")) {
                String[] parts = command.split(" ");
                if (parts.length == 2) {
                    privateVault.set(Integer.parseInt(parts[1]));
                } else {
                    privateVault.set(0);
                }
            }
        });
    }
}
