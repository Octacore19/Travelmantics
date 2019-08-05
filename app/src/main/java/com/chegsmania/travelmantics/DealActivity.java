package com.chegsmania.travelmantics;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
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
import com.squareup.picasso.Picasso;

import static com.chegsmania.travelmantics.utils.FirebaseUtils.isAdmin;
import static com.chegsmania.travelmantics.utils.FirebaseUtils.mStorageReference;

public class DealActivity extends AppCompatActivity {

    private static final int RESULT = 100;
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
                startActivityForResult(intent.createChooser(intent, "Insert Picture"), RESULT);
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
                            String imageName = task.getResult().getStorage().getPath();
                            deal.setImageName(imageName);
                            return ref.getDownloadUrl();
                        } else
                            throw task.getException();
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
        } else {
            mDatabaseReference.child(deal.getDealId()).setValue(deal);
        }
    }

    private void deleteDeal() {
        if (deal != null) {
            mDatabaseReference.child(deal.getDealId()).removeValue();
            Toast.makeText(this, "Deal successfully deleted", Toast.LENGTH_LONG).show();
        }
        if (deal != null && deal.getImageName() != null && !deal.getImageName().isEmpty()) {
            StorageReference reference = mStorageReference.child(deal.getImageName());
            reference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

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
        int width = Resources.getSystem().getDisplayMetrics().widthPixels;
        int height = (width * 2 / 3);
        Picasso.get()
                .load(url)
                /*.resize(width, height)
                .centerCrop()*/
                .into(imageView);

    }
}
