package uk.co.dotcode.customvillagertrades.trades;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class EnchantmentEntryDeserializer implements JsonDeserializer<EnchantmentEntry> {

    @Override
    public EnchantmentEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        JsonObject obj = json.getAsJsonObject();

        EnchantmentEntry entry = new EnchantmentEntry();


        List<String> keys = new ArrayList<>();

        if (obj.has("enchantmentKeys")) {
            JsonElement el = obj.get("enchantmentKeys");

            if (el.isJsonArray()) {
                for (JsonElement e : el.getAsJsonArray()) {
                    keys.add(e.getAsString());
                }
            } else if (el.isJsonPrimitive()) {
                keys.add(el.getAsString());
            }
        }


        if (keys.isEmpty() && obj.has("enchantmentKey")) {
            JsonElement el = obj.get("enchantmentKey");

            if (el.isJsonArray()) {
                for (JsonElement e : el.getAsJsonArray()) {
                    keys.add(e.getAsString());
                }
            } else if (el.isJsonPrimitive()) {
                keys.add(el.getAsString());
            }
        }

        entry.enchantmentKeys = keys;


        if (obj.has("minEnchantmentLevel")) {
            entry.minEnchantmentLevel = obj.get("minEnchantmentLevel").getAsInt();
        }

        if (obj.has("maxEnchantmentLevel")) {
            entry.maxEnchantmentLevel = obj.get("maxEnchantmentLevel").getAsInt();
        }

        // legacy: enchantmentLevel
        if (obj.has("enchantmentLevel")) {
            entry.enchantmentLevel = obj.get("enchantmentLevel").getAsInt();
        }

        return entry;
    }
}