package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class AccountLockoutService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private static final long LOCK_DURATION_MINUTES = 15;

    private final UserRepo userRepo;

    public AccountLockoutService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    /**
     * Checks whether the account is currently locked.
     *
     * If the 15-minute lock period has expired,
     * the account is automatically unlocked.
     */
    @Transactional
    public boolean isLocked(User user) {

        if (user == null) {
            return false;
        }

        if (!user.isAccountLocked()) {
            return false;
        }

        LocalDateTime lockedAt = user.getLockedAt();

        /*
         * Safety check.
         *
         * A locked account should always have
         * a lockedAt timestamp.
         */
        if (lockedAt == null) {
            return true;
        }

        LocalDateTime unlockTime =
                lockedAt.plusMinutes(
                        LOCK_DURATION_MINUTES
                );

        /*
         * Lock period has expired.
         */
        if (!LocalDateTime.now().isBefore(unlockTime)) {

            user.setAccountLocked(false);
            user.setFailedLoginAttempts(0);
            user.setLockedAt(null);

            userRepo.save(user);

            return false;
        }

        /*
         * 15-minute lock period is still active.
         */
        return true;
    }

    /**
     * Records a failed login attempt.
     */
    @Transactional
    public void recordFailedLogin(User user) {

        if (user == null) {
            return;
        }

        /*
         * Do not increment attempts while
         * the account is still locked.
         */
        if (isLocked(user)) {
            return;
        }

        Integer currentAttempts =
                user.getFailedLoginAttempts();

        if (currentAttempts == null) {
            currentAttempts = 0;
        }

        int newAttempts =
                currentAttempts + 1;

        user.setFailedLoginAttempts(
                newAttempts
        );

        /*
         * Lock after the 5th failed attempt.
         */
        if (newAttempts >= MAX_FAILED_ATTEMPTS) {

            user.setAccountLocked(true);

            user.setLockedAt(
                    LocalDateTime.now()
            );
        }

        userRepo.save(user);
    }

    /**
     * Records a successful login.
     *
     * Resets the consecutive failed-login counter.
     */
    @Transactional
    public void recordSuccessfulLogin(User user) {

        if (user == null) {
            return;
        }

        user.setFailedLoginAttempts(0);

        userRepo.save(user);
    }

    /**
     * Manually locks an account.
     */
    @Transactional
    public void lockAccount(User user) {

        if (user == null) {
            return;
        }

        user.setAccountLocked(true);

        user.setLockedAt(
                LocalDateTime.now()
        );

        userRepo.save(user);
    }

    /**
     * Manually unlocks an account.
     */
    @Transactional
    public void unlockAccount(User user) {

        if (user == null) {
            return;
        }

        user.setAccountLocked(false);

        user.setFailedLoginAttempts(0);

        user.setLockedAt(null);

        userRepo.save(user);
    }

    public int getMaxFailedAttempts() {
        return MAX_FAILED_ATTEMPTS;
    }

    public long getLockDurationMinutes() {
        return LOCK_DURATION_MINUTES;
    }
}

