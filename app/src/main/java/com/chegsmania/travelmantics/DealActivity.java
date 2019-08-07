package com.chegsmania.travelmantics;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.chegsmania.travelmantics.model.TravelDeal;
import com.chegsmania.travelmantics.utils.FirebaseUtils;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.Objects;

import static com.chegsmania.travelmantics.utils.FirebaseUtils.isAdmin;
import static com.chegsmania.travelmantics.utils.FirebaseUtils.mStorageReference;

public class DealActivity extends AppCompatActivity {

    private static final int RESULT = 100;
    private static final String LOG_TAG = DealActivity.class.getSimpleName();
    private TextInputEditText dealTitleText, dealPriceText, dealDescriptionText;
    private ImageView imageView;
    private DatabaseReference mDatabaseReference;
    private TravelDeal deal;
    private MaterialButton uploadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDatabaseReference = FirebaseUtils.mDatabaseReference;
        dealTitleText = findViewById(R.id.deal_title_edittext);
        dealPriceText = findViewById(R.id.deal_price_edittext);
        dealDescriptionText = findViewById(R.id.deal_description_edittext);
        imageView = findViewById(R.id.image_logo);
        getDataFromFirebase();

        uploadButton = findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Insert Picture"), RESULT);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT && resultCode == RESULT_OK) {
            if (data != null) {
                Uri imageUri = data.getData();
                final StorageReference ref = mStorageReference.child(imageUri.getLastPathSegment());
                UploadTask uploadTask = ref.putFile(imageUri);
                uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (task.isSuccessful()) {
                            String fullPath = Objects.requireNonNull(task.getResult()).getStorage().getPath();
                            String[] parts = fullPath.split("/");
                            Log.d("Parts Count", String.valueOf(parts.length));
                            String imageName = parts[2];
                            deal.setImageName(imageName);
                            return ref.getDownloadUrl();
                        } else
                            throw Objects.requireNonNull(task.getException());
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()){
                            Uri downloadUri = task.getResult();
                            if (downloadUri != null){
                                String imageUrl = downloadUri.toString();
                                deal.setImageUrl(imageUrl);
                            }
                        }
                    }
                }).addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        showImage(deal.getImageUrl());
                    }
                });
            }
        }
    }

    private void getDataFromFirebase() {
        TravelDeal travelDeal = getIntent().getParcelableExtra("Deal");
        if (travelDeal == null) {
            travelDeal = new TravelDeal();
        }
        deal = travelDeal;
        dealTitleText.setText(deal.getTitle());
        dealPriceText.setText(deal.getPrice());
        dealDescriptionText.setText(deal.getDescription());
        showImage(deal.getImageUrl());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.deal_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isAdmin) {
            menu.findItem(R.id.save_deal).setVisible(true);
            menu.findItem(R.id.delete_deal).setVisible(true);
            uploadButton.setEnabled(true);
            enableEditTexts(true);
        } else {
            menu.findItem(R.id.save_deal).setVisible(false);
            menu.findItem(R.id.delete_deal).setVisible(false);
            uploadButton.setEnabled(false);
            enableEditTexts(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_deal:
                saveDeal();
                startActivity(new Intent(this, ListActivity.class));
                clean();
                finish();
                return true;
            case R.id.delete_deal:
                deleteDeal();
                startActivity(new Intent(this, ListActivity.class));
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void clean() {
        dealTitleText.setText("");
        dealPriceText.setText("");
        dealDescriptionText.setText("");
    }

    private void saveDeal() {
        deal.setTitle(dealTitleText.getEditableText().toString());
        deal.setPrice(dealPriceText.getEditableText().toString());
        deal.setDescription(dealDescriptionText.getEditableText().toString());
        if (deal.getDealId() == null) {
            mDatabaseReference.push().setValue(deal);
            Log.i(LOG_TAG, "New TravelDeal object is created and stored in the database");
        } else {
            mDatabaseReference.child(deal.getDealId()).setValue(deal);
            Log.i(LOG_TAG, "The TravelDeal object" + deal + " has been successfully updated");
        }
    }

    private void deleteDeal() {
        if (deal == null) {
            Toast.makeText(this, "There is nothing to delete", Toast.LENGTH_LONG).show();
            Log.w(LOG_TAG, "There is no object to delete");
        }
        mDatabaseReference.child(deal.getDealId()).removeValue();
        Log.i(LOG_TAG, "Deal object successfully deleted");
        Toast.makeText(getApplicationContext(), "Deal successfully deleted", Toast.LENGTH_LONG).show();

        if (deal != null && deal.getImageName() != null && !deal.getImageName().isEmpty()) {
            StorageReference reference = mStorageReference.child(deal.getImageName());
            reference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.i(LOG_TAG, "Image successfully deleted");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.i(LOG_TAG, "Error: " + e.getMessage());
                }
            });
        }
    }

    private void enableEditTexts(boolean isEnabled) {
        dealTitleText.setEnabled(isEnabled);
        dealPriceText.setEnabled(isEnabled);
        dealDescriptionText.setEnabled(isEnabled);
    }

    private void showImage(String url) {
        final ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        Picasso.get()
                .load(url)
                .into(imageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getApplicationContext(), "Imsge failed to load", Toast.LENGTH_LONG).show();
                    }
                });
    }
}
