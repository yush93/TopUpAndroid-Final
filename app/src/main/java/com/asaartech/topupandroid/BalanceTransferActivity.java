package com.asaartech.topupandroid;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;

public class BalanceTransferActivity extends AppCompatActivity {

    EditText textContact, textCode, textAmount;
    TextView textName;

    private static final String TAG = BalanceTransferActivity.class.getSimpleName();
    private static final int REQUEST_CODE_PICK_CONTACTS = 1;
    private Uri uriContact;
    private String contactID; // contacts unique ID

    String carrier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_balance_transfer);

        carrier = getIntent().getStringExtra("CARRIER");
        setTitle(carrier);

        textName = (TextView) findViewById(R.id.textName);

        textContact = (EditText) findViewById(R.id.textContact);
        textCode = (EditText) findViewById(R.id.textCode);
        textAmount = (EditText) findViewById(R.id.textAmount);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public void btnContact_Clicked(View v) {
        startActivityForResult(new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI), REQUEST_CODE_PICK_CONTACTS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_CONTACTS && resultCode == RESULT_OK) {
            Log.d(TAG, "Response: " + data.toString());
            uriContact = data.getData();

            retrieveContactName();
            retrieveContactNumber();

        }
    }

    private void retrieveContactNumber() {

        String contactNumber = null;

        // getting contacts ID
        Cursor cursorID = getContentResolver().query(uriContact,
                new String[]{ContactsContract.Contacts._ID},
                null, null, null);

        if (cursorID.moveToFirst()) {

            contactID = cursorID.getString(cursorID.getColumnIndex(ContactsContract.Contacts._ID));
        }

        cursorID.close();

        Log.d(TAG, "Contact ID: " + contactID);

        // Using the contact ID now we will get contact phone number
        Cursor cursorPhone = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},

                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " +
                        ContactsContract.CommonDataKinds.Phone.TYPE + " = " +
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,

                new String[]{contactID},
                null);

        if (cursorPhone.moveToFirst()) {
            contactNumber = cursorPhone.getString(cursorPhone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
        }

        cursorPhone.close();

        textContact.setText(contactNumber);

        //Log.d(TAG, "Contact Phone Number: " + contactNumber);
    }

    private void retrieveContactName() {

        String contactName = null;

        // querying contact data store
        Cursor cursor = getContentResolver().query(uriContact, null, null, null, null);

        if (cursor.moveToFirst()) {

            // DISPLAY_NAME = The display name for the contact.
            // HAS_PHONE_NUMBER =   An indicator of whether this contact has at least one phone number.

            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        }

        cursor.close();

        textName.setText(contactName);
        //Log.d(TAG, "Contact Name: " + contactName);

    }

    public void btnTransfer_Clicked(View v) {
        String contact, code, amount;
        String ussd = null;
        if(carrier.equals("NTC Transfer")){
            contact = textContact.getText().toString();
            code = textCode.getText().toString();
            amount = textAmount.getText().toString();
            ussd = "tel:*422*" + code + "*" + contact + "*" + amount + "%23";
        } else if(carrier.equals("NCell Transfer")){
            //*17122*<Type receiver's mobile number>*<Type transfer amount>#.
            contact = textContact.getText().toString();
            amount = textAmount.getText().toString();
            ussd = "tel:*17122*" + contact + "*" + amount + "%23";
        }
        Intent intentTransfer = new Intent(Intent.ACTION_CALL);
        intentTransfer.setData(Uri.parse(ussd));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(BalanceTransferActivity.this, "Permission ERROR!!!", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(intentTransfer);
    }
}
