package de.craftery.castiautils.chestshop;

import lombok.Data;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Data
public class Shop {
    private String name;
    private String command;

    @Setter
    private static List<Shop> shops = new ArrayList<>();

    public Shop() {
        shops.add(this);
    }

    public static @Nullable Shop getByName(String name) {
        return shops.stream().filter(shop -> shop.getName().equals(name)).findFirst().orElse(null);
    }

    public static List<Shop> getByCommand(String command) {
        List<Shop> offers = new ArrayList<>();
        for (Shop shop : shops) {
            if (shop.command.equals(command)) {
                offers.add(shop);
            }
        }
        return offers;
    }

    public static List<Shop> getAll() {
        return shops;
    }
}
