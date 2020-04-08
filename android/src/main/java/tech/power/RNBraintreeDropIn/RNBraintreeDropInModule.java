package tech.power.RNBraintreeDropIn;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.braintreepayments.api.dropin.DropInActivity;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.ThreeDSecureInfo;
import com.braintreepayments.api.models.GooglePaymentRequest;
import com.braintreepayments.api.Venmo;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;
import java.lang.Exception;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.exceptions.AppSwitchNotAvailableException;
import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.VenmoAccountNonce;
import com.braintreepayments.api.internal.BraintreeSharedPreferences;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.VenmoAccountBuilder;
public class RNBraintreeDropInModule extends ReactContextBaseJavaModule {
  private Promise mPromise;
  private Activity activity;
  private AppCompatActivity acp;
  private BraintreeFragment mBraintreeFragment;
  private Context appContext;
  private static final int DROP_IN_REQUEST = 0x444;
  private boolean isVerifyingThreeDSecure = false;
  private String mdeviceData;
  private Configuration connectionConfig;
  private Boolean isVenmoReady = false;
  private Boolean isSetupCompleted = false;
  public RNBraintreeDropInModule(ReactApplicationContext reactContext) {
    super(reactContext);
    appContext = reactContext.getApplicationContext();
    reactContext.addActivityEventListener(mActivityListener);
  }


  private void sendStatus(Configuration configuration) {
    WritableMap jsResult = Arguments.createMap();
    jsResult.putBoolean("isVenmoReady", configuration.getPayWithVenmo().isEnabled(appContext));
    System.out.print("CALLING");
    System.out.print(jsResult);
    mPromise.resolve(jsResult);
  }

  private void setVenmoStatus(Configuration configuration) {
    this.isVenmoReady = configuration.getPayWithVenmo().isEnabled(appContext);
  }


  @ReactMethod
  public void getVenmoStatus(final Promise promise) {
    boolean venmoReady = false;

    if(connectionConfig != null){
      venmoReady = connectionConfig.getPayWithVenmo().isEnabled(appContext);
    }
    promise.resolve(venmoReady);
  }

  @ReactMethod 
  public void setup(final ReadableMap options){
    try {
      activity = getCurrentActivity();
      acp = (AppCompatActivity) activity;


      if(mBraintreeFragment == null && connectionConfig == null) {
        mBraintreeFragment =  BraintreeFragment.newInstance(acp, options.getString("clientToken"));
        // mBraintreeFragment.addListener(mConfigListener);
        mBraintreeFragment.addListener(new ConfigurationListener() {
          @Override
          public void onConfigurationFetched(Configuration configuration) {
            connectionConfig = configuration;
            // Have to call sendStatus method with connectionConfig
          }
        });
        mBraintreeFragment.addListener(mNonceListener);
        mBraintreeFragment.addListener(mErrorListener);
        mBraintreeFragment.addListener(mCancelListener);
        DataCollector.collectDeviceData(mBraintreeFragment, new BraintreeResponseListener<String>() {
          @Override
          public void onResponse(String deviceData) {
            // send deviceData to your server
            mdeviceData = deviceData;
          }
        });
      }
      // if(mBraintreeFragment != null && connectionConfig != null ){
      //   sendStatus(connectionConfig);
      // } else {

      //   mBraintreeFragment =  BraintreeFragment.newInstance(acp, options.getString("clientToken"));
      //   // mBraintreeFragment.addListener(mConfigListener);
      //   mBraintreeFragment.addListener(new ConfigurationListener() {
      //     @Override
      //     public void onConfigurationFetched(Configuration configuration) {
      //       connectionConfig = configuration;
      //       sendStatus(connectionConfig);
      //       // Have to call sendStatus method with connectionConfig
      //     }
      //   });
      //   mBraintreeFragment.addListener(mNonceListener);
      //   mBraintreeFragment.addListener(mErrorListener);
      //   mBraintreeFragment.addListener(mCancelListener);
      //   DataCollector.collectDeviceData(mBraintreeFragment, new BraintreeResponseListener<String>() {
      //     @Override
      //     public void onResponse(String deviceData) {
      //       // send deviceData to your server
      //       mdeviceData = deviceData;
      //     }
      //   });

      //     // isVenmoReady = connectionConfig.getPayWithVenmo().isEnabled(appContext);
      //     // if(!venmoReady) {
      //     //   mPromise.reject("APP_NOT_INSTALLED","Venmo is not installed!");
      //     // }
      // }

      // mBraintreeFragment is ready to use!
      // Venmo.authorizeAccount(mBraintreeFragment, false);
      // mPromise.resolve("success");
      // mPromise.resolve("success");

      // mPromise.resolve();
    } catch (Exception e) {
      System.out.print(e.getMessage());
      // mPromise.reject(e.getMessage());
      // There was an issue with your authorization string.
    }
  }
  @ReactMethod
  public void getNonce(final ReadableMap options, final Promise promise) {
    mPromise = promise;
    try {
      // mBraintreeFragment is ready to use!
      Venmo.authorizeAccount(mBraintreeFragment, false);
    } catch (Exception e) {
      System.out.print(e.getMessage());
      mPromise.reject(e.getMessage());
      // There was an issue with your authorization string.
    }
  }
  private final PaymentMethodNonceCreatedListener mNonceListener = new PaymentMethodNonceCreatedListener() {
    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
      // nonceCallback(paymentMethodNonce.getNonce());
      String nonce = paymentMethodNonce.getNonce();
      System.out.print(" onPaymentMethodNonceCreated ");
      System.out.print(nonce);
      mPromise.resolve(nonce);
    }
  };
  private final ConfigurationListener mConfigListener = new ConfigurationListener() {
    @Override
    public void onConfigurationFetched(Configuration configuration) {
      connectionConfig = configuration;
      System.out.print(" ConfigurationListener ");
      boolean venmoReady = configuration.getPayWithVenmo().isEnabled(appContext);
      isVenmoReady = configuration.getPayWithVenmo().isEnabled(appContext);
      System.out.print(venmoReady);
      mPromise.reject("APP_NOT_INSTALLED","Venmo is not installed!");
      // mPromise.resolve(venmoReady);
      // showVenmoButton(venmoReady);
    }
  };
  private final BraintreeErrorListener mErrorListener = new BraintreeErrorListener() {
    @Override
    public void onError(Exception error) {
      System.out.print("BraintreeErrorListener");
        System.out.print(error.getMessage());
      if (error instanceof AppSwitchNotAvailableException) {
        // Braintree is unable to switch to the Venmo app.
        // Append this to your log for troubleshooting.
        System.out.print("AppSwitchNotAvailableException");
        String developerReadableMessage = error.getMessage();
      }
    }
  };
  private final BraintreeCancelListener mCancelListener = new BraintreeCancelListener() {
    @Override
    public void onCancel(int requestCode) {
      System.out.print("BraintreeCancelListener");
      System.out.print(requestCode);
      // if (requestCode == Venmo.VENMO_REQUEST_CODE) {
      //   // Venmo request was canceled by the user, or the user pressed back.
      // }
    }
  };
  @ReactMethod
  public void show(final ReadableMap options, final Promise promise) {
    isVerifyingThreeDSecure = false;
    if (!options.hasKey("clientToken")) {
      promise.reject("NO_CLIENT_TOKEN", "You must provide a client token");
      return;
    }
    Activity currentActivity = getCurrentActivity();
    if (currentActivity == null) {
      promise.reject("NO_ACTIVITY", "There is no current activity");
      return;
    }
    DropInRequest dropInRequest = new DropInRequest().clientToken(options.getString("clientToken"));
    if(options.hasKey("vaultManager")) {
      dropInRequest.vaultManager(options.getBoolean("vaultManager"));
    }
    dropInRequest.collectDeviceData(true);
    if(options.getBoolean("googlePay")){
      GooglePaymentRequest googlePaymentRequest = new GooglePaymentRequest()
        .transactionInfo(TransactionInfo.newBuilder()
          .setTotalPrice(options.getString("orderTotal"))
          .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
          .setCurrencyCode(options.getString("currencyCode"))
          .build())
          .billingAddressRequired(true)
          .googleMerchantId(options.getString("googlePayMerchantId"));
      dropInRequest.googlePaymentRequest(googlePaymentRequest);
    }
    if (options.hasKey("threeDSecure")) {
      final ReadableMap threeDSecureOptions = options.getMap("threeDSecure");
      if (!threeDSecureOptions.hasKey("amount")) {
        promise.reject("NO_3DS_AMOUNT", "You must provide an amount for 3D Secure");
        return;
      }
      isVerifyingThreeDSecure = true;
      dropInRequest
      .amount(String.valueOf(threeDSecureOptions.getDouble("amount")))
      .requestThreeDSecureVerification(true);
    }
    mPromise = promise;
    currentActivity.startActivityForResult(dropInRequest.getIntent(currentActivity), DROP_IN_REQUEST);
  }
  private static boolean shouldVault(Context context) {
    return BraintreeSharedPreferences.getSharedPreferences(context)
            .getBoolean(VAULT_VENMO_KEY, false);
  }
  private final ActivityEventListener mActivityListener = new BaseActivityEventListener() {

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
      super.onActivityResult(requestCode, resultCode, data);
      if (resultCode == Activity.RESULT_OK) {
        Bundle extras = data.getExtras();
        String nonce = extras.getString(EXTRA_PAYMENT_METHOD_NONCE);
        String venmoUsername = extras.getString(EXTRA_USER_NAME);
        // // if (shouldVault(appContext)) {
        //   // vault(mBraintreeFragment, nonce);
        // // } else { 
        //   String venmoUsername = extras.getString(EXTRA_USER_NAME);
          // VenmoAccountNonce venmoAccountNonce = new VenmoAccountNonce(nonce, venmoUsername, venmoUsername);
        //   mBraintreeFragment.postCallback(venmoAccountNonce);
        // }
        resolvePayment(nonce, venmoUsername);
      } else if (resultCode == Activity.RESULT_CANCELED) {
        mPromise.reject("USER_CANCELLATION", "Request cancelled by user");
      } else {
        // Exception exception = (Exception) data.getSerializableExtra(DropInActivity.EXTRA_ERROR);
        // mPromise.reject(exception.getMessage(), exception.getMessage());
      }
      mPromise = null;
    }
  };
  private static final String VAULT_VENMO_KEY = "com.braintreepayments.api.Venmo.VAULT_VENMO_KEY";
  private String EXTRA_PAYMENT_METHOD_NONCE = "com.braintreepayments.api.EXTRA_PAYMENT_METHOD_NONCE";
  private String EXTRA_USER_NAME = "com.braintreepayments.api.EXTRA_USER_NAME";
  // private final PaymentMethodNonce getPaymentMethodeNonce(Bundle extraData) {
  //   WritableMap jsResult = Arguments.createMap();
  //   jsResult.putString("nonce", extraData.getString(EXTRA_PAYMENT_METHOD_NONCE));
  //   jsResult.putString("type", "");
  //   jsResult.putString("description", "");
  //   jsResult.putBoolean("isDefault", true);
  //   jsResult.putString("deviceData", "");
  //   return jsResult;
  // }
  // private final void resolvePayment(PaymentMethodNonce paymentMethodNonce, String deviceData) {
  //   WritableMap jsResult = Arguments.createMap();
  //   jsResult.putString("nonce", paymentMethodNonce.getNonce());
  //   jsResult.putString("type", paymentMethodNonce.getTypeLabel());
  //   jsResult.putString("description", paymentMethodNonce.getDescription());
  //   jsResult.putBoolean("isDefault", paymentMethodNonce.isDefault());
  //   jsResult.putString("deviceData", deviceData);
  //   mPromise.resolve(jsResult);
  // }
  // private static void vault(final BraintreeFragment fragment, String nonce) {
  //   VenmoAccountBuilder vaultBuilder = new VenmoAccountBuilder()
  //           .nonce(nonce);
  //   TokenizationClient.tokenize(fragment, vaultBuilder, new PaymentMethodNonceCallback() {
  //       @Override
  //       public void success(PaymentMethodNonce paymentMethodNonce) {
  //         fragment.postCallback(paymentMethodNonce);
  //         fragment.sendAnalyticsEvent("pay-with-venmo.vault.success");
  //       }
  //       @Override
  //       public void failure(Exception exception) {
  //         fragment.postCallback(exception);
  //         fragment.sendAnalyticsEvent("pay-with-venmo.vault.failed");
  //       }
  //   });
  // }
  private final void resolvePayment(String paymentMethodNonce, String username) {
    WritableMap jsResult = Arguments.createMap();
    jsResult.putString("nonce", paymentMethodNonce);
    jsResult.putString("type", "");
    jsResult.putString("description", "");
    jsResult.putBoolean("isDefault", true);
    jsResult.putString("deviceData", mdeviceData);
    jsResult.putString("userName", username);
    mPromise.resolve(jsResult);
  }
  @Override
  public String getName() {
    return "RNBraintreeDropIn";
  }
}
