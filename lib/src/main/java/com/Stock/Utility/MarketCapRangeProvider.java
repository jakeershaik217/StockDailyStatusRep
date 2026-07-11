package com.Stock.Utility;

import java.util.HashMap;

import org.testng.annotations.DataProvider;

/**
 * Shared TestNG {@link DataProvider} that yields the market-capitalization
 * ranges used to parallelize the stock publishers. Every publisher class used
 * to declare an identical copy of this provider; they now reference this one
 * via {@code dataProviderClass = MarketCapRangeProvider.class}.
 */
public class MarketCapRangeProvider {

	private static final int[][] RANGES = {
			{ 0, 50 },
			{ 50, 100 },
			{ 100, 1000 },
			{ 1000, 10000 },
			{ 10000, 100000 },
			{ 100000, 1000000 },
			{ 1000000, 0 }
	};

	@DataProvider(name = "paralleltest", parallel = true)
	public static Object[][] getMarketCapRange() {

		Object[][] HashMapData = new Object[RANGES.length][1];
		for (int i = 0; i < RANGES.length; i++) {
			HashMap<String, Integer> Maps = new HashMap<String, Integer>();
			Maps.put("Range1", RANGES[i][0]);
			Maps.put("Range2", RANGES[i][1]);
			HashMapData[i][0] = Maps;
		}
		return HashMapData;
	}
}
