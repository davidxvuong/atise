package ca.davidvuong.atise;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.google.zxing.integration.android.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShoppingActivity extends AppCompatActivity implements AsyncResponse {
    private Firebase ref;
    private NfcAdapter nfc;
    private PendingIntent nfcPendingIntent;
    private IntentFilter[] readTagFilters;
    private Tag detectedTag;
    private BraintreeAsyncTask processPayment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping);
        Firebase.setAndroidContext(this);
        ref = new Firebase("https://luminous-fire-9033.firebaseio.com");

        nfc = NfcAdapter.getDefaultAdapter(this);

        if (nfc == null) {
            makeToast("Error: NFC not supported!");
            finish();
            return;
        }

        if (!nfc.isEnabled()) {
            makeToast("Enable NFC before using the app!");
            finish();
            return;
        }

        nfcPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter filter2 = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);

        readTagFilters = new IntentFilter[] {tagDetected, filter2};
    }

    protected void onNewIntent(Intent intent) {
        setIntent(intent);

        if (getIntent().getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            readFromTag(getIntent());
        }
    }

    private void readFromTag(Intent intent) {
        Ndef ndef = Ndef.get(detectedTag);

        try {
            ndef.connect();

            Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if(messages!= null) {
                NdefMessage[] ndefMessages = new NdefMessage[messages.length];

                for (int i = 0; i < messages.length; i++) {
                    ndefMessages[i] = (NdefMessage)messages[i];
                }

                NdefRecord record = ndefMessages[0].getRecords()[0];

                byte[] payload = record.getPayload();
                String barcode = new String(payload);
                if (barcode != null) {
                    barcodeHandler(barcode);
                }
                else {
                    makeToast("Error: Cannot read tag!");
                }


                ndef.close();
            }
        }
        catch (Exception e) {
            makeToast("Error: Cannot read tag!");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        nfc.enableForegroundDispatch(this, nfcPendingIntent, readTagFilters, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode ,data);

        if (result != null) {
            String scanContent = result.getContents();
            barcodeHandler(scanContent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_shopping_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_scan:
                startScan();
                return true;
            case R.id.action_checkout:
                checkoutHandler();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startScan() {
        IntentIntegrator t = new IntentIntegrator(ShoppingActivity.this);
        t.initiateScan();
    }

    private void barcodeHandler(String barcode) {
        final String fBarcode = barcode;

        ValueEventListener tmpListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Map<String, Object> raw = (Map<String, Object>) snapshot.getValue();
                    ref.child("carts").child(ref.getAuth().getUid()).push().setValue(fBarcode);
                }
                else {
                    makeToast("Barcode not found! Please try again.");
                }
            }

            @Override
            public void onCancelled(FirebaseError error) {
                makeToast("Error: Something went wrong with item retrieval.");
            }
        };

        Firebase itemsRef = ref.child("items").child(fBarcode);

        itemsRef.addListenerForSingleValueEvent(tmpListener);
    }

    private void checkoutHandler() {
        Firebase usersRef = ref.child("users").child(ref.getAuth().getUid());

        ValueEventListener tmpListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Map<String, Object> raw = (Map<String, Object>) snapshot.getValue();

                    String creditCardNum = (String) raw.get("ccn");
                    String expDate = (String) raw.get("exp");
                    String cvv = (String) raw.get("ccv");

                    finPriceHandler(creditCardNum, expDate, cvv);
                }
                else {
                    makeToast("Severe panic error: user not found!");
                }
            }

            @Override
            public void onCancelled(FirebaseError error) {
                makeToast("Error: Something went wrong with user retrieval.");
            }
        };

        usersRef.addListenerForSingleValueEvent(tmpListener);
    }

    private void finPriceHandler(String ccn, String exp, String cvv) {
        final String fCredit = ccn;
        final String fExp = exp;
        final String fCVV = cvv;

        Firebase cartRef = ref.child("carts").child(ref.getAuth().getUid());

        ValueEventListener tmpListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Map<String, String> raw = (HashMap<String, String>) snapshot.getValue();
                    List<String> cartItems = new ArrayList<String>();
                    for (String pid : raw.values()) {
                        cartItems.add(pid);
                    }
                    paymentHandler(fCredit, fExp, fCVV, cartItems);
                }
                else {
                    makeToast("You have no items in the cart!");
                }
            }

            @Override
            public void onCancelled(FirebaseError error) {
                makeToast("Error: Something went wrong with cart retrieval.");
            }
        };

        cartRef.addListenerForSingleValueEvent(tmpListener);
    }

    private void paymentHandler(String ccn, String exp, String cvv, List<String> cart) {
        final String fCredit = ccn;
        final String fExp = exp;
        final String fCVV = cvv;
        final List<String> fCart = cart;

        Firebase cartRef = ref.child("items");

        ValueEventListener tmpListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Iterable<DataSnapshot> items = snapshot.getChildren();

                double amount = 0.0;
                for (DataSnapshot item : items) {
                    if (fCart.contains(item.getKey())) {
                        Map<String, Object> vals = (Map<String, Object>) item.getValue();
                        amount += Double.parseDouble(vals.get("price").toString());
                    }
                }

                pay(fCredit, fExp, fCVV, amount);
            }

            @Override
            public void onCancelled(FirebaseError error) {
                makeToast("Error: Something went wrong with cart retrieval.");
            }
        };

        cartRef.addListenerForSingleValueEvent(tmpListener);
    }
    private void pay(String fCredit, String fExp, String fCVV, double amount) {
        makeToast(fCredit + " " + fExp + " " + fCVV + " " + String.valueOf(amount));
        processPayment = new BraintreeAsyncTask(this, fCredit, fExp, fCVV, String.valueOf(amount));
        processPayment.execute();
    }


    private void makeToast (String toast) {
        Toast.makeText(this, toast, Toast.LENGTH_LONG).show();
    }

    @Override
    public void processFinish(boolean result) {
        Intent i;

        if (result == true) {
            //payment process went through. display success activity
            ref.child("carts").child(ref.getAuth().getUid()).removeValue();
            i = new Intent(ShoppingActivity.this, SuccessActivity.class);
            startActivity(i);
            finish();
        }
        else {
            //payment process failed. display failed activity
            i = new Intent(ShoppingActivity.this, FailActivity.class);
            startActivity(i);
            finish();
        }
    }
}
