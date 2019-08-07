package com.chegsmania.travelmantics;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chegsmania.travelmantics.utils.FirebaseUtils;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import static com.chegsmania.travelmantics.utils.FirebaseUtils.attachStateListener;
import static com.chegsmania.travelmantics.utils.FirebaseUtils.detachStateListener;
import static com.chegsmania.travelmantics.utils.FirebaseUtils.isAdmin;

public class ListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void init(){
        FirebaseUtils.openFirebaseReference("traveldeals", this);
        RecyclerView recycler = findViewById(R.id.recycler_view);
        DealAdapter adapter = new DealAdapter();
        recycler.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recycler.setLayoutManager(layoutManager);
    }

    public void showMenu(){
        invalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        detachStateListener();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
        attachStateListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.list_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isAdmin){
            menu.findItem(R.id.insert_deal).setVisible(true);
        } else {
            menu.findItem(R.id.insert_deal).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.insert_deal:
                startActivity(new Intent(this, DealActivity.class));
                finish();
                return true;
            case R.id.log_out_deal:
                AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            public void onComplete(@NonNull Task<Void> task) {
                                attachStateListener();
                            }
                        });
                detachStateListener();
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
