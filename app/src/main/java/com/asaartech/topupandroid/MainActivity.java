package com.asaartech.topupandroid;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        
        ///////////////////////////////Asking Permission with user////////////////////////////////
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{
                                Manifest.permission.CAMERA,
                                Manifest.permission.READ_CONTACTS,
                                Manifest.permission.CALL_PHONE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE},
                1);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        tabLayout.setupWithViewPager(mViewPager);

        tabLayout.getTabAt(0).setIcon(R.drawable.ic_sim_card_black_24dp);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_sim_card_black_24dp);



    }
    
    /////////////////////////////////For Permission////////////////////////////////
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "All Permissions Granted!", Toast.LENGTH_SHORT).show();
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.          
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Toast.makeText(this, "Setting Clicked", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /////////////////////Fragments or tabs are displayed from here

    public static class PlaceholderFragment extends Fragment {

        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }


        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            if (getArguments().getInt(ARG_SECTION_NUMBER) == 1) {
                View rootView = inflater.inflate(R.layout.fragment_ntc, container, false);
                return rootView;
            } else {
                View rootView = inflater.inflate(R.layout.fragment_ncell, container, false);
                return rootView;
            }


        }
    }


    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "NTC";
                case 1:
                    return "NCell";
            }
            return null;
        }
    }


    /////////For Click Events///////////////////
    public void btn_clicked(View v) {
        Intent nav = new Intent(this, ScanActivity.class);
        ////////////////////////For Recharge//////////////////////
        if (v.getId() == R.id.ntcRechargeLayout) {
            nav.putExtra("CARRIER", "TopUp NTC");
            startActivity(nav);
            Toast.makeText(this, "NTC Recharge", Toast.LENGTH_SHORT).show();
        } else if (v.getId() == R.id.ncellRechargeLayout) {
            nav.putExtra("CARRIER", "TopUp NCell");
            startActivity(nav);
            Toast.makeText(this, "NCell Recharge Pressed", Toast.LENGTH_LONG).show();
        }

        ////////////////////For Balance inquiry//////////////////////////
        else if (v.getId() == R.id.ntcBalanceLayout) {
            Intent ussdIntent = new Intent(Intent.ACTION_CALL);
            ussdIntent.setData(Uri.parse("tel:*400%23"));
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Permission ERROR!!!", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(ussdIntent);
        } else if(v.getId() == R.id.ncellBalanceLayout){
            Intent ussdIntent = new Intent(Intent.ACTION_CALL);
            ussdIntent.setData(Uri.parse("tel:*101%23"));
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Permission ERROR!!!", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(ussdIntent);

        }

        ///////////////////////////For Balance Transfer/////////////////////////////

        else if(v.getId() == R.id.ntcBalanceTransferLayout){
            Intent tNav = new Intent(this, BalanceTransferActivity.class);
            tNav.putExtra("CARRIER", "NTC Transfer");
            startActivity(tNav);
            Toast.makeText(this, "Transfer", Toast.LENGTH_LONG).show();
        } else {
            Intent tNav = new Intent(this, BalanceTransferActivity.class);
            tNav.putExtra("CARRIER", "NCell Transfer");
            Snackbar.make(v, "Transfer Balance", Snackbar.LENGTH_LONG).show();
            startActivity(tNav);
            //Toast.makeText(this, "Transfer", Toast.LENGTH_LONG).show();

        }
    }

    boolean doubleBackToExitPressedOnce = false;

    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Tap back button once more to exit.", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

}
