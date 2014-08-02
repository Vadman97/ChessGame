package com.nwgjb.commons.util;

public class BitField
{
	public static boolean getBit(int fields, int bitOffset)
	{
		return (fields & (1 << bitOffset)) != 0;
	}

	public static int setBit(int fields, int bitOffset)
	{
		return fields | (1 << bitOffset);
	}

	public static int clearBit(int fields, int bitOffset)
	{
		return fields & ~(1 << bitOffset);
	}

	public static int changeBit(int fields, int bitOffset, boolean val)
	{
		if (val)
		{
			return setBit(fields, bitOffset);
		} else
		{
			return clearBit(fields, bitOffset);
		}

	}
}
