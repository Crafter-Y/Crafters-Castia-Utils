package de.craftery.castiautils.chestshop;

import lombok.Data;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Data
public class Offer {
    private String shop;
    private int x;
    private int y;
    private int z;
    private String owner;
    private String item;
    private String display;
    private Float buyPrice;
    private Float sellPrice;
    private boolean empty = false;
    private boolean full = false;

    @Setter
    private static List<Offer> offers = new ArrayList<>();

    public Offer() {
        offers.add(this);
    }

    public static List<Offer> getByItem(String item) {
        List<Offer> offers = new ArrayList<>();
        for (Offer offer : Offer.offers) {
            if (offer.item.equals(item)) {
                offers.add(offer);
            }
        }
        return offers;
    }

    public static @Nullable Offer getByCoordinate(int x, int y, int z) {
        for (Offer offer : offers) {
            if (offer.x == x && offer.y == y && offer.z == z) {
                return offer;
            }
        }
        return null;
    }

    public void delete() {
        offers.remove(this);
    }

    public static @Nullable Offer getById(String id) {
        for (Offer offer : offers) {
            if (offer.getId().equals(id)) {
                return offer;
            }
        }
        return null;
    }

    public String getId() {
        return x + "-" + y + "-" + z;
    }

    public static List<Offer> getAll() {
        return offers;
    }
}
