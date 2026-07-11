package com.RestAssured.RestPackage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;

import org.junit.Test;

public class JSONObjectCallsTest {

    @Test
    public void constructorAndSettersExposeNewsFields() {
        JSONObjectCalls news = new JSONObjectCalls("1", "Initial subject", "2023-07-17");

        assertEquals("1", news.getNewsid());
        assertEquals("Initial subject", news.getNewsSubj());
        assertEquals("2023-07-17", news.getNewsdt());

        news.setNewsid("2");
        news.setNewsSubj("Updated subject");
        news.setNewsdt("2023-07-18");

        assertEquals("2", news.getNewsid());
        assertEquals("Updated subject", news.getNewsSubj());
        assertEquals("2023-07-18", news.getNewsdt());
    }

    @Test
    public void rootStoresNewsCollection() {
        Root root = new Root();
        ArrayList<JSONObjectCalls> news = new ArrayList<>();

        root.jsonObjectCalls = news;

        assertSame(news, root.jsonObjectCalls);
    }
}
