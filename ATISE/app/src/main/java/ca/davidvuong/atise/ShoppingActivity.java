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
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.integration.android.*;

public class ShoppingActivity extends AppCompatActivity {
    private NfcAdapter nfc;
    private PendingIntent nfcPendingIntent;
    private IntentFilter[] readTagFilters;
    private Tag detectedTag;

    //to be removed
    private Button barcodeButton;

    private ShoppingActivity getInstance() {
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        getSupportActionBar().hide();

        barcodeButton = (Button)findViewById(R.id.tmpBarcodeBtn);
        barcodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Invoke third party app
                IntentIntegrator t = new IntentIntegrator(getInstance());

                t.initiateScan();
            }
        });
        nfc = NfcAdapter.getDefaultAdapter(this);

        if (nfc == null) {
            Toast.makeText(this, "NFC not supported", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!nfc.isEnabled()) {
            Toast.makeText(this, "Enable NFC before using the app", Toast.LENGTH_LONG).show();
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
                String text = new String(payload);
                Toast.makeText(this, text, Toast.LENGTH_LONG).show();

                ndef.close();
            }
        }
        catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Cannot Read From Tag.", Toast.LENGTH_LONG).show();
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

            Toast.makeText(this, scanContent, Toast.LENGTH_LONG).show();
        }
    }
}
