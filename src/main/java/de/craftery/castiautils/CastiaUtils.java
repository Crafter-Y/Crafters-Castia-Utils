package de.craftery.castiautils;

import de.craftery.castiautils.adblock.Adblocker;
import de.craftery.castiautils.ah.AhLogger;
import de.craftery.castiautils.api.AdditionalDataTooltip;
import de.craftery.castiautils.api.RefetchService;
import de.craftery.castiautils.chestshop.*;
import de.craftery.castiautils.config.CastiaConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CastiaUtils implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("castiautils");

    @Override
    public void onInitialize() {
        LOGGER.info("Starting up Crafters Castia Utils (castiautils)");

        AutoConfig.register(CastiaConfig.class, JanksonConfigSerializer::new);

        logOptional(ShopConfig::load);
        ShopConfig.register();

        AhLogger.register();
        Adblocker.register();
        ItemShopTooltip.register();
        AdditionalDataTooltip.register();
        ShopLogger.register();
        ShopCommand.register();
        ShopKeybinds.register();
        ContainerValueProvider.register();
        RefetchService.register();
    }

    public static CastiaConfig getConfig() {
        return AutoConfig.getConfigHolder(CastiaConfig.class).getConfig();
    }

    public static void logOptional(CastiaUtilsException.CastiaExceptionRunnable functionThatMayThrow) {
        try {
            functionThatMayThrow.run();
        } catch (CastiaUtilsException e) {
            CastiaUtils.LOGGER.error(e.getMessage());
        }
    }
}
