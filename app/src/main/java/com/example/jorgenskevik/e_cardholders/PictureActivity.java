package com.example.jorgenskevik.e_cardholders;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.jorgenskevik.e_cardholders.Variables.KVTVariables;
import com.example.jorgenskevik.e_cardholders.models.SessionManager;
import com.example.jorgenskevik.e_cardholders.models.User;
import com.example.jorgenskevik.e_cardholders.remote.UserAPI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * The type Picture activity.
 */
public class PictureActivity extends Activity {
    /**
     * The Image view.
     */
    ImageView imageView;
    /**
     * The Session manager.
     */
    SessionManager sessionManager;
    /**
     * The Bitmap.
     */
    Bitmap bitmap;
    /**
     * The Four digits.
     */
    String fourDigits, /**
     * The Auth token.
     */
    authToken, /**
     * The Code string.
     */
    codeString, /**
     * The Id.
     */
    id, /**
     * The Bearer token.
     */
    bearerToken, /**
     * The Path.
     */
    path;
    /**
     * The User details.
     */
    HashMap<String, String> userDetails;
    /**
     * The Duration.
     */
    int duration, /**
     * The Idx.
     */
    idx;
    /**
     * The Code.
     */
    EditText code;
    /**
     * The Context.
     */
    Context context;
    /**
     * The Toast.
     */
    Toast toast;
    /**
     * The Temporary uri.
     */
    Uri temporaryUri;
    /**
     * The Final file.
     */
    File finalFile, /**
     * The Directory.
     */
    directory, /**
     * The My path.
     */
    myPath;
    /**
     * The User.
     */
    User user;
    /**
     * The Intent.
     */
    Intent intent;
    /**
     * The Bytes.
     */
    ByteArrayOutputStream bytes;
    /**
     * The Cursor.
     */
    Cursor cursor;
    /**
     * The Context wrapper.
     */
    ContextWrapper contextWrapper;
    /**
     * The File output stream.
     */
    FileOutputStream fileOutputStream;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.picture_view);
        imageView = (ImageView) findViewById(R.id.imageView8);
        sessionManager = new SessionManager(getApplicationContext());

        Intent intent = getIntent();

        String mediaPath = intent.getExtras().getString("picture");
        File f = new File(mediaPath);

        Picasso.with(getApplicationContext()).load(f).resize(300,300).centerCrop().into(imageView);
    }

    /**
     * Add picture button.
     *
     * @param v the v
     */

    public void moreInfo(View v){
        context = getApplicationContext();
        duration = Toast.LENGTH_SHORT;
        toast = Toast.makeText(context, R.string.moreInfo, duration);
        toast.show();
    }


    public void addPictureButton(View v) {
        Intent intent = getIntent();
        final String mediaPath = intent.getExtras().getString("picture");
        userDetails = sessionManager.getUserDetails();
        fourDigits = userDetails.get(SessionManager.KEY_PICTURETOKEN);
        authToken = userDetails.get(SessionManager.KEY_TOKEN);
        code = (EditText) findViewById(R.id.editText1);
        codeString = code.getText().toString();

        if (fourDigits.trim().equals("BRUKT")) {
            context = getApplicationContext();
            duration = Toast.LENGTH_SHORT;
            toast = Toast.makeText(context, R.string.DenyPicture, duration);
            toast.show();

        } else if (fourDigits.trim().equals(codeString)) {

            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(KVTVariables.getBaseUrl())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            id = userDetails.get(SessionManager.KEY_ID);
            UserAPI userapi = retrofit.create(UserAPI.class);
            bearerToken = "Bearer " + authToken.toString();
            File file = new File(mediaPath);

            String mimeType = getMimeType(file);

            RequestBody reqFile = RequestBody.create(MediaType.parse(mimeType), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("photo", file.getName(), reqFile);
            RequestBody name = RequestBody.create(MediaType.parse("multipart/form-data"), fourDigits);


            userapi.postPicture(id, bearerToken, KVTVariables.getAcceptVersion(), KVTVariables.getAppkey(), body, name).enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (response.isSuccessful()){
                        user = response.body();
                        sessionManager.updatePicture(user.getPicture());
                        path = mediaPath;
                        String picture = user.getPicture();
                        sessionManager.updatePath(path);
                        sessionManager.updatePictureToken("BRUKT");
                        Intent i = new Intent(PictureActivity.this, UserActivity.class);
                        startActivity(i);
                    }else{
                        context = getApplicationContext();
                        duration = Toast.LENGTH_SHORT;
                        toast = Toast.makeText(context, R.string.updatePicture, duration);
                        toast.show();
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    System.out.println("7");

                    context = getApplicationContext();
                    duration = Toast.LENGTH_SHORT;
                    toast = Toast.makeText(context, R.string.PictureNotUpdated, duration);
                    toast.show();
                }
            });

        } else if (!fourDigits.trim().equals(codeString)) {
            context = getApplicationContext();
            duration = Toast.LENGTH_SHORT;
            toast = Toast.makeText(context, R.string.wrongCode, duration);
            toast.show();
        }
    }

    /**
     * Gets mime type.
     *
     * @param url the url
     * @return the mime type
     */
    public static String getMimeType(File url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(String.valueOf(url));
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }
}
