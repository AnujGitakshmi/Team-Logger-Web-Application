
package com.example.TeamLoggerC.service;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ApplicationEnforcementService {

    private static final Logger log =
            LoggerFactory.getLogger(ApplicationEnforcementService.class);

    private static final int PROCESS_TERMINATE = 0x0001;

    private static final int PROCESS_QUERY_LIMITED_INFORMATION = 0x1000;

    private final Kernel32 kernel32;

    public ApplicationEnforcementService() {
        this.kernel32 = Kernel32.INSTANCE;
    }

    /**
     * Attempts to terminate the exact Windows process identified by PID.
     *
     * Returns true only when Windows reports successful termination.
     * Requires sufficient Windows permissions.
     */
    public boolean blockProcess(int processId) {

        if (processId <= 0) {
            log.warn("Cannot terminate process: invalid PID={}", processId);
            return false;
        }

        // Never allow this service to terminate its own JVM.
        long currentJvmPid = ProcessHandle.current().pid();

        if (processId == currentJvmPid) {
            log.error(
                    "Refusing to terminate TeamLoggerC's own JVM. PID={}",
                    processId
            );
            return false;
        }

        WinNT.HANDLE process = null;

        try {
            process = kernel32.OpenProcess(
                    PROCESS_TERMINATE,
                    false,
                    processId
            );

            if (process == null) {
                int errorCode = kernel32.GetLastError();

                log.error(
                        "OpenProcess failed. PID={}, WindowsErrorCode={}. "
                                + "Check process existence and permissions.",
                        processId,
                        errorCode
                );

                return false;
            }

            boolean terminated = kernel32.TerminateProcess(process, 1);

            if (!terminated) {
                int errorCode = kernel32.GetLastError();

                log.error(
                        "TerminateProcess failed. PID={}, WindowsErrorCode={}",
                        processId,
                        errorCode
                );

                return false;
            }

            log.info(
                    "Windows accepted process termination. PID={}",
                    processId
            );

            return true;

        } catch (RuntimeException e) {
            log.error(
                    "Unexpected error while terminating process PID={}",
                    processId,
                    e
            );

            return false;

        } finally {
            if (process != null) {
                boolean closed = kernel32.CloseHandle(process);

                if (!closed) {
                    int errorCode = kernel32.GetLastError();

                    log.warn(
                            "CloseHandle failed for PID={}, WindowsErrorCode={}",
                            processId,
                            errorCode
                    );
                }
            }
        }
    }

    /**
     * Checks whether Windows allows this service to open the process
     * with limited-information query rights.
     *
     * A false result does not conclusively prove that the process is gone:
     * Windows permissions can also prevent access.
     */
    public boolean processExists(int processId) {

        if (processId <= 0) {
            return false;
        }

        WinNT.HANDLE process = null;

        try {
            process = kernel32.OpenProcess(
                    PROCESS_QUERY_LIMITED_INFORMATION,
                    false,
                    processId
            );

            if (process == null) {
                int errorCode = kernel32.GetLastError();

                log.debug(
                        "Unable to open process for querying. "
                                + "PID={}, WindowsErrorCode={}",
                        processId,
                        errorCode
                );

                return false;
            }

            return true;

        } catch (RuntimeException e) {
            log.warn(
                    "Error checking process access. PID={}",
                    processId,
                    e
            );

            return false;

        } finally {
            if (process != null) {
                boolean closed = kernel32.CloseHandle(process);

                if (!closed) {
                    int errorCode = kernel32.GetLastError();

                    log.debug(
                            "CloseHandle failed after process query. "
                                    + "PID={}, WindowsErrorCode={}",
                            processId,
                            errorCode
                    );
                }
            }
        }
    }
}
