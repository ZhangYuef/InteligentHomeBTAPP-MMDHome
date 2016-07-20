package net.bluetoothviewer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import net.bluetoothviewer.library.R;

/**
 * Created by ZYF on 2016/7/17.
 */
public class LoginActivity extends Activity {
    private EditText accountEdit;
    private EditText passwordEdit;
    private Button login;
    private CheckBox rememberPass;

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        accountEdit=(EditText)findViewById(R.id.account);
        passwordEdit=(EditText)findViewById(R.id.password);
        rememberPass=(CheckBox)findViewById(R.id.remember_pass);
        login=(Button)findViewById(R.id.login);

        pref= PreferenceManager.getDefaultSharedPreferences(this);
        boolean isRemember=pref.getBoolean("remember_password",false);
        if(isRemember){
            String account=pref.getString("account","");
            String password=pref.getString("password","");
            accountEdit.setText(account);
            passwordEdit.setText(password);
            rememberPass.setChecked(true);
        }


        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String account =accountEdit.getText().toString();
                String password=passwordEdit.getText().toString();
                //account abc password 123456
                if(account.equals("abc")&&password.equals("123456")){
                    editor=pref.edit();
                    if(rememberPass.isChecked()){
                        editor.putString("account",account);
                        editor.putString("password",password);
                        editor.putBoolean("remember_password",true);
                    }else{
                        editor.clear();
                    }
                    editor.commit();
                    Intent intent=new Intent(LoginActivity.this,BluetoothViewer.class);
                    startActivity(intent);
                    finish();
                }else {
                    Toast.makeText(LoginActivity.this, "WRONGGGGGGGG", Toast.LENGTH_SHORT).show();
                }
            }
        });





    }
}
