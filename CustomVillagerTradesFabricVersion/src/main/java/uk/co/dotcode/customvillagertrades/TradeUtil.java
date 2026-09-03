package uk.co.dotcode.customvillagertrades;



import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import uk.co.dotcode.customvillagertrades.events.TradeRegistry;
import uk.co.dotcode.customvillagertrades.trades.EnchantmentEntry;
import uk.co.dotcode.customvillagertrades.trades.MyTrade;

import java.util.*;

public class TradeUtil {

	public static final Random random = new Random();



	public static ResourceLocation getRL(String key) {
		return ResourceLocation.tryParse(key);
	}

	public static Item getItemFromKey(String key) {
		ResourceLocation id = getRL(key);
		return id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
	}

	public static ItemStack getItemStackFromKey(String key, int count) {
		Item item = getItemFromKey(key);
		if (item == Items.AIR) return ItemStack.EMPTY;
		return new ItemStack(item, count);
	}

	public static Potion getPotionFromKey(String key) {
		ResourceLocation id = getRL(key);
		return id == null ? null : BuiltInRegistries.POTION.get(id);
	}

	public static MobEffect getEffectFromKey(String key) {
		ResourceLocation id = getRL(key);
		return id == null ? null : BuiltInRegistries.MOB_EFFECT.get(id);
	}

	public static VillagerProfession getProfessionFromKey(String key) {
		ResourceLocation id = getRL(key);
		return id == null ? null : BuiltInRegistries.VILLAGER_PROFESSION.get(id);
	}

	public static String getKeyFromProfession(VillagerProfession prof) {
		ResourceLocation id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(prof);
		return id == null ? "" : id.toString();
	}



	public static int getLevel(EnchantmentEntry entry, Enchantment ench) {

		int min = entry.getMinLevel();
		int max = entry.getMaxLevel(ench.getMaxLevel());

		if (max < min) max = min;

		if (max > min) {
			return min + random.nextInt(max - min + 1);
		}

		return min;
	}



	public static void addOffersFromItemListings(
			AbstractVillager villager,
			VillagerTrades.ItemListing[] itemListings,
			int count
	) {
		MerchantOffers offers = villager.getOffers();
		Set<Integer> set = new HashSet<>();

		if (itemListings.length > count) {
			while (set.size() < count) {
				set.add(villager.getRandom().nextInt(itemListings.length));
			}
		} else {
			for (int i = 0; i < itemListings.length; i++) {
				set.add(i);
			}
		}

		for (int index : set) {
			VillagerTrades.ItemListing listing = itemListings[index];
			MerchantOffer offer = listing.getOffer(villager, villager.getRandom());

			if (offer != null) {
				offers.add(offer);
			}
		}
	}



	public static MyTrade generateUTID(String profession, MyTrade trade) {

		String base = profession + "_" + assembleUTIDString(trade);

		int attempts = 0;
		String utid = base;

		while (TradeRegistry.usedUTIDs.containsKey(utid) && attempts < 5) {
			utid = base + "_" + attempts;
			attempts++;
		}

		trade.assignUTID(utid);
		return trade;
	}

	private static String assembleUTIDString(MyTrade trade) {

		String offer = trade.offer != null ? trade.offer.itemKey : "null_offer";
		String request = trade.request != null ? trade.request.itemKey : "null_request";

		return offer + "_for_" + request;
	}


	public static List<VillagerProfession> getAllProfessions() {
		return BuiltInRegistries.VILLAGER_PROFESSION.stream().toList();
	}

	public static int getIntFromColor(int r, int g, int b) {
		return (r << 16) | (g << 8) | b;
	}

	public static String itemKey(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	public static final RandomSource randomSource = RandomSource.create();

}