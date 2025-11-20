package edu.commonwealthu.lastserverstanding.ui;

import androidx.annotation.DrawableRes;

/**
 * Data class representing a tower type available for purchase.
 *
 * @author Ethan Wight
 */
public record TowerOption(String name, String type, int cost, float damage, float range,
                          float fireRate, @DrawableRes int iconResId, boolean isLocked,
                          String description) {
    public TowerOption(String name, String type, int cost, float damage, float range,
                       float fireRate, int iconResId, boolean isLocked, String description) {
        this.name = name;
        this.type = type;
        this.cost = cost;
        this.damage = damage;
        this.range = range;
        this.fireRate = fireRate;
        this.iconResId = iconResId;
        this.isLocked = isLocked;
        this.description = description;
    }

    @Override
    public int iconResId() {
        return iconResId;
    }
}
