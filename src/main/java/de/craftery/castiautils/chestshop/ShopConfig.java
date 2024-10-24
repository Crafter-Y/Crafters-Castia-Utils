package de.craftery.castiautils.chestshop;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import de.craftery.castiautils.CastiaUtils;
import de.craftery.castiautils.CastiaUtilsException;
import de.craftery.castiautils.api.RequestService;
import de.craftery.castiautils.config.CastiaConfig;
import de.craftery.castiautils.config.DataSource;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Jankson;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ShopConfig {
    public static void load() throws CastiaUtilsException {
        CastiaConfig config = CastiaUtils.getConfig();
        Gson gson = new Gson();

        if (config.dataSource == DataSource.LOCAL_ONLY || config.dataSource == DataSource.MERGE) {
            CastiaUtils.LOGGER.info("Loading data from local config");
            try {
                String json = FileUtils.readFileToString(getConfigFile("offers.json5"), "UTF-8");
                Offer[] offersArr = gson.fromJson(json, Offer[].class);
                if (offersArr != null) {
                    List<Offer> offers = new ArrayList<>(Arrays.stream(offersArr).toList());
                    Offer.setOffers(offers);
                }
            } catch (JsonSyntaxException | IOException e) {
                throw new CastiaUtilsException("Could not load offers.json5: " + e.getMessage());
            }

            try {
                String json = FileUtils.readFileToString(getConfigFile("shops.json5"), "UTF-8");
                Shop[] shopArr = gson.fromJson(json, Shop[].class);
                if (shopArr != null) {
                    List<Shop> shops = new ArrayList<>(Arrays.stream(shopArr).toList());
                    Shop.setShops(shops);
                }
            } catch (JsonSyntaxException | IOException e) {
                throw new CastiaUtilsException("Could not load shops.json5: " + e.getMessage());
            }
        }
        if (config.dataSource == DataSource.LOCAL_ONLY) return;

        try {
            JsonElement shopData = RequestService.get("shop");
            List<Shop> shops = new ArrayList<>(Arrays.stream(gson.fromJson(shopData, Shop[].class)).toList());
            if (config.dataSource == DataSource.SERVER_ONLY) {
                CastiaUtils.LOGGER.info("Loading data from API");
                Shop.setShops(shops);
            } else {
                CastiaUtils.LOGGER.info("Merging data from API");
                Shop.mergeIncoming(shops);
            }
        } catch (CastiaUtilsException e) {
            throw new CastiaUtilsException("Failed to load shops from API: " + e.getMessage());
        }

        try {
            JsonElement offerData = RequestService.get("offer");
            List<Offer> offers = new ArrayList<>(Arrays.stream(gson.fromJson(offerData, Offer[].class)).toList());
            if (config.dataSource == DataSource.SERVER_ONLY) {
                CastiaUtils.LOGGER.info("Loading data from API");
                Offer.setOffers(offers);
            } else {
                CastiaUtils.LOGGER.info("Merging data from API");
                Offer.mergeIncoming(offers);
            }
        } catch (CastiaUtilsException e) {
            throw new CastiaUtilsException("Failed to load offers from API: " + e.getMessage());
        }
    }

    public static void register() {
        ClientLifecycleEvents.CLIENT_STOPPING.register((client) -> writeState());

        AtomicInteger currentTick = new AtomicInteger(1);
        ClientTickEvents.END_CLIENT_TICK.register((client) -> {
            int tick = currentTick.getAndIncrement();

            if (tick % 2400 == 0) {
                writeState();
            }

        });
    }

    public static void writeState() {
        CastiaConfig config = CastiaUtils.getConfig();

        if (config.dataSource == DataSource.LOCAL_ONLY || config.dataSource == DataSource.MERGE) {
            CastiaUtils.LOGGER.info("Saving data to local storage");

            Jankson jankson = Jankson.builder().build();
            me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.JsonElement offers = jankson.toJson(Offer.getAll().toArray());
            me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.JsonElement shops = jankson.toJson(Shop.getAll().toArray());

            BufferedWriter offersWriter;
            BufferedWriter shopsWriter;
            try {
                offersWriter = new BufferedWriter(new FileWriter(getConfigFile("offers.json5")));
                offersWriter.append(offers.toJson());
                offersWriter.close();

                shopsWriter = new BufferedWriter(new FileWriter(getConfigFile("shops.json5")));
                shopsWriter.append(shops.toJson());
                shopsWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static File getConfigFile(String filename) {
        filename = "./config/castiautils/" + filename;
        try {
            Files.createDirectories(Paths.get("./config/castiautils/"));
            File myObj = new File(filename);
            boolean ignored = myObj.createNewFile();
            return myObj;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
