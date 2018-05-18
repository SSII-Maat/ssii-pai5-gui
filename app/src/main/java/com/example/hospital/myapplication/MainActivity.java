package com.example.hospital.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.crypto.KeyGenerator;


public class MainActivity extends AppCompatActivity {

    // Setup Server information
    protected static String server = "localhost";
    protected static int port = 4430;

    private static PrivateKey privateKey;

    private static final String privateKeyString = "-----BEGIN PRIVATE KEY-----\n" +
            "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAK0UBlFD5U5bhu1r\n" +
            "BLjYLpAU/cE27iibr8gGs/SLlMWZoErOUMoTb4m4lZs/Q9C6v/7i0bvUZQN/LU63\n" +
            "6fczgeVwe9DT/XCa0FTcXb2R/o5twl2Tdw6CFfNs/l4e8yghsU2ofOm6CNPpibuC\n" +
            "n0ThaZhyU/zCHY5d7sRtzw+o7OJrAgMBAAECgYEAllm13zRu5zHFNUtpL7XERS+m\n" +
            "/AhevPH5snZ4LzBtzXXV9AXj0pctpmKtu84qeBEyphWdgmBQW8hHsIE9gpvA8wSa\n" +
            "3Rz9h7lw4ZFkDrWl7Xs6y9ALKdZ7moS4cjMsWqdzU80sU+hAHRwgudAzx0WLvWvC\n" +
            "hw+5ETy+kdbqf2DZidECQQDTs8XgehkSC0GJIt003NCXfJ0yPJitzCvONeqxB8cx\n" +
            "bpRKd7CsWrUpHEwL6Hwd7pOSpDlpdI2aK4h4MgK1agiDAkEA0UtIrXztLLPG2VhL\n" +
            "DsGmzxAkWJTUla79p8EIIIQITjWtmOPm9IH4XfN8ZqYz75DcCYmOP54cuvfPqM8O\n" +
            "hyoJ+QJAeRzO5qZTc2w3GPJ2JMjzGMc000mxezRkFzvnQVIW1iPR+GxTCbd3Dsbe\n" +
            "hq2BXEph6LHFGpyQahPfpgvOWuUHawJBAMjzC5jnNaGSCv5rs8U7UbnFueADJgmB\n" +
            "trH2uKLfoknVaBQ/3WQt1hX+zhaQxZTi9SGDHT0fxl4NySg/hadpSaECQBbNhUJy\n" +
            "+bdwqRNM3aBDCYc1LJ5c73QnGVynPqjlh4+nd7jlMPQil1vb7NdrQXeMffmnSOKC\n" +
            "iI5NJoG5+hH4vCg=\n" +
            "-----END PRIVATE KEY-----";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_main);

        // Capturamos el boton de Enviar
        View button = findViewById(R.id.button_send);

        // Generación de claves
        try {
            String pkcs8Pem = privateKeyString.toString();
            pkcs8Pem = pkcs8Pem.replace("-----BEGIN PRIVATE KEY-----", "");
            pkcs8Pem = pkcs8Pem.replace("-----END PRIVATE KEY-----", "");
            pkcs8Pem = pkcs8Pem.replaceAll("\\s+","");

            byte [] pkcs8EncodedBytes = Base64.decode(pkcs8Pem, Base64.DEFAULT);

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            privateKey = kf.generatePrivate(keySpec);
        } catch(NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        } catch(InvalidKeySpecException ikse) {
            ikse.printStackTrace();
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
                                    JSONArray data = new JSONArray();

                                    data.put(results.get(0));
                                    data.put(results.get(1));
                                    data.put(results.get(2));
                                    data.put(results.get(3));
                                    data.put(results.get(4));

                                    Log.v("TestApplication", data.toString());

                                    JSONObject result = new JSONObject();

                                    try {
                                        Socket socket = new Socket(InetAddress.getLocalHost(), port);

                                        // Obtenemos el nounce
                                        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

//                                        String nounce = br.readLine();

//                                        data.put("nounce", nounce);

                                        result.put("data", data);

                                        // 2. Firmar los datos

                                        Signature sign = Signature.getInstance("SHA256withRSA");
                                        Log.d("TestApplication", privateKey.toString());
                                        sign.initSign(privateKey);
                                        sign.update(data.toString().getBytes());
                                        byte[] signature = sign.sign();

                                        String signatureString = Base64.encodeToString(signature, Base64.NO_WRAP | Base64.NO_PADDING);

                                        Log.d("TestApplication", signatureString);
                                        result.put("signature", signatureString);

                                        // 3. Enviar los datos

                                        socket.getOutputStream().write(result.toString().getBytes("UTF-8"));
                                        socket.getOutputStream().flush();

                                        socket.close();

                                        // Comprobamos la respuesta:
                                        String res = br.readLine();
                                        if(res.contains("success")) {
                                            Toast.makeText(MainActivity.this, "Petición enviada correctamente", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(MainActivity.this, "Se ha producido un error en la petición", Toast.LENGTH_SHORT).show();
                                        }
                                    } catch(Exception e) {
                                        e.printStackTrace();
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
