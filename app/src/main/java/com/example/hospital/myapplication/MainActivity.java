package com.example.hospital.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    // Setup Server information
    protected static String server = "192.168.1.133";
    protected static int port = 7070;

    private static KeyPair kp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Capturamos el boton de Enviar
        View button = findViewById(R.id.button_send);

        // Generación de claves
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            kp = kpg.generateKeyPair();
        } catch(NoSuchAlgorithmException nsae) {
            throw new RuntimeException("Algorithm doesn't exists!");
        }

        // Llama al listener del boton Enviar
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });


    }

    // Creación de un cuadro de dialogo para confirmar pedido
    private void showDialog() throws Resources.NotFoundException {
        List<String> inputs = new ArrayList<>(); // 0 - sábanas, 1 - camas, 2 - mesas, 3 - sillas, 4 - sillones
        inputs.add(((EditText) findViewById(R.id.sabanas_input)).getText().toString());
        inputs.add(((EditText) findViewById(R.id.camas_input)).getText().toString());
        inputs.add(((EditText) findViewById(R.id.mesas_input)).getText().toString());
        inputs.add(((EditText) findViewById(R.id.sillas_input)).getText().toString());
        inputs.add(((EditText) findViewById(R.id.sillones_input)).getText().toString());

        boolean errorFound = false;
        String errorMsgs = new String();

        Iterator<String> iterator = inputs.iterator();

        final List<Integer> results = new ArrayList<>();

        while(iterator.hasNext()) {
            String result = iterator.next();
            try {
                Integer value = Integer.parseInt(result);
                if (value >= 0 && value <= 200) {
                    results.add(value);
                } else {
                    throw new NumberFormatException("Number above 500 or below 0");
                }
            } catch (NumberFormatException nfe) {
                errorFound = true;
                String errorMsg = null;
                switch (results.size()) {
                    case 0:
                        errorMsg = "El campo 'sábanas' debe contener un número no decimal entre 1 y 500.\n";
                        break;
                    case 1:
                        errorMsg = "El campo 'camas' debe contener un número no decimal entre 1 y 500.\n";
                        break;
                    case 2:
                        errorMsg = "El campo 'mesas' debe contener un número no decimal entre 1 y 500.\n";
                        break;
                    case 3:
                        errorMsg = "El campo 'sillas' debe contener un número no decimal entre 1 y 500.\n";
                        break;
                    default:
                        errorMsg = "El campo 'sillones' debe contener un número no decimal entre 1 y 500.\n";
                        break;
                }
                results.add(null);
                errorMsgs = errorMsgs.concat(errorMsg);
            }
        }

        if(!errorMsgs.isEmpty()) {
            Toast.makeText(getApplicationContext(), errorMsgs.substring(0, errorMsgs.length() - 1), Toast.LENGTH_SHORT).show();
        }

        if (!errorFound) {
            new AlertDialog.Builder(this)
                    .setTitle("Enviar")
                    .setMessage("Se va a proceder al envio")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                // Catch ok button and send information
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    // 1. Extraer los datos de la vista
                                    JSONObject data = new JSONObject();

                                    try {
                                        data.put("sabanas", results.get(0));
                                        data.put("camas", results.get(1));
                                        data.put("mesas", results.get(2));
                                        data.put("sillas", results.get(3));
                                        data.put("sillones", results.get(4));
                                    } catch(JSONException jsone) {
                                        throw new RuntimeException(jsone.getMessage());
                                    }

                                    Log.v("TestApplication", data.toString());

                                    JSONObject result = new JSONObject();

                                    try {
                                        Socket socket = new Socket(InetAddress.getByName(server), port);

                                        // Obtenemos el nounce
                                        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                                        String nounce = br.readLine();

                                        data.put("nounce", nounce);

                                        result.put("data", data);
                                        result.put("publicKey", Base64.encodeToString(kp.getPublic().getEncoded(), Base64.DEFAULT));

                                        // 2. Firmar los datos

                                        Signature sign = Signature.getInstance("SHA512 with RSA", "SC");
                                        sign.initSign(kp.getPrivate());
                                        sign.update(data.toString().getBytes());
                                        byte[] signature = sign.sign();

                                        result.put("signature", Base64.encodeToString(signature, Base64.DEFAULT));

                                        // 3. Enviar los datos

                                        socket.getOutputStream().write(result.toString().getBytes());
                                        socket.getOutputStream().flush();
                                    } catch(Exception e) {
                                        Log.w("TestApplication", "Excepción: "+e.toString());
                                        Toast.makeText(MainActivity.this, "Fallo en el envío de la petición, no se ha podido conectar con el servidor", Toast.LENGTH_SHORT).show();
                                    }

                                    Toast.makeText(MainActivity.this, "Petición enviada correctamente", Toast.LENGTH_SHORT).show();
                                }
                            }

                    )
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }
    }


}
