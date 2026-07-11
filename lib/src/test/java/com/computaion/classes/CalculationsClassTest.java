package com.computaion.classes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

public class CalculationsClassTest {

    @Test
    public void filtersCompaniesAtOrAboveMinimumWhenMaximumIsZero() {
        HashMap<String, Object> belowRange = company(99.99);
        HashMap<String, Object> lowerBoundary = company(100.0);
        HashMap<String, Object> aboveRange = company(250.0);
        List<HashMap<String, Object>> companies = companies(belowRange, lowerBoundary, aboveRange);

        List<HashMap<String, Object>> result =
                new CalculationsClass().getCompanyDatawithMarketCapMarketCapRange(100, 0, companies);

        assertEquals(2, result.size());
        assertSame(lowerBoundary, result.get(0));
        assertSame(aboveRange, result.get(1));
    }

    @Test
    public void filtersCompaniesWithinHalfOpenRange() {
        HashMap<String, Object> belowRange = company(99.99);
        HashMap<String, Object> lowerBoundary = company(100.0);
        HashMap<String, Object> insideRange = company(199.99);
        HashMap<String, Object> upperBoundary = company(200.0);
        List<HashMap<String, Object>> companies =
                companies(belowRange, lowerBoundary, insideRange, upperBoundary);

        List<HashMap<String, Object>> result =
                new CalculationsClass().getCompanyDatawithMarketCapMarketCapRange(100, 200, companies);

        assertEquals(2, result.size());
        assertSame(lowerBoundary, result.get(0));
        assertSame(insideRange, result.get(1));
    }

    private HashMap<String, Object> company(double marketCap) {
        HashMap<String, Object> company = new HashMap<>();
        company.put("CompanyMarketCap", marketCap);
        return company;
    }

    @SafeVarargs
    private final List<HashMap<String, Object>> companies(HashMap<String, Object>... companies) {
        List<HashMap<String, Object>> result = new ArrayList<>();
        for (HashMap<String, Object> company : companies) {
            result.add(company);
        }
        return result;
    }
}
