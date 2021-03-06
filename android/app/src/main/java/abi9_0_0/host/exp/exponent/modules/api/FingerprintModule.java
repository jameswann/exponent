// Copyright 2015-present 650 Industries. All rights reserved.

package abi9_0_0.host.exp.exponent.modules.api;

import android.hardware.fingerprint.FingerprintManager;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;

import abi9_0_0.com.facebook.react.bridge.Arguments;
import abi9_0_0.com.facebook.react.bridge.Promise;
import abi9_0_0.com.facebook.react.bridge.ReactApplicationContext;
import abi9_0_0.com.facebook.react.bridge.ReactContextBaseJavaModule;
import abi9_0_0.com.facebook.react.bridge.ReactMethod;
import abi9_0_0.com.facebook.react.bridge.UiThreadUtil;
import abi9_0_0.com.facebook.react.bridge.WritableMap;

import javax.annotation.Nullable;

public class FingerprintModule extends ReactContextBaseJavaModule {
  private final FingerprintManagerCompat mFingerprintManager;
  private @Nullable CancellationSignal mCancellationSignal;
  private @Nullable Promise mPromise;
  private boolean mIsAuthenticating = false;

  private final FingerprintManagerCompat.AuthenticationCallback mAuthenticationCallback =
      new FingerprintManagerCompat.AuthenticationCallback() {
    @Override
    public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
      mIsAuthenticating = false;
      WritableMap successResult = Arguments.createMap();
      successResult.putBoolean("success", true);
      safeResolve(successResult);
    }

    @Override
    public void onAuthenticationFailed() {
      mIsAuthenticating = false;
      WritableMap failResult = Arguments.createMap();
      failResult.putBoolean("success", false);
      failResult.putString("error", "authentication_failed");
      safeResolve(failResult);
      // Failed authentication doesn't stop the authentication process, stop it anyway so it works
      // with the promise API.
      safeCancel();
    }

    @Override
    public void onAuthenticationError(int errMsgId, CharSequence errString) {
      mIsAuthenticating = false;
      WritableMap errorResult = Arguments.createMap();
      errorResult.putBoolean("success", false);
      errorResult.putString("error", convertErrorCode(errMsgId));
      errorResult.putString("message", errString.toString());
      safeResolve(errorResult);
    }

    @Override
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
      mIsAuthenticating = false;
      WritableMap helpResult = Arguments.createMap();
      helpResult.putBoolean("success", false);
      helpResult.putString("error", convertHelpCode(helpMsgId));
      helpResult.putString("message", helpString.toString());
      safeResolve(helpResult);
      // Help doesn't stop the authentication process, stop it anyway so it works with the
      // promise API.
      safeCancel();
    }
  };

  public FingerprintModule(ReactApplicationContext context) {
    super(context);

    mFingerprintManager = FingerprintManagerCompat.from(context);
  }

  @Override
  public String getName() {
    return "ExponentFingerprint";
  }

  @ReactMethod
  public void hasHardwareAsync(Promise promise) {
    boolean hasHardware = mFingerprintManager.isHardwareDetected();
    promise.resolve(hasHardware);
  }

  @ReactMethod
  public void isEnrolledAsync(Promise promise) {
    boolean isEnrolled = mFingerprintManager.hasEnrolledFingerprints();
    promise.resolve(isEnrolled);
  }

  @ReactMethod
  public void authenticateAsync(final Promise promise) {
    // FingerprintManager callbacks are invoked on the main thread so also run this there to avoid
    // having to do locking.
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (mIsAuthenticating) {
          WritableMap cancelResult = Arguments.createMap();
          cancelResult.putBoolean("success", false);
          cancelResult.putString("error", "app_cancel");
          safeResolve(cancelResult);
          mPromise = promise;
          return;
        }

        mIsAuthenticating = true;
        mPromise = promise;
        mCancellationSignal = new CancellationSignal();
        mFingerprintManager.authenticate(null, 0, mCancellationSignal, mAuthenticationCallback, null);
      }
    });
  }

  @ReactMethod
  public void cancelAuthenticate() {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        safeCancel();
      }
    });
  }

  private void safeCancel() {
    if (mCancellationSignal != null) {
      mCancellationSignal.cancel();
      mCancellationSignal = null;
    }
  }

  private void safeResolve(Object result) {
    if (mPromise != null) {
      mPromise.resolve(result);
      mPromise = null;
    }
  }

  private static String convertErrorCode(int code) {
    switch (code) {
      case FingerprintManager.FINGERPRINT_ERROR_CANCELED:
        return "user_cancel";
      case FingerprintManager.FINGERPRINT_ERROR_HW_UNAVAILABLE:
        return "not_available";
      case FingerprintManager.FINGERPRINT_ERROR_LOCKOUT:
        return "lockout";
      case FingerprintManager.FINGERPRINT_ERROR_NO_SPACE:
        return "no_space";
      case FingerprintManager.FINGERPRINT_ERROR_TIMEOUT:
        return "timeout";
      case FingerprintManager.FINGERPRINT_ERROR_UNABLE_TO_PROCESS:
        return "unable_to_process";
      default:
        return "unknown";
    }
  }

  private static String convertHelpCode(int code) {
    switch (code) {
      case FingerprintManager.FINGERPRINT_ACQUIRED_IMAGER_DIRTY:
        return "imager_dirty";
      case FingerprintManager.FINGERPRINT_ACQUIRED_INSUFFICIENT:
        return "insufficient";
      case FingerprintManager.FINGERPRINT_ACQUIRED_PARTIAL:
        return "partial";
      case FingerprintManager.FINGERPRINT_ACQUIRED_TOO_FAST:
        return "too_fast";
      case FingerprintManager.FINGERPRINT_ACQUIRED_TOO_SLOW:
        return "too_slow";
      default:
        return "unknown";
    }
  }
}
