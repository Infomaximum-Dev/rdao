package com.infomaximum.database.core.transaction.modifier;

/**
 * Created by kris on 18.05.17.
 */
public class ModifierSet extends Modifier {

    private final byte[] value;

    public ModifierSet(String columnFamily, final byte[] key, final byte[] value) {
        super(columnFamily, key);
        this.value = value;
    }

    public byte[] getValue() {
        return value;
    }
}