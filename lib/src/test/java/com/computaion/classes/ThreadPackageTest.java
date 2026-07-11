package com.computaion.classes;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.RestAssured.RestPackage.RestAssuredClass;

public class ThreadPackageTest {

    @Test
    public void returnsTheSameSingletonInstance() {
        assertSame(ThreadPackage.getInstance(), ThreadPackage.getInstance());
    }

    @Test
    public void storesEachServiceForTheCurrentThread() {
        ThreadPackage threads = ThreadPackage.getInstance();
        RestAssuredClass restAssured = new RestAssuredClass();
        ShareHoldingChange shareHolding = new ShareHoldingChange();
        ShareHoldingPercentageChange percentageChange = new ShareHoldingPercentageChange();
        PromoterBuying promoterBuying = new PromoterBuying();

        threads.setThreadLocal(restAssured);
        threads.setThreadLocalShareHolding(shareHolding);
        threads.setThreadLocalSharePChange(percentageChange);
        threads.setThreadLocalPromoterBuying(promoterBuying);

        assertSame(restAssured, threads.getThreadLocal());
        assertSame(shareHolding, threads.getThreadLocalShareHolding());
        assertSame(percentageChange, threads.getThreadLocalSharePchange());
        assertSame(promoterBuying, threads.getThreadLocalPromoterBuying());
    }

    @Test
    public void valuesAreIsolatedFromOtherThreads() throws Exception {
        ThreadPackage threads = ThreadPackage.getInstance();
        threads.setThreadLocal(new RestAssuredClass());
        AtomicReference<RestAssuredClass> childValue = new AtomicReference<>();

        Thread child = new Thread(() -> childValue.set(threads.getThreadLocal()));
        child.start();
        child.join();

        assertNull(childValue.get());
    }
}
