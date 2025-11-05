package com.tatoalu.hotpotato;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Enhanced NSD Helper with LocalDash patterns for Hot Potato game
 * Provides robust service discovery, registration, and connection management
 */
public class NsdHelper {

    private static final String TAG = "NsdHelper";
    public static final String SERVICE_TYPE = "_hotpotato._tcp.";
    public static final String SERVICE_NAME_PREFIX = "HotPotato";
    
    private final Context mContext;
    private final NsdManager mNsdManager;
    
    private NsdManager.ResolveListener mResolveListener;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.RegistrationListener mRegistrationListener;
    
    private String mServiceName;
    private NsdServiceInfo mRegisteredService;
    private final List<NsdServiceInfo> mDiscoveredServices;
    
    private boolean isDiscovering = false;
    private boolean isRegistered = false;
    
    // Callbacks for service events
    private ServiceDiscoveryListener discoveryListener;
    private ServiceRegistrationListener registrationListener;
    
    public interface ServiceDiscoveryListener {
        void onServiceFound(NsdServiceInfo serviceInfo);
        void onServiceLost(NsdServiceInfo serviceInfo);
        void onServiceResolved(NsdServiceInfo serviceInfo);
        void onDiscoveryStarted();
        void onDiscoveryStopped();
        void onDiscoveryFailed(int errorCode);
    }
    
    public interface ServiceRegistrationListener {
        void onServiceRegistered(NsdServiceInfo serviceInfo);
        void onServiceUnregistered();
        void onRegistrationFailed(int errorCode);
        void onUnregistrationFailed(int errorCode);
    }

    public NsdHelper(Context context) {
        mContext = context;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        mDiscoveredServices = new CopyOnWriteArrayList<>();
        mServiceName = SERVICE_NAME_PREFIX + "_" + System.currentTimeMillis();
        
        initializeNsd();
    }
    
    public void setServiceDiscoveryListener(ServiceDiscoveryListener listener) {
        this.discoveryListener = listener;
    }
    
    public void setServiceRegistrationListener(ServiceRegistrationListener listener) {
        this.registrationListener = listener;
    }

    public void initializeNsd() {
        initializeResolveListener();
        initializeDiscoveryListener();
        initializeRegistrationListener();
    }

    private void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started for: " + regType);
                isDiscovering = true;
                if (discoveryListener != null) {
                    discoveryListener.onDiscoveryStarted();
                }
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "Service found: " + service.getServiceName());
                
                // Check if it's our service type
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                    return;
                }
                
                // Don't resolve our own service
                if (service.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Found our own service: " + mServiceName);
                    return;
                }
                
                // Check if it's a Hot Potato game service
                if (service.getServiceName().startsWith(SERVICE_NAME_PREFIX)) {
                    Log.d(TAG, "Found Hot Potato service, resolving: " + service.getServiceName());
                    mNsdManager.resolveService(service, mResolveListener);
                }
                
                if (discoveryListener != null) {
                    discoveryListener.onServiceFound(service);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.w(TAG, "Service lost: " + service.getServiceName());
                
                // Remove from discovered services
                for (java.util.Iterator<NsdServiceInfo> iterator = mDiscoveredServices.iterator(); iterator.hasNext(); ) {
                    NsdServiceInfo s = iterator.next();
                    if (s.getServiceName().equals(service.getServiceName())) {
                        iterator.remove();
                        break;
                    }
                }
                
                if (discoveryListener != null) {
                    discoveryListener.onServiceLost(service);
                }
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
                isDiscovering = false;
                if (discoveryListener != null) {
                    discoveryListener.onDiscoveryStopped();
                }
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Start discovery failed for " + serviceType + ", error: " + errorCode);
                isDiscovering = false;
                
                // Try to stop discovery to clean up
                try {
                    mNsdManager.stopServiceDiscovery(this);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to stop discovery after start failure", e);
                }
                
                if (discoveryListener != null) {
                    discoveryListener.onDiscoveryFailed(errorCode);
                }
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Stop discovery failed for " + serviceType + ", error: " + errorCode);
                isDiscovering = false;
                
                if (discoveryListener != null) {
                    discoveryListener.onDiscoveryFailed(errorCode);
                }
            }
        };
    }

    private void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed for " + serviceInfo.getServiceName() + ", error: " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Service resolved: " + serviceInfo.getServiceName());
                
                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Resolved our own service");
                    return;
                }
                
                // Add to discovered services if not already present
                boolean alreadyExists = false;
                for (NsdServiceInfo existing : mDiscoveredServices) {
                    if (existing.getServiceName().equals(serviceInfo.getServiceName())) {
                        alreadyExists = true;
                        break;
                    }
                }

                if (!alreadyExists) {
                    mDiscoveredServices.add(serviceInfo);
                }
                
                if (discoveryListener != null) {
                    discoveryListener.onServiceResolved(serviceInfo);
                }
            }
        };
    }

    private void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Service registered: " + serviceInfo.getServiceName());
                mServiceName = serviceInfo.getServiceName();
                mRegisteredService = serviceInfo;
                isRegistered = true;
                
                if (registrationListener != null) {
                    registrationListener.onServiceRegistered(serviceInfo);
                }
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Registration failed for " + serviceInfo.getServiceName() + ", error: " + errorCode);
                isRegistered = false;
                
                if (registrationListener != null) {
                    registrationListener.onRegistrationFailed(errorCode);
                }
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Service unregistered: " + serviceInfo.getServiceName());
                isRegistered = false;
                mRegisteredService = null;
                
                if (registrationListener != null) {
                    registrationListener.onServiceUnregistered();
                }
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Unregistration failed for " + serviceInfo.getServiceName() + ", error: " + errorCode);
                
                if (registrationListener != null) {
                    registrationListener.onUnregistrationFailed(errorCode);
                }
            }
        };
    }

    /**
     * Register the Hot Potato game service
     */
    public void registerService(int port, String playerName) {
        if (isRegistered) {
            Log.w(TAG, "Service already registered");
            return;
        }
        
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(port);
        
        // Add player name as attribute if supported
        try {
            serviceInfo.setAttribute("player", playerName);
            serviceInfo.setAttribute("game", "hotpotato");
            serviceInfo.setAttribute("version", "1.0");
        } catch (Exception e) {
            Log.w(TAG, "Failed to set service attributes", e);
        }

        try {
            mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
            Log.d(TAG, "Registering service: " + mServiceName + " on port " + port);
        } catch (Exception e) {
            Log.e(TAG, "Failed to register service", e);
            if (registrationListener != null) {
                registrationListener.onRegistrationFailed(-1);
            }
        }
    }

    /**
     * Start discovering Hot Potato game services
     */
    public void discoverServices() {
        if (isDiscovering) {
            Log.w(TAG, "Discovery already in progress");
            return;
        }
        
        // Clear previous discoveries
        mDiscoveredServices.clear();
        
        try {
            mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
            Log.d(TAG, "Starting service discovery for: " + SERVICE_TYPE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service discovery", e);
            if (discoveryListener != null) {
                discoveryListener.onDiscoveryFailed(-1);
            }
        }
    }

    /**
     * Stop service discovery
     */
    public void stopDiscovery() {
        if (!isDiscovering) {
            Log.w(TAG, "Discovery not in progress");
            return;
        }
        
        try {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            Log.d(TAG, "Stopping service discovery");
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop service discovery", e);
        }
    }

    /**
     * Unregister the service
     */
    public void unregisterService() {
        if (!isRegistered || mRegisteredService == null) {
            Log.w(TAG, "No service to unregister");
            return;
        }
        
        try {
            mNsdManager.unregisterService(mRegistrationListener);
            Log.d(TAG, "Unregistering service: " + mServiceName);
        } catch (Exception e) {
            Log.e(TAG, "Failed to unregister service", e);
        }
    }

    /**
     * Get all discovered Hot Potato services
     */
    public List<NsdServiceInfo> getDiscoveredServices() {
        return new ArrayList<>(mDiscoveredServices);
    }

    /**
     * Get the registered service info
     */
    public NsdServiceInfo getRegisteredService() {
        return mRegisteredService;
    }

    /**
     * Check if currently discovering services
     */
    public boolean isDiscovering() {
        return isDiscovering;
    }

    /**
     * Check if service is registered
     */
    public boolean isRegistered() {
        return isRegistered;
    }

    /**
     * Get the service name
     */
    public String getServiceName() {
        return mServiceName;
    }

    /**
     * Clean up all NSD operations
     */
    public void tearDown() {
        Log.d(TAG, "Tearing down NSD Helper");
        
        // Stop discovery if active
        if (isDiscovering) {
            stopDiscovery();
        }
        
        // Unregister service if registered
        if (isRegistered) {
            unregisterService();
        }
        
        // Clear discovered services
        mDiscoveredServices.clear();
        
        // Clear listeners
        discoveryListener = null;
        registrationListener = null;
    }
}