package edu.commonwealthu.lastserverstanding.ui;

import androidx.annotation.DrawableRes;

/**
 * Data class representing a tower type available for purchase
 */
public class TowerOption {
    private final String name;
    private final String type;
    private final int cost;
    private final float damage;
    private final float range;
    private final float fireRate;
    @DrawableRes
    private final int iconResId;
    private final boolean isLocked;
    private final String description;

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

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getCost() {
        return cost;
    }

    public float getDamage() {
        return damage;
    }

    public float getRange() {
        return range;
    }

    public float getFireRate() {
        return fireRate;
    }

    public int getIconResId() {
        return iconResId;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public String getDescription() {
        return description;
    }
}
