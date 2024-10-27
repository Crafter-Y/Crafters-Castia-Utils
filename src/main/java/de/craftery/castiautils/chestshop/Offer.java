package de.craftery.castiautils.chestshop;

import com.google.gson.JsonObject;
import de.craftery.castiautils.CastiaUtils;
import lombok.Data;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class Offer implements Serializable {
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

    public static void mergeIncoming(List<Offer> newOffers) {
        for (Offer offer : newOffers) {
            if (offers.stream().noneMatch(s -> s.getId().equals(offer.getId()))) {
                if (Shop.getByName(offer.getShop()) != null) {
                    offers.add(offer);
                }
            }
        }
    }

    public static List<Offer> getByItem(String item) {
        List<Offer> returnOffers = new ArrayList<>();
        for (Offer offer : offers) {
            if (offer.item == null) {
                CastiaUtils.LOGGER.error(offer.getId() + " " + offer.getOwner() + " is null!");
                continue;
            }
            if (offer.item.equals(item)) {
                returnOffers.add(offer);
            }
        }
        return returnOffers;
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

    public JsonObject getUniqueIdentifier() {
        JsonObject jsonObject = new JsonObject();
        JsonObject coordinates = new JsonObject();
        coordinates.addProperty("x", x);
        coordinates.addProperty("y", y);
        coordinates.addProperty("z", z);
        jsonObject.add("x_y_z", coordinates);
        return jsonObject;
    }
}
