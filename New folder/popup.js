const PAIR_URL =
    "http://localhost:8080/employee/browser-extension/pair";

const TOKEN_KEY = "teamLoggerToken";

const statusElement = document.getElementById("status");
const pairButton = document.getElementById("pairButton");
const unpairButton = document.getElementById("unpairButton");

async function updateStatus() {
    const result = await chrome.storage.local.get(TOKEN_KEY);

    if (result[TOKEN_KEY]) {
        statusElement.textContent =
            "Extension is paired with TeamLoggerC.";
        pairButton.textContent = "Pair Again";
        unpairButton.disabled = false;
    } else {
        statusElement.textContent =
            "Extension is not paired. Log in to TeamLoggerC first.";
        pairButton.textContent = "Pair with TeamLoggerC";
        unpairButton.disabled = true;
    }
}

pairButton.addEventListener("click", async () => {
    pairButton.disabled = true;
    statusElement.textContent =
        "Connecting to TeamLoggerC...";

    try {
        // This request uses the employee's existing TeamLogger
        // browser session and its JSESSIONID cookie.
        const response = await fetch(PAIR_URL, {
            method: "POST",
            credentials: "include",
            headers: {
                "Accept": "application/json"
            }
        });

        const data = await response.json().catch(() => ({}));

        if (!response.ok) {
            throw new Error(
                data.error ||
                `Pairing failed (HTTP ${response.status}).`
            );
        }

        if (!data.token) {
            throw new Error(
                "The server did not return an extension token."
            );
        }

        await chrome.storage.local.set({
            [TOKEN_KEY]: data.token
        });

        statusElement.textContent =
            "Successfully paired with TeamLoggerC.";

        await updateStatus();

    } catch (error) {
        statusElement.textContent =
            error.message ||
            "Pairing failed. Check your TeamLoggerC login.";

    } finally {
        pairButton.disabled = false;
    }
});

unpairButton.addEventListener("click", async () => {
    await chrome.storage.local.remove(TOKEN_KEY);

    statusElement.textContent =
        "Extension token removed from this browser.";

    await updateStatus();
});

updateStatus();
