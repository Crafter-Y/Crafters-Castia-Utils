package de.craftery.castiautils.chestshop;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.craftery.castiautils.CastiaUtils;
import de.craftery.castiautils.api.RequestService;
import de.craftery.castiautils.config.CastiaConfig;
import de.craftery.castiautils.config.DataSource;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Jankson;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.JsonElement;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ShopConfig {
    public static void load() {
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
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (JsonSyntaxException e) {
                CastiaUtils.LOGGER.info("No offers stored in file");
            }

            try {
                String json = FileUtils.readFileToString(getConfigFile("shops.json5"), "UTF-8");
                Shop[] shopArr = gson.fromJson(json, Shop[].class);
                if (shopArr != null) {
                    List<Shop> shops = new ArrayList<>(Arrays.stream(shopArr).toList());
                    Shop.setShops(shops);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (JsonSyntaxException e) {
                CastiaUtils.LOGGER.info(e);
                CastiaUtils.LOGGER.info("No shops stored in file");
            }
        }
        if (config.dataSource == DataSource.LOCAL_ONLY) return;

        RequestService.ApiResponseWithData shopData = RequestService.get("shop");
        if (!shopData.isSuccess()) {
            // TODO: do something
            CastiaUtils.LOGGER.error("Failed to load shops from API: " + shopData.getData().toString());
            return;
        }

        List<Shop> shops = new ArrayList<>(Arrays.stream(gson.fromJson(shopData.getData(), Shop[].class)).toList());
        if (config.dataSource == DataSource.SERVER_ONLY) {
            CastiaUtils.LOGGER.info("Loading data from API");
            Shop.setShops(shops);
        } else {
            CastiaUtils.LOGGER.info("Merging data from API");
            Shop.mergeIncoming(shops);
        }

        RequestService.ApiResponseWithData offerData = RequestService.get("offer");
        if (!offerData.isSuccess()) {
            // TODO: do something
            CastiaUtils.LOGGER.error("Failed to load offers from API: " + offerData.getData().toString());
            return;
        }

        List<Offer> offers = new ArrayList<>(Arrays.stream(gson.fromJson(offerData.getData(), Offer[].class)).toList());
        if (config.dataSource == DataSource.SERVER_ONLY) {
            CastiaUtils.LOGGER.info("Loading data from API");
            Offer.setOffers(offers);
        } else {
            CastiaUtils.LOGGER.info("Merging data from API");
            Offer.mergeIncoming(offers);
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
            JsonElement offers = jankson.toJson(Offer.getAll().toArray());
            JsonElement shops = jankson.toJson(Shop.getAll().toArray());

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
        filename = "./config/" + filename;
        try {
            File myObj = new File(filename);
            boolean ignored = myObj.createNewFile();
            return myObj;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
