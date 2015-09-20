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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.*;

import java.text.DecimalFormat;
import java.util.ArrayList;




public class ShoppingActivity extends AppCompatActivity {
    private NfcAdapter nfc;
    private PendingIntent nfcPendingIntent;
    private IntentFilter[] readTagFilters;
    private Tag detectedTag;
    private SimpleAdapter simpleAdpt;



    //to be removed
    private Button barcodeButton;

    private ShoppingActivity getInstance() {
        return this;
    }



    private void SetPrice(double subtotal){

        TextView subtotalView = (TextView) findViewById(R.id.SubtotalValue);
        TextView taxView=(TextView) findViewById(R.id.TaxValue);
        TextView totalView=(TextView) findViewById(R.id.TotalValue);
        TextView totalView2=(TextView) findViewById(R.id.TotalValue2);

        DecimalFormat df = new DecimalFormat("#.00");
        subtotalView.setText("$"+df.format(subtotal));
        taxView.setText("$"+df.format(subtotal*0.13));
        totalView.setText("$"+df.format(subtotal*1.13));
        totalView2.setText("$"+df.format(subtotal*1.13));

        return;
    }

    private void populateListView(){


        // our adapter instance
        //ArrayAdapterItem adapter = new ArrayAdapterItem(this, R.layout.list_view_row_item, ObjectItemData);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping);
        getSupportActionBar().hide();



        ArrayList<Row> initialrows = new ArrayList<Row>();
        initialrows.add(new Row("candy", "$25"));
        initialrows.add(new Row("chocolate", "$30"));

        UsersAdapter adapter = new UsersAdapter(this, initialrows);
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(adapter);

        //Row newUser = new Row("item_name", "$item_cost");
        //adapter.add(newUser);
        SetPrice(1024);
/*
        ArrayList<String> items = new ArrayList<String>();
        items.add("row1");
        items.add("row2");
        items.add("row3");


        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
        ListView listView = (ListView) findViewById(R.id.lvItems);
        listView.setAdapter(itemsAdapter);
*/







        /**
        barcodeButton = (Button) findViewById(R.id.tmpBarcodeBtn);
        barcodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Invoke third party app
                IntentIntegrator t = new IntentIntegrator(getInstance());

                t.initiateScan();
            }
        });
         **/
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

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            String scanContent = result.getContents();

            Toast.makeText(this, scanContent, Toast.LENGTH_LONG).show();
        }
    }
}
