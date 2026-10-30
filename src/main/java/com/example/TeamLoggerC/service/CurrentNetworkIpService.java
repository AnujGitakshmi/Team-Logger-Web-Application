
package com.example.TeamLoggerC.service;

import org.springframework.stereotype.Service;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class CurrentNetworkIpService {

    private static final Logger LOGGER =
            Logger.getLogger(CurrentNetworkIpService.class.getName());

    /**
     * Returns the preferred local IPv4 address of an active network adapter.
     *
     * Prefers active Wi-Fi/Ethernet adapters and ordinary LAN addresses.
     * Loopback, link-local, and known virtual adapters receive lower priority.
     *
     * This is a local adapter address, not necessarily the public IP address
     * seen by a remote server.
     */
    public String getCurrentIpv4Address() {

        List<IpCandidate> candidates = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> interfaces =
                    NetworkInterface.getNetworkInterfaces();

            if (interfaces == null) {
                LOGGER.warning(
                        "No network interfaces were returned by the operating system."
                );
                return null;
            }

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                try {
                    if (!networkInterface.isUp()
                            || networkInterface.isLoopback()) {
                        continue;
                    }

                    String interfaceName =
                            safeValue(networkInterface.getName());

                    String displayName =
                            safeValue(networkInterface.getDisplayName());

                    boolean virtualInterface =
                            networkInterface.isVirtual()
                                    || isKnownVirtualAdapter(
                                    interfaceName + " " + displayName
                            );

                    Enumeration<InetAddress> addresses =
                            networkInterface.getInetAddresses();

                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();

                        if (!(address instanceof Inet4Address)
                                || address.isLoopbackAddress()
                                || address.isAnyLocalAddress()
                                || address.isMulticastAddress()) {
                            continue;
                        }

                        String ipv4 = address.getHostAddress();

                        int score = calculateScore(
                                interfaceName,
                                displayName,
                                address,
                                virtualInterface
                        );

                        IpCandidate candidate = new IpCandidate(
                                ipv4,
                                interfaceName,
                                displayName,
                                score
                        );

                        candidates.add(candidate);

                        LOGGER.fine(
                                "IPv4 candidate: address=" + ipv4
                                        + ", interface=" + interfaceName
                                        + ", displayName=" + displayName
                                        + ", virtual=" + virtualInterface
                                        + ", score=" + score
                        );
                    }

                } catch (SocketException | SecurityException e) {
                    LOGGER.log(
                            Level.WARNING,
                            "Unable to inspect network interface "
                                    + networkInterface.getName(),
                            e
                    );
                }
            }

        } catch (SocketException | SecurityException e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Unable to enumerate network interfaces.",
                    e
            );
            return null;
        }

        if (candidates.isEmpty()) {
            LOGGER.warning(
                    "No suitable local IPv4 address was found."
            );
            return null;
        }

        candidates.sort(
                Comparator.comparingInt(IpCandidate::score)
                        .reversed()
                        .thenComparing(IpCandidate::interfaceName)
        );

        IpCandidate selected = candidates.get(0);

        LOGGER.info(
                "Selected local IPv4 address: " + selected.address()
                        + ", interface=" + selected.interfaceName()
                        + ", displayName=" + selected.displayName()
                        + ", score=" + selected.score()
        );

        if (candidates.size() > 1) {
            LOGGER.info(
                    "Found " + candidates.size()
                            + " IPv4 candidates. The highest-ranked "
                            + "candidate was selected."
            );
        }

        return selected.address();
    }

    private int calculateScore(
            String interfaceName,
            String displayName,
            InetAddress address,
            boolean virtualInterface
    ) {
        int score = 0;

        String adapterText =
                (interfaceName + " " + displayName)
                        .toLowerCase(Locale.ROOT);

        // Prefer conventional Wi-Fi and Ethernet adapters.
        if (containsAny(
                adapterText,
                "wi-fi",
                "wifi",
                "wireless",
                "wlan",
                "ethernet"
        )) {
            score += 40;
        }

        // Prefer private LAN addresses such as 192.168.x.x and 10.x.x.x.
        if (address.isSiteLocalAddress()) {
            score += 25;
        }

        // Link-local addresses (169.254.x.x) are usually not suitable
        // for normal office/Wi-Fi network policy selection.
        if (address.isLinkLocalAddress()) {
            score -= 50;
        }

        // Known virtual adapters should generally lose to a physical LAN
        // adapter when both are available.
        if (virtualInterface) {
            score -= 100;
        }

        // VPN addresses can be valid for some deployments, so do not
        // automatically reject them. Their priority depends on the
        // available adapters and the naming of the interface.
        if (containsAny(
                adapterText,
                "vpn",
                "wireguard",
                "openvpn",
                "tailscale",
                "zerotier"
        )) {
            score -= 10;
        }

        return score;
    }

    private boolean isKnownVirtualAdapter(String adapterText) {
        String value = adapterText.toLowerCase(Locale.ROOT);

        return containsAny(
                value,
                "virtualbox",
                "vmware",
                "hyper-v",
                "hyperv",
                "virtual ethernet",
                "virtual adapter",
                "host-only",
                "host only",
                "docker",
                "wsl",
                "loopback"
        );
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private record IpCandidate(
            String address,
            String interfaceName,
            String displayName,
            int score
    ) {
    }
}
