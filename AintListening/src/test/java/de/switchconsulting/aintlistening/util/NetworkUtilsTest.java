/*
 * Copyright 2026 Switch Consulting (https://switch-consulting.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.switchconsulting.aintlistening.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link NetworkUtils}.
 */
public class NetworkUtilsTest {

    private Context context;
    private ConnectivityManager connectivityManager;
    private NetworkCapabilities networkCapabilities;

    @Before
    public void setUp() {
        context = mock(Context.class);
        connectivityManager = mock(ConnectivityManager.class);
        networkCapabilities = mock(NetworkCapabilities.class);

        when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);
        Network network = mock(Network.class);
        when(connectivityManager.getActiveNetwork()).thenReturn(network);
        when(connectivityManager.getNetworkCapabilities(any())).thenReturn(networkCapabilities);
    }

    @Test
    public void testIsOnlineWhenConnectivityManagerIsNull() {
        when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(null);
        assertFalse(NetworkUtils.isOnline(context));
    }

    @Test
    public void testIsOnlineWhenCapabilitiesIsNull() {
        when(connectivityManager.getNetworkCapabilities(any())).thenReturn(null);
        assertFalse(NetworkUtils.isOnline(context));
    }

    @Test
    public void testIsOnlineWithWifi() {
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true);
        assertTrue(NetworkUtils.isOnline(context));
    }

    @Test
    public void testIsOnlineWithCellular() {
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(true);
        assertTrue(NetworkUtils.isOnline(context));
    }

    @Test
    public void testIsOnlineWithEthernet() {
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)).thenReturn(true);
        assertTrue(NetworkUtils.isOnline(context));
    }

    @Test
    public void testIsOnlineWhenNoTransportsMatch() {
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(false);
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(false);
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)).thenReturn(false);
        assertFalse(NetworkUtils.isOnline(context));
    }
}
