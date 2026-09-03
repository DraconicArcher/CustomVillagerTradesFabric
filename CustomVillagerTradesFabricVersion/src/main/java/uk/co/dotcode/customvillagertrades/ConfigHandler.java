package uk.co.dotcode.customvillagertrades;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.npc.VillagerTrades;

import uk.co.dotcode.customvillagertrades.trades.*;


public class ConfigHandler {

	public static File folder;
	public static File folderWanderer;
	public static File folderExports;
	public static File globalConfig;

	public static RegistryAccess ACCESS;


	public static void setup(File configDirectory) {

		folder = new File(
				configDirectory,
				"customvillagertrades"
		);

		folderWanderer = new File(
				folder,
				"wanderer"
		);

		folderExports = new File(
				folder,
				"exports"
		);

		globalConfig = new File(
				folder,
				"config.json"
		);
	}



	public static HashMap<String, TradeCollection> customTrades =
			new HashMap<String, TradeCollection>();

	public static HashMap<String, WandererTradeCollection> customWandererTrades =
			new HashMap<String, WandererTradeCollection>();

	public static Map<String, Map<Integer, List<VillagerTrades.ItemListing>>>
			registeredCustomTrades =
			new HashMap<>();

	public static Map<Integer, List<VillagerTrades.ItemListing>>
			registeredCustomWandererTrades =
			null;

	public static Map<Integer, List<VillagerTrades.ItemListing>>
			registeredAllCategoryTrades =
			null;


	public static void init() {

		boolean newConfigs = false;

		Gson gson = new GsonBuilder()
				.registerTypeAdapter(
						EnchantmentEntry.class,
						new EnchantmentEntryDeserializer()
				)
				.setPrettyPrinting()
				.create();

		if (!folder.exists()) {
			folder.mkdirs();
			newConfigs = true;
		}

		if (!folderWanderer.exists()) {
			folderWanderer.mkdirs();
		}

		if (!folderExports.exists()) {
			folderExports.mkdirs();
		}

		if (!globalConfig.exists()) {
			try (FileWriter writer = new FileWriter(globalConfig)) {
				gson.toJson(new GlobalConfig(), writer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (isJsonFile(globalConfig)) {
			loadGlobalConfigFile(gson, globalConfig);
		}

		File[] fileArray = folder.listFiles();
		File[] fileArrayWanderer = folderWanderer.listFiles();

		if (newConfigs) {
			ModLogger.info(
					"No custom trades were found. Generating example trades..."
			);

			generateExampleTrade();
		}

		if (fileArray != null) {
			for (File f : fileArray) {
				if (!f.isDirectory() && isJsonFile(f)) {
					loadFile(gson, f, false);
				}
			}
		}

		if (fileArrayWanderer != null) {
			for (File f : fileArrayWanderer) {
				if (!f.isDirectory() && isJsonFile(f)) {
					loadFile(gson, f, true);
				}
			}
		}
	}


	public static void overwriteTradeCollection(TradeCollection collection) {

		Gson gson = new GsonBuilder()
				.setPrettyPrinting()
				.create();

		File file = new File(
				folder,
				collection.profession + ".json"
		);

		try (FileWriter writer = new FileWriter(file)) {
			gson.toJson(collection, writer);
		} catch (IOException e) {
			ModLogger.error(
					"Failed to overwrite trade collection: "
							+ collection.profession
			);

			e.printStackTrace();
		}
	}


	private static void loadGlobalConfigFile(Gson gson, File file) {

		try (Reader reader = new FileReader(file)) {

			ModLogger.info("Loading CVT global config");

			GlobalConfig loaded =
					gson.fromJson(reader, GlobalConfig.class);

		} catch (Exception e) {

			ModLogger.error(
					"Failed to load global config, using defaults"
			);

			e.printStackTrace();
		}
	}


	private static void loadFile(
			Gson gson,
			File f,
			boolean isWanderer
	) {

		String currentProfession = "INVALID";

		ModLogger.info(
				"Loading custom villager trades from file: "
						+ f.getName()
		);

		try (Reader reader = new FileReader(f)) {

			if (isWanderer) {

				WandererTradeCollection coll =
						gson.fromJson(
								reader,
								WandererTradeCollection.class
						);

				if (coll == null || coll.profession == null) {

					ModLogger.error(
							"Invalid wanderer trade file: "
									+ f.getName()
					);

					return;
				}

				currentProfession =
						coll.profession.toLowerCase();

				customWandererTrades.put(
						currentProfession,
						coll
				);

			} else {

				TradeCollection coll =
						gson.fromJson(
								reader,
								TradeCollection.class
						);

				if (coll == null || coll.profession == null) {

					ModLogger.error(
							"Invalid trade file: " + f.getName()
					);

					return;
				}

				currentProfession =
						coll.profession.toLowerCase();

				customTrades.put(
						currentProfession,
						coll
				);

				if (ACCESS != null && !coll.validate(ACCESS)) {

					ModLogger.error(
							"Invalid trade file! Source: "
									+ coll.source
									+ ", Profession: "
									+ coll.profession
					);
				}
			}

		}catch (IOException e) {

			ModLogger.error(
					"A problem has been found with the config file for '"
							+ currentProfession
							+ "'! File cannot be accessed."
			);

			e.printStackTrace();

		} catch (JsonSyntaxException e) {

			ModLogger.error(
					"A problem has been found with the config file for '"
							+ currentProfession
							+ "'! JSON formatting issue."
							+ " Cause: " + e.getCause()
			);
		}


		if (isWanderer) {

			for (WandererTradeCollection coll :
					customWandererTrades.values()) {

				if (!coll.validate()) {

					ModLogger.error(
							"Invalid wanderer trade file! Source: "
									+ coll.source
									+ ", Profession: "
									+ coll.profession
					);
				}
			}

		} else {

			for (TradeCollection coll :
					customTrades.values()) {

				if (!coll.validate(ACCESS)) {

					ModLogger.error(
							"Invalid trade file! Source: "
									+ coll.source
									+ ", Profession: "
									+ coll.profession
					);
				}
			}
		}
	}


	public static void finalizeTrades() {

		if (ACCESS == null) {

			ModLogger.error(
					"RegistryAccess not ready yet — "
							+ "skipping finalizeTrades()"
			);

			return;
		}

		registeredCustomTrades.clear();

		for (TradeCollection coll : customTrades.values()) {

			if (!coll.validate(ACCESS)) {

				ModLogger.error(
						"Invalid trade file: "
								+ coll.profession
				);

				continue;
			}

			Map<Integer, List<VillagerTrades.ItemListing>>
					converted = new HashMap<>();

			for (MyTrade trade : coll.trades) {

				if (trade == null || trade.tradeLevel == null) {
					continue;
				}

				converted
						.computeIfAbsent(
								trade.tradeLevel,
								k -> new ArrayList<>()
						)
						.add(new MyTradeConverted(trade));
			}

			registeredCustomTrades.put(
					coll.profession.toLowerCase(),
					converted
			);
		}

		ModLogger.info(
				"Finished converting custom trades"
		);

		finalizeWandererTrades();
	}



	public static void finalizeWandererTrades() {

		registeredCustomWandererTrades =
				new HashMap<>();

		List<VillagerTrades.ItemListing>
				common =
				new ArrayList<>();

		List<VillagerTrades.ItemListing> rare =
				new ArrayList<>();

		for (WandererTradeCollection coll :
				customWandererTrades.values()) {

			if (!coll.validate()) {

				ModLogger.error(
						"Invalid wanderer trade file: "
								+ coll.profession
				);

				continue;
			}

			for (MyWandererTrade trade :
					coll.trades) {

				VillagerTrades.ItemListing listing =
						new MyTradeConverted(trade);

				if (trade.isRare) {
					rare.add(listing);
				} else {
					common.add(listing);
				}
			}
		}

		registeredCustomWandererTrades.put(
				1,
				common
		);

		registeredCustomWandererTrades.put(
				2,
				rare
		);

		ModLogger.info(
				"Finished converting wanderer trades. "
						+ "Common=" + common.size()
						+ ", Rare=" + rare.size()
		);
	}





	public static void exportTradeCollection(
			TradeCollection collection
	) {

		Gson gson = new GsonBuilder()
				.setPrettyPrinting()
				.create();

		String filename =
				collection.profession;

		if (filename.contains(":")) {

			filename =
					filename.substring(
							filename.indexOf(':') + 1
					);
		}

		File file =
				new File(
						folderExports,
						filename + ".json"
				);

		try (FileWriter writer =
					 new FileWriter(file)) {

			gson.toJson(
					collection,
					writer
			);

		} catch (IOException e) {

			ModLogger.error(
					"Failed to export trade collection: "
							+ collection.profession
			);

			e.printStackTrace();
		}
	}


	public static void exportWandererTradeCollection(
			WandererTradeCollection collection
	) {

		Gson gson = new GsonBuilder()
				.setPrettyPrinting()
				.create();

		File file =
				new File(
						folderExports,
						"wanderer.json"
				);

		try (FileWriter writer =
					 new FileWriter(file)) {

			gson.toJson(
					collection,
					writer
			);

		} catch (IOException e) {

			ModLogger.error(
					"Failed to export wanderer trade collection"
			);

			e.printStackTrace();
		}
	}


	public static TradeCollection loadTrades(
			String profession
	) {

		return customTrades.get(profession);
	}


	public static WandererTradeCollection loadWandererTrades(
			String profession
	) {

		return customWandererTrades.get(profession);
	}


	private static boolean isJsonFile(File f) {

		return f.getName()
				.toLowerCase()
				.endsWith(".json");
	}


	private static void generateExampleTrade() {

		TradeCollection exampleTrades =
				new TradeCollection();

		exampleTrades.profession =
				"minecraft:armorer";


		TradeItem offer1 =
				new TradeItem();

		offer1.setItemKey(
				"minecraft:dirt"
		);

		offer1.setAmount(3);


		EnchantmentEntry e1 =
				new EnchantmentEntry();

		e1.enchantmentKeys =
				List.of("minecraft:knockback");

		e1.minEnchantmentLevel = 2;
		e1.maxEnchantmentLevel = 2;


		EnchantmentEntry e2 =
				new EnchantmentEntry();

		e2.enchantmentKeys =
				List.of("minecraft:sharpness");

		e2.minEnchantmentLevel = 1;
		e2.maxEnchantmentLevel = 5;


		offer1.setEnchantments(
				List.of(e1, e2)
		);


		TradeItem request1 =
				new TradeItem();

		request1.setItemKey(
				"minecraft:cobblestone"
		);

		request1.setAmount(1);


		MyTrade trade1 =
				new MyTrade();

		trade1.offer = offer1;
		trade1.request = request1;
		trade1.maxUses = 3;
		trade1.tradeExp = 10;
		trade1.priceMultiplier = 0.1f;
		trade1.tradeLevel = 1;


		TradeItem offer2 =
				new TradeItem();

		offer2.setItemKey(
				"minecraft:emerald"
		);

		offer2.setAmount(2);


		TradeItem request2 =
				new TradeItem();

		request2.setItemKey(
				"minecraft:iron_ingot"
		);

		request2.setAmount(2);


		MyTrade trade2 =
				new MyTrade();

		trade2.offer = offer2;
		trade2.request = request2;
		trade2.maxUses = 6;
		trade2.tradeExp = 10;
		trade2.priceMultiplier = 0.1f;
		trade2.tradeLevel = 2;


		TradeItem ironAxe =
				new TradeItem();

		ironAxe.setItemKey(
				"minecraft:iron_axe"
		);

		ironAxe.setAmount(1);
		ironAxe.setPriceModifier(-3);


		TradeItem goldAxe =
				new TradeItem();

		goldAxe.setItemKey(
				"minecraft:golden_axe"
		);

		goldAxe.setAmount(1);


		TradeItem diamondAxe =
				new TradeItem();

		diamondAxe.setItemKey(
				"minecraft:diamond_axe"
		);

		diamondAxe.setAmount(1);
		diamondAxe.setPriceModifier(4);


		TradeItem request3 =
				new TradeItem();

		request3.setItemKey(
				"minecraft:iron_ingot"
		);

		request3.setAmount(6);


		MyTrade trade3 =
				new MyTrade();

		trade3.multiOffer =
				List.of(
						ironAxe,
						goldAxe,
						diamondAxe
				);

		trade3.request = request3;
		trade3.maxUses = 15;
		trade3.tradeExp = 5;
		trade3.priceMultiplier = 0.1f;
		trade3.tradeLevel = 3;


		TradeItem sword =
				new TradeItem();

		sword.setItemKey(
				"minecraft:wooden_sword"
		);

		sword.setAmount(1);

		sword.setEnchantments(
				List.of(
						new EnchantmentEntry(
								List.of("minecraft:mending"),
								1,
								1
						),
						new EnchantmentEntry(
								List.of("minecraft:looting"),
								1,
								3
						),
						new EnchantmentEntry(
								List.of("minecraft:sweeping_edge"),
								1,
								2
						)
				)
		);


		TradeItem request4 =
				new TradeItem();

		request4.setItemKey(
				"minecraft:quartz"
		);

		request4.setAmount(5);


		MyTrade trade4 =
				new MyTrade();

		trade4.offer = sword;
		trade4.request = request4;
		trade4.maxUses = 20;
		trade4.tradeExp = 5;
		trade4.priceMultiplier = 0.0f;
		trade4.tradeLevel = 4;


		TradeItem book =
				new TradeItem();

		book.setItemKey(
				"minecraft:enchanted_book"
		);

		book.setAmount(1);

		book.setEnchantments(
				List.of(
						new EnchantmentEntry(
								List.of("random"),
								2,
								3
						)
				)
		);

		book.setBlacklist(
				List.of(
						"minecraft:protection",
						"minecraft:fire_protection"
				)
		);


		TradeItem request5 =
				new TradeItem();

		request5.setItemKey(
				"minecraft:emerald"
		);

		request5.setAmount(3);


		MyTrade trade5 =
				new MyTrade();

		trade5.offer = book;
		trade5.request = request5;
		trade5.maxUses = 3;
		trade5.tradeExp = 10;
		trade5.priceMultiplier = 0.1f;
		trade5.tradeLevel = 1;


		exampleTrades.trades =
				List.of(
						trade1,
						trade2,
						trade3,
						trade4,
						trade5
				);


		Gson gson =
				new GsonBuilder()
						.setPrettyPrinting()
						.create();

		File exportPath =
				new File(
						folder,
						"armorer.json"
				);

		try (FileWriter writer =
					 new FileWriter(exportPath)) {

			gson.toJson(
					exampleTrades,
					writer
			);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
