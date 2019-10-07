package com.hdpolover.ybbproject;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.security.Key;
import java.util.HashMap;

import static android.app.Activity.RESULT_OK;
import static com.google.firebase.storage.FirebaseStorage.getInstance;

public class ProfileFragment extends Fragment {

    //firebase
    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    //storage
    StorageReference storageReference;
    //path where image of user profile and cover will be stored
    String storagePath = "Users_Profile_Cover_Imgs/";

    //views from xml
    ImageView avatarIv, coverIv;
    TextView nameTv, emailTv, phoneTv;
    FloatingActionButton fab;

    //progress dialog
    ProgressDialog progressDialog;

    //permissions constants
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;
    //arrays of permissions to be requsted
    String cameraPermissions[];
    String storagePermissions[];

    //url of picked image
    Uri image_uri;

    //for checking profile of cover photo
    String profileOrCoverPhoto;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {


        //inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        firebaseAuth = firebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        firebaseDatabase = firebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("Users");
        storageReference = getInstance().getReference(); //firebase storage refence

        //init array of permissions
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //init views
        avatarIv = view.findViewById(R.id.avatarIv);
        coverIv = view.findViewById(R.id.coverIv);
        nameTv = view.findViewById(R.id.nameTv);
        emailTv = view.findViewById(R.id.emailTv);
        phoneTv = view.findViewById(R.id.phoneTv);
        fab = view.findViewById(R.id.fab);

        progressDialog = new ProgressDialog(getActivity());

        //we have to get info of currently signed in user
        Query query = databaseReference.orderByChild("email").equalTo(user.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                //check until required data get
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    //get data
                    String name = "" + ds.child("name").getValue();
                    String email = "" + ds.child("email").getValue();
                    String phone = "" + ds.child("phone").getValue();
                    String image = "" + ds.child("image").getValue();
                    String cover = "" + ds.child("cover").getValue();

                    //set data
                    nameTv.setText(name);
                    emailTv.setText(email);
                    phoneTv.setText(phone);

                    try {
                        //if image is received then set
                        Picasso.get().load(image).into(avatarIv);
                    } catch (Exception e) {
                        //if there is any exception while getting image then set default
                        Picasso.get().load(R.drawable.ic_default_img_white).into(avatarIv);
                    }

                    try {
                        //if image is received then set
                        Picasso.get().load(cover).into(coverIv);
                    } catch (Exception e) {
                        //if there is any exception while getting image then set default
//                        Picasso.get().load(R.drawable.ic_default_img_white).into(coverIv);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        
        //fab button click
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEditProfileDialog();
            }
        });

        return view;
    }

    private boolean checkStoragePermission(){
        //check if storage permission is enabled or not
        //return true if enabled
        boolean result = ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission(){
        //request runtime storage permission
        requestPermissions(storagePermissions,STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        //check if storage permission is enabled or not
        //return true if enabled

        boolean result = ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);

        boolean result1 = ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);

        return result && result1;
    }

    private void requestCameraPermission(){
        //request runtime storage permission
        requestPermissions(cameraPermissions,CAMERA_REQUEST_CODE);
    }

    private void showEditProfileDialog() {
        /* show dialog containing options
        1. Edit profile picture
        2. Edit Cover
        3. Edit Name
        4. Edit Phone
         */

        //options to show in dialog
        String options[] = {"Edit Profile Picture","Edit Cover Photo","Edit Name","Edit Phone"};
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //set Title
        builder.setTitle("Choose Action");
        //set items to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //handle dialog item click
                if(i == 0){
                    //edit profile
                    progressDialog.setMessage("Updating Profile Picture");
                    profileOrCoverPhoto = "image";
                    showImagePicDialog();
                    
                }else if(i == 1){
                    //edit cover
                    progressDialog.setMessage("Updating Cover Picture");
                    profileOrCoverPhoto = "cover";
                    showImagePicDialog();

                }else if(i == 2){
                    //edit name
                    progressDialog.setMessage("Updating Name");
                    //calling method and pass key "name" as parameter
                    showNamePhotoUpdateDialog("name");

                }else if(i == 3){
                    //edit phone
                    progressDialog.setMessage("Updating Phone");
                    //calling method and pass key "phone" as parameter
                    showNamePhotoUpdateDialog("phone");

                }
            }
        });
        //create and show dialog
        builder.create().show();
    }

    private void showNamePhotoUpdateDialog(final String keyParam) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Update "+keyParam);
        //set layout of dialog
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10,10,10,10);
        //add edit text
        final EditText editText = new EditText(getActivity());
        editText.setHint("Enter "+keyParam);
        linearLayout.addView(editText);

        builder.setView(linearLayout);

        //add buttons in dialog to update
        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //input text from edit text
                String value = editText.getText().toString().trim();
                //validate if user has entered something

                if(!TextUtils.isEmpty(value)){

                    progressDialog.show();
                    HashMap<String, Object> result = new HashMap<>();
                    result.put(keyParam, value);

                    databaseReference.child(user.getUid()).updateChildren(result)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    //updated
                                    progressDialog.dismiss();
                                    Toast.makeText(getActivity(),"Updated...",Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(getActivity(),""+e.getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    });

                }else {
                     Toast.makeText(getActivity(),"Please enter "+keyParam, Toast.LENGTH_SHORT).show();
                }
            }
        });


        //add buttons in dialog to cancel
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        //Create and show dialog
        builder.create().show();

    }

    private void showImagePicDialog() {
        //show dialog containing options camera and gallery to pick image

        //options to show in dialog
        String options[] = {"Camera","Gallery"};
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //set Title
        builder.setTitle("Pick Image From");
        //set items to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //handle dialog item click
                if(i == 0){
                    //camera

                    if(!checkCameraPermission()){
                        requestCameraPermission();
                    }else{
                        pickFromCamera();
                    }

                }else if(i == 1) {
                    //gallery

                    if(!checkStoragePermission()){
                        requestStoragePermission();
                    }else{
                        pickFromGallery();
                    }
                }
            }
        });
        //create and show dialog
        builder.create().show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                //PICK FROM CAMERA
                if(grantResults.length > 0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if(cameraAccepted && writeStorageAccepted){
                        //permission enabled
                        pickFromCamera();
                    }else{
                        Toast.makeText(getActivity(),"Pleas enable camera & storage permissions",Toast.LENGTH_SHORT).show();
                    }

                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                //PICK FROM GALLERY
                if(grantResults.length > 0){
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if(writeStorageAccepted){
                        //permission enabled
                        pickFromGallery();
                    }else{
                        Toast.makeText(getActivity(),"Pleas enable storage permissions",Toast.LENGTH_SHORT).show();
                    }

                }
            }
            break;
        }
     }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if(requestCode == RESULT_OK){
            if(requestCode == IMAGE_PICK_GALLERY_CODE){
                //image is picked from gallery, get url of image
                image_uri = data.getData();

                uploadProfileCoverPhoto(image_uri);
            }

            if(requestCode == IMAGE_PICK_CAMERA_CODE){
                //image is picked from camera, get url of image

                uploadProfileCoverPhoto(image_uri);

            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadProfileCoverPhoto(Uri uri) {

        //show progress
        progressDialog.show();

        //path and name of image to be stored in firebase storage
        //e.g Users_Profile_Cover_Imgs/image_e112312412.jpg
        String filePathAndName = storagePath+ "" + profileOrCoverPhoto+ "_" +user.getUid();

        StorageReference storageReference2nd = storageReference.child(filePathAndName);
        storageReference2nd.putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        Uri downloadUri = uriTask.getResult();

                        //check if image is uploaded or not and url is received
                        if(uriTask.isSuccessful()){
                            //image uplaoded
                            //add / update url iin user database
                            HashMap<String, Object> results = new HashMap<>();
                            results.put(profileOrCoverPhoto, downloadUri.toString());

                            databaseReference.child(user.getUid()).updateChildren(results)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            //url is database of user is added successfully
                                            progressDialog.dismiss();
                                            Toast.makeText(getActivity(),"Image Updated...",Toast.LENGTH_SHORT).show();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    //error
                                    progressDialog.dismiss();
                                    Toast.makeText(getActivity(),"Error Updating Image...",Toast.LENGTH_SHORT).show();
                                }
                            });
                        }else{
                            //error
                            progressDialog.dismiss();
                            Toast.makeText(getActivity(),"Some error occured",Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //there were some error
                progressDialog.dismiss();
                Toast.makeText(getActivity(),""+e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void pickFromCamera() {
        //intent of picking image from camera
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");
        //put image uri
        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);

        //intent to start camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }

    private void pickFromGallery() {
    //pick from galley
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent,IMAGE_PICK_GALLERY_CODE);
    }

}
