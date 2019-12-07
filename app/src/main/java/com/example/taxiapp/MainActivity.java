package com.example.taxiapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import dmax.dialog.SpotsDialog;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    Button btnSignIn, btnSignUp;

    RelativeLayout parentLayout;

    TextView txt_rider_app;

    FirebaseAuth auth;
    FirebaseDatabase db;
    DatabaseReference users;
    FirebaseUser firebaseUser;


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Arkhip_font.ttf")
                .setFontAttrId(R.attr.fontPath).build());


        txt_rider_app = (TextView) findViewById(R.id.text_raider_app);

        txt_rider_app.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CustomerActivity.class));
            }
        });

        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        users = db.getReference("Users").child("Drivers");

        btnSignIn = (Button) findViewById(R.id.btnSignIn);
        btnSignUp = (Button) findViewById(R.id.btnSignUp);
        parentLayout = (RelativeLayout) findViewById(R.id.parentLayout);

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegisterDialog();
            }
        });

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoginDialog();
            }
        });
    }


    private void showLoginDialog(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("ВОЙТИ");
        dialog.setMessage("Пожалуйста, используйте э-почту и пароль для входа");

        LayoutInflater inflater = LayoutInflater.from(this);
        final View login_layout = inflater.inflate(R.layout.layout_login, null);

        final MaterialEditText edtEmail = login_layout.findViewById(R.id.edt_email2);
        final MaterialEditText edtPassword = login_layout.findViewById(R.id.edt_password2);

        dialog.setView(login_layout);

        dialog.setPositiveButton("ВОЙТИ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                final SpotsDialog waitingDialog = new SpotsDialog(MainActivity.this);
                waitingDialog.show();

                final String txt_email = edtEmail.getText().toString();
                String txt_password = edtPassword.getText().toString();

                if (TextUtils.isEmpty(txt_email)||TextUtils.isEmpty(txt_password)){
                    Snackbar.make(parentLayout, "Пожалуйста, введите э-почту и пароль", Snackbar.LENGTH_LONG).show();
                    return;
                }
                auth.signInWithEmailAndPassword(txt_email, txt_password)
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                waitingDialog.dismiss();

                                DatabaseReference reference = FirebaseDatabase.getInstance()
                                        .getReference("Users")
                                        .child("Drivers");

                                reference.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot snapshot: dataSnapshot.getChildren()){

                                            String email = Objects.requireNonNull(snapshot.child("email").getValue().toString());

                                            if (txt_email.equals(email)){

                                                startActivity(new Intent(MainActivity.this, DriverMap.class));
                                            } else {
                                                Snackbar.make(parentLayout, "Извините, пожалуйста, зарегистрируйтесь как водитель!", Snackbar.LENGTH_LONG).show();
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        Snackbar.make(parentLayout, databaseError.getMessage(), Snackbar.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                waitingDialog.dismiss();
                                Snackbar.make(parentLayout, "АВТОРИЗАЦИЯ НЕ УДАЛАСЬ", Snackbar.LENGTH_LONG).show();
                                return;
                            }
                        });

            }
        });

        dialog.setNegativeButton("ОТМЕНА", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }


    private void showRegisterDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("РЕГИСТРАЦИЯ");
        dialog.setMessage("Пожалуйста, используйте э-почту для регистрации");

        LayoutInflater inflater = LayoutInflater.from(this);
        final View redister_layout = inflater.inflate(R.layout.layout_register, null);

        final MaterialEditText edtEmail = redister_layout.findViewById(R.id.edt_email);
        final MaterialEditText edtPassword = redister_layout.findViewById(R.id.edt_password);
        final MaterialEditText edtName = redister_layout.findViewById(R.id.edt_name);
        final MaterialEditText edtPhone = redister_layout.findViewById(R.id.edt_phone);

        dialog.setView(redister_layout);

        dialog.setPositiveButton("РЕГИСТРАЦИЯ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (TextUtils.isEmpty(edtEmail.getText().toString())){

                    Snackbar.make(parentLayout, "Пожалуйста, введите э-почту", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(edtName.getText().toString())){

                    Snackbar.make(parentLayout, "Пожалуйста, введите ваше имя", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(edtPhone.getText().toString())){

                    Snackbar.make(parentLayout, "Пожалуйста, введите номер телефона", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(edtPassword.getText().toString())){

                    Snackbar.make(parentLayout, "Пожалуйста, введите пароль", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                if (edtPassword.getText().toString().length() < 6){

                    Snackbar.make(parentLayout, "Пожалуйста, введите пароль", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                auth.createUserWithEmailAndPassword(edtEmail.getText().toString(), edtPassword.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                User user = new User(
                                        edtEmail.getText().toString(),
                                        edtPassword.getText().toString(),
                                        edtName.getText().toString(),
                                        edtPhone.getText().toString()
                                );

                                users.child(user.getName())
                                        .setValue(user)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Snackbar.make(parentLayout, "Регистрация прошла успешно!", Snackbar.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Snackbar.make(parentLayout, "Регистрация не удалась: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                            }
                        });
            }
        });

        dialog.setNegativeButton("ОТМЕНА", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}