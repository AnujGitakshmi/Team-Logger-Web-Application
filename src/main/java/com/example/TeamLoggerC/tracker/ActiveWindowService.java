
package com.example.TeamLoggerC.tracker;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class ActiveWindowService {

    private static final Logger log =
            LoggerFactory.getLogger(ActiveWindowService.class);

    private static final int PROCESS_QUERY_LIMITED_INFORMATION = 0x1000;
    private static final int MAX_WINDOW_TITLE_LENGTH = 4096;
    private static final int MAX_EXECUTABLE_PATH_LENGTH = 32768;

    private final User32 user32;
    private final Kernel32 kernel32;

    public ActiveWindowService() {
        this.user32 = User32.INSTANCE;
        this.kernel32 = Kernel32.INSTANCE;
    }

    /**
     * Returns the handle of the foreground Windows window.
     */
    public HWND getActiveWindowHandle() {
        return user32.GetForegroundWindow();
    }

    /**
     * Returns the foreground window title.
     */
    public String getActiveWindowTitle() {
        HWND hwnd = getActiveWindowHandle();

        if (hwnd == null) {
            return "";
        }

        return readWindowTitle(hwnd);
    }

    /**
     * Returns the process ID associated with the foreground window.
     */
    public int getActiveProcessId() {
        HWND hwnd = getActiveWindowHandle();

        if (hwnd == null) {
            return 0;
        }

        return getProcessId(hwnd);
    }

    /**
     * Returns the executable name of the foreground application.
     *
     * Examples: chrome.exe, msedge.exe, idea64.exe.
     */
    public String getActiveExecutableName() {
        int processId = getActiveProcessId();

        if (processId <= 0) {
            return "";
        }

        return getExecutableNameByProcessId(processId);
    }

    /**
     * Returns a human-readable name for the foreground application.
     */
    public String getActiveApplicationName() {
        return getApplicationDisplayName(getActiveExecutableName());
    }

    /**
     * Collects application information from one foreground-window
     * handle so the title and PID refer to the same window snapshot.
     */
    public ActiveWindowInfo getActiveWindowInfo() {
        HWND hwnd = getActiveWindowHandle();

        if (hwnd == null) {
            return null;
        }

        int processId = getProcessId(hwnd);

        if (processId <= 0) {
            return new ActiveWindowInfo(
                    "",
                    "",
                    0,
                    readWindowTitle(hwnd)
            );
        }

        String executableName =
                getExecutableNameByProcessId(processId);

        String applicationName =
                getApplicationDisplayName(executableName);

        String windowTitle = readWindowTitle(hwnd);

        return new ActiveWindowInfo(
                applicationName,
                executableName,
                processId,
                windowTitle
        );
    }

    private int getProcessId(HWND hwnd) {
        if (hwnd == null) {
            return 0;
        }

        IntByReference processIdReference = new IntByReference();

        user32.GetWindowThreadProcessId(hwnd, processIdReference);

        return processIdReference.getValue();
    }

    private String readWindowTitle(HWND hwnd) {
        if (hwnd == null) {
            return "";
        }

        char[] windowText = new char[MAX_WINDOW_TITLE_LENGTH];

        int length = user32.GetWindowText(
                hwnd,
                windowText,
                windowText.length
        );

        if (length <= 0) {
            return "";
        }

        return new String(windowText, 0, length).trim();
    }

    /**
     * Obtains the executable name for a particular PID.
     *
     * An empty string means Windows did not provide a usable name.
     * This may happen when the process exits or access is denied.
     */
    private String getExecutableNameByProcessId(int processId) {
        if (processId <= 0) {
            return "";
        }

        HANDLE process = kernel32.OpenProcess(
                PROCESS_QUERY_LIMITED_INFORMATION,
                false,
                processId
        );

        if (process == null) {
            int errorCode = kernel32.GetLastError();

            log.debug(
                    "Could not open process for executable lookup. "
                            + "PID={}, WindowsErrorCode={}",
                    processId,
                    errorCode
            );

            return "";
        }

        try {
            char[] executablePath =
                    new char[MAX_EXECUTABLE_PATH_LENGTH];

            IntByReference size =
                    new IntByReference(executablePath.length);

            boolean success = kernel32.QueryFullProcessImageName(
                    process,
                    0,
                    executablePath,
                    size
            );

            if (!success) {
                int errorCode = kernel32.GetLastError();

                log.debug(
                        "Could not query process image name. "
                                + "PID={}, WindowsErrorCode={}",
                        processId,
                        errorCode
                );

                return "";
            }

            String fullPath = new String(
                    executablePath,
                    0,
                    size.getValue()
            );

            int lastSlash = Math.max(
                    fullPath.lastIndexOf('\\'),
                    fullPath.lastIndexOf('/')
            );

            return lastSlash >= 0
                    ? fullPath.substring(lastSlash + 1)
                    : fullPath;

        } catch (RuntimeException e) {
            log.warn(
                    "Error retrieving executable name for PID={}",
                    processId,
                    e
            );

            return "";

        } finally {
            if (!kernel32.CloseHandle(process)) {
                log.debug(
                        "Could not close process handle for PID={}",
                        processId
                );
            }
        }
    }

    private String getApplicationDisplayName(String executableName) {
        if (executableName == null || executableName.isBlank()) {
            return "";
        }

        String normalized =
                executableName.trim().toLowerCase(Locale.ROOT);

        switch (normalized) {
            case "chrome.exe":
                return "Google Chrome";
            case "msedge.exe":
                return "Microsoft Edge";
            case "firefox.exe":
                return "Mozilla Firefox";
            case "opera.exe":
                return "Opera";
            case "brave.exe":
                return "Brave";
            case "idea64.exe":
            case "idea.exe":
                return "IntelliJ IDEA";
            case "code.exe":
                return "Visual Studio Code";
            case "devenv.exe":
                return "Visual Studio";
            case "eclipse.exe":
                return "Eclipse";
            case "notepad.exe":
                return "Notepad";
            case "notepad++.exe":
                return "Notepad++";
            case "explorer.exe":
                return "Windows Explorer";
            case "taskmgr.exe":
                return "Task Manager";
            case "powershell.exe":
                return "Windows PowerShell";
            case "cmd.exe":
                return "Command Prompt";
            case "wt.exe":
                return "Windows Terminal";
            case "vlc.exe":
                return "VLC Media Player";
            case "spotify.exe":
                return "Spotify";
            case "discord.exe":
                return "Discord";
            case "slack.exe":
                return "Slack";
            case "teams.exe":
                return "Microsoft Teams";
            case "winword.exe":
                return "Microsoft Word";
            case "excel.exe":
                return "Microsoft Excel";
            case "powerpnt.exe":
                return "Microsoft PowerPoint";
            case "outlook.exe":
                return "Microsoft Outlook";
            default:
                if (normalized.endsWith(".exe")) {
                    return executableName.substring(
                            0,
                            executableName.length() - 4
                    );
                }

                return executableName;
        }
    }

    /**
     * Holds information about one foreground-window observation.
     */
    public static class ActiveWindowInfo {

        private final String applicationName;
        private final String executableName;
        private final int processId;
        private final String windowTitle;

        public ActiveWindowInfo(
                String applicationName,
                String executableName,
                int processId,
                String windowTitle
        ) {
            this.applicationName = applicationName;
            this.executableName = executableName;
            this.processId = processId;
            this.windowTitle = windowTitle;
        }

        public String getApplicationName() {
            return applicationName;
        }

        public String getExecutableName() {
            return executableName;
        }

        public int getProcessId() {
            return processId;
        }

        public String getWindowTitle() {
            return windowTitle;
        }

        @Override
        public String toString() {
            return "ActiveWindowInfo{" +
                    "applicationName='" + applicationName + '\'' +
                    ", executableName='" + executableName + '\'' +
                    ", processId=" + processId +
                    ", windowTitle='" + windowTitle + '\'' +
                    '}';
        }
    }
}
