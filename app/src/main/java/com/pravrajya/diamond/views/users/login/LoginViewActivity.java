package com.pravrajya.diamond.views.users.login;

import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;

import android.content.Intent;
import android.os.Bundle;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.fxn.stash.Stash;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.pravrajya.diamond.R;
import com.pravrajya.diamond.databinding.ActivityLoginViewBinding;
import com.pravrajya.diamond.views.BaseActivity;
import com.pravrajya.diamond.views.users.main.views.MainActivity;

import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import com.facebook.AccessToken;

import java.util.Arrays;
import java.util.Objects;

import static com.pravrajya.diamond.utils.Constants.USER_PROFILE;


public class LoginViewActivity extends BaseActivity {

    private ActivityLoginViewBinding loginBinding;

    private GoogleSignInClient mGoogleSignInClient;

    private static final int RC_SIGN_IN = 123;

    private final int FACEBOOK_SIGN_IN  = 64206;

    private CallbackManager callbackManager;

    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loginBinding = DataBindingUtil.setContentView(this, R.layout.activity_login_view);

        Objects.requireNonNull(getSupportActionBar()).setTitle("");
        getSupportActionBar().setHomeAsUpIndicator(getResources().getDrawable(R.drawable.ic_close_black));
        getSupportActionBar().setElevation(0);
        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        callbackManager = CallbackManager.Factory.create();

        socialLogin();
    }



    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
        if (user != null || isLoggedIn) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }




    private void firebaseCreateAccount(String name, String email, String password) {
        showProgressDialog("Login in progress");
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    hideProgressDialog();
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        assert user != null;
                        setUserProfile(user.getUid(), user.getEmail(), name, "");
                    } else {
                        showError(Objects.requireNonNull(task.getException()).getLocalizedMessage());
                    }
                });
    }



    private void socialLogin(){

        loginBinding.btnFirebaseLogin.setOnClickListener(view->{
            String name = loginBinding.etFullName.getText().toString();
            String emailId = loginBinding.etEmail.getText().toString();
            String password=loginBinding.etPassword.getText().toString();

            if (TextUtils.isEmpty(name)){
                showError("Please enter full valid name");
                loginBinding.inputName.setError("Please enter full valid name");
            }else if (!isValidEmail(emailId)){
                showError("Please enter a valid e-mail address");
                loginBinding.inputEmail.setError("Please enter a valid e-mail address");
            }else if(!isValidPassword(password)){
                showError("Password is not Valid");
                loginBinding.inputPassword.setError("Password is not Valid");
            }else{
                firebaseCreateAccount(name,emailId, password);
            }
        });

        loginBinding.googleLoginButton.setOnClickListener(view->{
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

        loginBinding.facebookLoginButton.setReadPermissions(Arrays.asList("email", "public_profile"));
        loginBinding.facebookLoginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }
            @Override
            public void onCancel() {
                showError("Login Canceled");
            }

            @Override
            public void onError(FacebookException error) {
                showError(error.getLocalizedMessage());
            }
        });
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                googleLoginAuth(credential);

            } catch (ApiException e) {
                View parentLayout = findViewById(android.R.id.content);
                Snackbar.make(parentLayout, "Google sign in failed.", Snackbar.LENGTH_SHORT).show();
            }
        }else if (requestCode == FACEBOOK_SIGN_IN) {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }

    }


    private void googleLoginAuth(AuthCredential authCredential) {
        showProgressDialog("Google authenticating...");
        mAuth.signInWithCredential(authCredential).addOnCompleteListener(this, task -> {
            hideProgressDialog();
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user!=null){
                    setUserProfile(user.getUid(), user.getEmail(), user.getDisplayName(), user.getPhotoUrl().toString());
                }
            } else {
                showError(task.getException().getLocalizedMessage());
            }
        });
    }


    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user!=null){
                            setUserProfile(user.getUid(), user.getEmail(), user.getDisplayName(), user.getPhotoUrl().toString());
                        }
                    } else {
                        showError(task.getException().getLocalizedMessage());
                    }
                });
    }



    private void setUserProfile(String uid, String email, String name, String userProfile) {
        Stash.put(USER_PROFILE,new UserProfile(uid, email, name, userProfile));
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }


    private void showError(String errorMessage) {
        View parentLayout = findViewById(android.R.id.content);
        Snackbar.make(parentLayout, errorMessage, Snackbar.LENGTH_SHORT).show();
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAlert() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exit");
        builder.setMessage("Do you want to exit login ?");
        builder.setPositiveButton("Back", (dialog, id) -> finish())
                .setNegativeButton("Cancel", (dialog, which) -> {
                });builder.show();
    }

    @Override
    public void onBackPressed() {
        showAlert();
    }

}