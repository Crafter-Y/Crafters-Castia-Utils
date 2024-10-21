package de.craftery.castiautils.ah;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AhOffer {
    private String item;
    private float price;
    private int amount;
}
