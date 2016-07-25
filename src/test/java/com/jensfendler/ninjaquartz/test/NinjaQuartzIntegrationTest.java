/*
 * Copyright 2016 Fendler Consulting cc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jensfendler.ninjaquartz.test;

import ninja.app.controllers.Application;
import ninja.standalone.Standalone;
import ninja.standalone.StandaloneHelper;
import ninja.utils.NinjaMode;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Basic integration tests for Ninja Quartz module.
 * 
 * @author Jens Fendler
 *
 */
public class NinjaQuartzIntegrationTest {

    private static Standalone<?> standalone;

    private static OkHttpClient client;

    private static okhttp3.Request.Builder requestBuilder;

    @SuppressWarnings("rawtypes")
    @BeforeClass
    static public void beforeClass() throws Exception {
        // start ninja application
        Application.LOG.info("Starting Ninja server...");
        int randomPort = StandaloneHelper.findAvailablePort(8090, 9000);
        Class<? extends Standalone> standaloneClass = StandaloneHelper.resolveStandaloneClass();
        standalone = StandaloneHelper.create(standaloneClass).ninjaMode(NinjaMode.test).port(randomPort).configure()
                .start();

        // prepare http client
        Application.LOG.info("Initializing OKHttpClient...");
        requestBuilder = new Request.Builder();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        client = new OkHttpClient.Builder().followRedirects(true).addInterceptor(loggingInterceptor).build();

    }

    @AfterClass
    static public void afterClass() {
        // clean application shutdown
        standalone.shutdown();
    }

    @Test
    public void testApplication() throws Exception {
        Response response = requestGet("/");
        assertTrue("Index page failed with code " + response.code(), response.isSuccessful());
    }

    @Test
    public void testSchedule1() throws Exception {
        Application.LOG.info("Pausing 15s for Quartz to run scheduled methods...");
        // give the scheduler some time to run the test methods
        Thread.sleep(15000);
        Application.LOG.info("Done pausing.");

        Response response = requestGet("/schedules");
        assertTrue("Schedules page failed with code " + response.code(), response.isSuccessful());

        String[] values = response.body().string().split(",");
        assertTrue("Expected (5) values not received", (values != null) && (values.length == 5));

        for (int i = 0; i < values.length; i++) {
            try {
                Integer value = Integer.parseInt(values[i]);
                assertTrue("Schedule method number " + (i + 1) + " seems to not have run (non-positive counter value)",
                        (value > 0));
            } catch (NumberFormatException nfe) {
                assertTrue("Counter value is not an integer: " + values[i], false);
            }
        }
    }

    /**
     * HTTP Client Helper method to issue a GET request
     * 
     * @param appPath
     * @return
     * @throws IOException
     */
    protected Response requestGet(String appPath) throws IOException {
        Request request = buildGetRequest(appPath);
        return client.newCall(request).execute();
    }

    /**
     * HTTP Client helper method to build a GET request.
     * 
     * @param appPath
     * @return
     */
    protected Request buildGetRequest(String appPath) {
        return requestBuilder.get().url(standalone.getBaseUrls().get(0) + appPath).build();
    }
}
