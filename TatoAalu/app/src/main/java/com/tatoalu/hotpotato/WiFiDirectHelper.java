package com.tatoalu.hotpotato;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Wi-Fi Direct Helper for Hot Potato game
 * Based on LocalDash patterns for peer-to-peer networking without internet
 */
public class WiFiDirectHelper {
    private static final String TAG = "WiFiDirectHelper";
    
    private final Context context;
    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    
    private WiFiDirectBroadcastReceiver receiver;
    private IntentFilter intentFilter;
    
    private boolean isWifiP2pEnabled = false;
    private boolean isDiscovering = false;
    private boolean isGroupOwner = false;
    
    private final List<WifiP2pDevice> peers = new ArrayList<>();
    private WifiP2pDevice connectedDevice;
    private WifiP2pInfo connectionInfo;
    
    // Listeners
    private WiFiDirectListener wifiDirectListener;
    private PeerDiscoveryListener peerDiscoveryListener;
    private ConnectionListener connectionListener;
    
    public interface WiFiDirectListener {
        void onWiFiP2pEnabled(boolean enabled);
        void onPeersChanged(List<WifiP2pDevice> peers);
        void onConnectionChanged(WifiP2pInfo info);
        void onDeviceChanged(WifiP2pDevice device);
        void onDiscoveryStarted();
        void onDiscoveryStopped();
        void onError(String error);
    }
    
    public interface PeerDiscoveryListener {
        void onPeerFound(WifiP2pDevice device);
        void onPeerLost(WifiP2pDevice device);
        void onPeerListUpdated(List<WifiP2pDevice> peers);
    }
    
    public interface ConnectionListener {
        void onConnectionEstablished(WifiP2pInfo info);
        void onConnectionLost();
        void onConnectionFailed(String reason);
        void onGroupFormed(WifiP2pGroup group);
        void onGroupRemoved();
    }
    
    public WiFiDirectHelper(Context context) {
        this.context = context.getApplicationContext();
        
        // Initialize Wi-Fi P2P Manager
        manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        if (manager != null) {
            channel = manager.initialize(context, context.getMainLooper(), null);
        } else {
            channel = null;
            Log.e(TAG, "Wi-Fi P2P is not supported on this device");
        }
        
        setupBroadcastReceiver();
    }
    
    public void setWiFiDirectListener(WiFiDirectListener listener) {
        this.wifiDirectListener = listener;
    }
    
    public void setPeerDiscoveryListener(PeerDiscoveryListener listener) {
        this.peerDiscoveryListener = listener;
    }
    
    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }
    
    private void setupBroadcastReceiver() {
        receiver = new WiFiDirectBroadcastReceiver();
        
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }
    
    /**
     * Register the broadcast receiver
     */
    public void registerReceiver() {
        if (receiver != null && intentFilter != null) {
            context.registerReceiver(receiver, intentFilter);
            Log.d(TAG, "WiFi Direct broadcast receiver registered");
        }
    }
    
    /**
     * Unregister the broadcast receiver
     */
    public void unregisterReceiver() {
        try {
            if (receiver != null) {
                context.unregisterReceiver(receiver);
                Log.d(TAG, "WiFi Direct broadcast receiver unregistered");
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Receiver was not registered", e);
        }
    }
    
    /**
     * Start peer discovery
     */
    public void startPeerDiscovery() {
        if (manager == null || channel == null) {
            notifyError("Wi-Fi P2P not available");
            return;
        }
        
        if (!isWifiP2pEnabled) {
            notifyError("Wi-Fi P2P is not enabled");
            return;
        }
        
        if (isDiscovering) {
            Log.w(TAG, "Peer discovery already in progress");
            return;
        }
        
        // Check permissions
        if (!hasRequiredPermissions()) {
            notifyError("Required permissions not granted");
            return;
        }
        
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Peer discovery started successfully");
                isDiscovering = true;
                if (wifiDirectListener != null) {
                    wifiDirectListener.onDiscoveryStarted();
                }
            }
            
            @Override
            public void onFailure(int reasonCode) {
                Log.e(TAG, "Failed to start peer discovery: " + reasonCode);
                isDiscovering = false;
                notifyError("Failed to start peer discovery: " + getErrorMessage(reasonCode));
            }
        });
    }
    
    /**
     * Stop peer discovery
     */
    public void stopPeerDiscovery() {
        if (manager == null || channel == null) {
            return;
        }
        
        if (!isDiscovering) {
            Log.w(TAG, "Peer discovery not in progress");
            return;
        }
        
        manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Peer discovery stopped successfully");
                isDiscovering = false;
                if (wifiDirectListener != null) {
                    wifiDirectListener.onDiscoveryStopped();
                }
            }
            
            @Override
            public void onFailure(int reasonCode) {
                Log.e(TAG, "Failed to stop peer discovery: " + reasonCode);
                isDiscovering = false;
                notifyError("Failed to stop peer discovery: " + getErrorMessage(reasonCode));
            }
        });
    }
    
    /**
     * Connect to a specific peer
     */
    public void connectToPeer(WifiP2pDevice device) {
        if (manager == null || channel == null) {
            notifyError("Wi-Fi P2P not available");
            return;
        }
        
        if (!hasRequiredPermissions()) {
            notifyError("Required permissions not granted");
            return;
        }
        
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        
        // Set group owner intent (0 = least likely to be GO, 15 = most likely)
        config.groupOwnerIntent = 0; // Let the other device be group owner
        
        Log.d(TAG, "Connecting to peer: " + device.deviceName + " (" + device.deviceAddress + ")");
        
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Connection initiated successfully to " + device.deviceName);
                connectedDevice = device;
            }
            
            @Override
            public void onFailure(int reasonCode) {
                Log.e(TAG, "Failed to connect to peer: " + reasonCode);
                if (connectionListener != null) {
                    connectionListener.onConnectionFailed("Failed to connect: " + getErrorMessage(reasonCode));
                }
            }
        });
    }
    
    /**
     * Create a Wi-Fi P2P group (become group owner)
     */
    public void createGroup() {
        if (manager == null || channel == null) {
            notifyError("Wi-Fi P2P not available");
            return;
        }
        
        if (!hasRequiredPermissions()) {
            notifyError("Required permissions not granted");
            return;
        }
        
        manager.createGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Group created successfully");
                isGroupOwner = true;
            }
            
            @Override
            public void onFailure(int reasonCode) {
                Log.e(TAG, "Failed to create group: " + reasonCode);
                isGroupOwner = false;
                notifyError("Failed to create group: " + getErrorMessage(reasonCode));
            }
        });
    }
    
    /**
     * Remove the current Wi-Fi P2P group
     */
    public void removeGroup() {
        if (manager == null || channel == null) {
            return;
        }
        
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Group removed successfully");
                isGroupOwner = false;
                connectedDevice = null;
                connectionInfo = null;
                if (connectionListener != null) {
                    connectionListener.onGroupRemoved();
                }
            }
            
            @Override
            public void onFailure(int reasonCode) {
                Log.e(TAG, "Failed to remove group: " + reasonCode);
                notifyError("Failed to remove group: " + getErrorMessage(reasonCode));
            }
        });
    }
    
    /**
     * Disconnect from current peer
     */
    public void disconnect() {
        if (manager == null || channel == null) {
            return;
        }
        
        manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Disconnection initiated");
            }
            
            @Override
            public void onFailure(int reasonCode) {
                Log.e(TAG, "Failed to disconnect: " + reasonCode);
            }
        });
        
        // Also try to remove group
        removeGroup();
    }
    
    /**
     * Request peer list
     */
    private void requestPeers() {
        if (manager == null || channel == null) {
            return;
        }
        
        if (!hasRequiredPermissions()) {
            return;
        }
        
        manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerList) {
                Collection<WifiP2pDevice> refreshedPeers = peerList.getDeviceList();
                
                // Update peers list
                peers.clear();
                peers.addAll(refreshedPeers);
                
                Log.d(TAG, "Peers updated: " + peers.size() + " devices found");
                
                // Notify listeners
                if (wifiDirectListener != null) {
                    wifiDirectListener.onPeersChanged(new ArrayList<>(peers));
                }
                
                if (peerDiscoveryListener != null) {
                    peerDiscoveryListener.onPeerListUpdated(new ArrayList<>(peers));
                }
            }
        });
    }
    
    /**
     * Request connection info
     */
    private void requestConnectionInfo() {
        if (manager == null || channel == null) {
            return;
        }
        
        manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                connectionInfo = info;
                
                Log.d(TAG, "Connection info updated - Group formed: " + info.groupFormed + 
                      ", Is group owner: " + info.isGroupOwner + 
                      ", Group owner address: " + (info.groupOwnerAddress != null ? info.groupOwnerAddress.getHostAddress() : "null"));
                
                if (info.groupFormed) {
                    isGroupOwner = info.isGroupOwner;
                    
                    if (connectionListener != null) {
                        connectionListener.onConnectionEstablished(info);
                    }
                    
                    if (wifiDirectListener != null) {
                        wifiDirectListener.onConnectionChanged(info);
                    }
                }
            }
        });
    }
    
    /**
     * Check if required permissions are granted
     */
    private boolean hasRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    /**
     * Get error message for Wi-Fi P2P error codes
     */
    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case WifiP2pManager.ERROR:
                return "Internal error";
            case WifiP2pManager.P2P_UNSUPPORTED:
                return "Wi-Fi P2P is not supported";
            case WifiP2pManager.BUSY:
                return "System is busy";
            case WifiP2pManager.NO_SERVICE_REQUESTS:
                return "No service requests";
            default:
                return "Unknown error (" + errorCode + ")";
        }
    }
    
    private void notifyError(String error) {
        Log.e(TAG, error);
        if (wifiDirectListener != null) {
            wifiDirectListener.onError(error);
        }
    }
    
    // Getters
    public boolean isWifiP2pEnabled() {
        return isWifiP2pEnabled;
    }
    
    public boolean isDiscovering() {
        return isDiscovering;
    }
    
    public boolean isGroupOwner() {
        return isGroupOwner;
    }
    
    public List<WifiP2pDevice> getPeers() {
        return new ArrayList<>(peers);
    }
    
    public WifiP2pDevice getConnectedDevice() {
        return connectedDevice;
    }
    
    public WifiP2pInfo getConnectionInfo() {
        return connectionInfo;
    }
    
    public InetAddress getGroupOwnerAddress() {
        return connectionInfo != null ? connectionInfo.groupOwnerAddress : null;
    }
    
    /**
     * Broadcast receiver for Wi-Fi P2P events
     */
    private class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                // Check if Wi-Fi P2P is enabled
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                isWifiP2pEnabled = (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
                
                Log.d(TAG, "Wi-Fi P2P state changed: " + (isWifiP2pEnabled ? "ENABLED" : "DISABLED"));
                
                if (wifiDirectListener != null) {
                    wifiDirectListener.onWiFiP2pEnabled(isWifiP2pEnabled);
                }
                
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                // Peer list has changed
                Log.d(TAG, "Peers changed, requesting peer list");
                requestPeers();
                
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                // Connection state has changed
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                WifiP2pInfo wifiP2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
                WifiP2pGroup group = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
                
                Log.d(TAG, "Connection changed - Network connected: " + 
                      (networkInfo != null && networkInfo.isConnected()) + 
                      ", Group formed: " + (wifiP2pInfo != null && wifiP2pInfo.groupFormed));
                
                if (networkInfo != null && networkInfo.isConnected()) {
                    // Connected to P2P network
                    requestConnectionInfo();
                    
                    if (group != null && connectionListener != null) {
                        connectionListener.onGroupFormed(group);
                    }
                } else {
                    // Disconnected from P2P network
                    connectionInfo = null;
                    connectedDevice = null;
                    isGroupOwner = false;
                    
                    if (connectionListener != null) {
                        connectionListener.onConnectionLost();
                    }
                }
                
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                // This device's details have changed
                WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                
                Log.d(TAG, "This device changed: " + (device != null ? device.deviceName : "null"));
                
                if (wifiDirectListener != null && device != null) {
                    wifiDirectListener.onDeviceChanged(device);
                }
            }
        }
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up WiFi Direct Helper");
        
        // Stop discovery
        if (isDiscovering) {
            stopPeerDiscovery();
        }
        
        // Disconnect and remove group
        disconnect();
        
        // Unregister receiver
        unregisterReceiver();
        
        // Clear listeners
        wifiDirectListener = null;
        peerDiscoveryListener = null;
        connectionListener = null;
        
        // Clear data
        peers.clear();
        connectedDevice = null;
        connectionInfo = null;
    }
}