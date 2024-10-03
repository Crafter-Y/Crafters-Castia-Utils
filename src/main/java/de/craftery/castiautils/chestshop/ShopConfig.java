package de.craftery.castiautils.chestshop;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.craftery.castiautils.CastiaUtils;
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
        Gson gson = new Gson();
        try {
            String json = FileUtils.readFileToString(getConfigFile("offers.json5"), "UTF-8");
            List<Offer> offers = new ArrayList<>(Arrays.stream(gson.fromJson(json, Offer[].class)).toList());
            Offer.setOffers(offers);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JsonSyntaxException e) {
            CastiaUtils.LOGGER.info("No offers stored in file");
        }

        try {
            String json = FileUtils.readFileToString(getConfigFile("shops.json5"), "UTF-8");
            List<Shop> shops = new ArrayList<>(Arrays.stream(gson.fromJson(json, Shop[].class)).toList());
            Shop.setShops(shops);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JsonSyntaxException e) {
            CastiaUtils.LOGGER.info(e);
            CastiaUtils.LOGGER.info("No shops stored in file");
        }
    }

    public static void register() {
        ClientLifecycleEvents.CLIENT_STOPPING.register((client) -> {
            writeState();
        });

        AtomicInteger currentTick = new AtomicInteger(1);
        ClientTickEvents.END_CLIENT_TICK.register((client) -> {
            int tick = currentTick.getAndIncrement();

            if (tick % 2400 == 0) {
                writeState();
            }

        });
    }

    private static void writeState() {
        CastiaUtils.LOGGER.info("Saving state");
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
