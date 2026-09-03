package uk.co.dotcode.customvillagertrades.trades;

import net.minecraft.nbt.CompoundTag;

public class NBTData {

	public String nbtName;
	public String dataType;
	public String data;

	public NBTData() {
		if (nbtName == null) {
			nbtName = "";
		}

		if (dataType == null) {
			dataType = "string";
		}

		if (data == null) {
			data = "";
		}
	}

	/**
	 * Possible data types: String, Boolean, Integer, Float, Byte, Long
	 */
	public CompoundTag getTag() {
		CompoundTag tag = new CompoundTag();

		switch (dataType.toLowerCase()) {
			case "string":
				tag.putString(nbtName, data);
				break;
			case "boolean":
				tag.putBoolean(nbtName, Boolean.parseBoolean(data));
				break;
			case "integer":
				tag.putInt(nbtName, Integer.parseInt(data));
				break;
			case "float":
				tag.putFloat(nbtName, Float.parseFloat(data));
				break;
			case "byte":
				tag.putByte(nbtName, Byte.parseByte(data));
				break;
			case "long":
				tag.putLong(nbtName, Long.parseLong(data));
				break;
			default:
				tag.putString(nbtName, data);
				break;
		}

		return tag;
	}

}
