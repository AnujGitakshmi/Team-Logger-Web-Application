const API_URL =
    "http://localhost:8080/api/extension/website-activity";

// Prevent overlapping submissions for the same URL.
let lastSubmittedUrl = "";
let lastSubmittedAt = 0;

function getWebsiteName(url) {
    try {
        const hostname = new URL(url).hostname
            .toLowerCase()
            .replace(/^www\./, "");

        const knownSites = {
            "youtube.com": "YouTube",
            "youtu.be": "YouTube",
            "facebook.com": "Facebook",
            "instagram.com": "Instagram",
            "linkedin.com": "LinkedIn",
            "web.whatsapp.com": "WhatsApp Web",
            "whatsapp.com": "WhatsApp",
            "github.com": "GitHub",
            "google.com": "Google"
        };

        for (const domain in knownSites) {
            if (
                hostname === domain ||
                hostname.endsWith("." + domain)
            ) {
                return knownSites[domain];
            }
        }

        return hostname;
    } catch (error) {
        return "Unknown Website";
    }
}

async function sendWebsiteActivity(website) {
    try {
        const stored = await chrome.storage.local.get([
            "teamLoggerToken"
        ]);

        const token = stored.teamLoggerToken;

        if (!token) {
            console.log(
                "TeamLogger extension is not paired yet."
            );
            return;
        }

        const response = await fetch(API_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify(website)
        });

        if (response.status === 401) {
            console.error(
                "TeamLogger token is invalid, expired, or revoked. Pair again."
            );
            return;
        }

        if (!response.ok) {
            console.error(
                "TeamLogger activity submission failed:",
                response.status,
                await response.text()
            );
            return;
        }

        console.log(
            "TeamLogger activity submitted:",
            website.websiteName
        );
    } catch (error) {
        console.error(
            "Could not send activity to TeamLoggerC:",
            error
        );
    }
}

async function detectActiveWebsite() {
    try {
        const tabs = await chrome.tabs.query({
            active: true,
            lastFocusedWindow: true
        });

        const tab = tabs[0];

        if (!tab || !tab.url) {
            return;
        }

        // Only record ordinary web pages.
        const parsedUrl = new URL(tab.url);

        if (!["http:", "https:"].includes(parsedUrl.protocol)) {
            return;
        }

        const now = Date.now();

        // Avoid duplicate submissions for the same URL
        // within a short interval.
        if (
            tab.url === lastSubmittedUrl &&
            now - lastSubmittedAt < 5000
        ) {
            return;
        }

        const website = {
            websiteName: getWebsiteName(tab.url),
            url: tab.url,
            pageTitle: tab.title || "",
            browser: "Google Chrome"
        };

        console.log("TeamLogger detected website:", website);

        // Submit only when the extension has a pairing token.
        await sendWebsiteActivity(website);

        lastSubmittedUrl = tab.url;
        lastSubmittedAt = now;

    } catch (error) {
        console.error(
            "Website detection failed:",
            error
        );
    }
}

chrome.tabs.onActivated.addListener(() => {
    detectActiveWebsite();
});

chrome.tabs.onUpdated.addListener(
    (tabId, changeInfo, tab) => {
        if (
            tab.active &&
            (
                changeInfo.url ||
                changeInfo.status === "complete"
            )
        ) {
            detectActiveWebsite();
        }
    }
);

chrome.windows.onFocusChanged.addListener(() => {
    detectActiveWebsite();
});

// Run once when Chrome starts the service worker.
detectActiveWebsite();
