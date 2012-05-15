package com.blogspot.zomwi.chatbluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PrincipalActivity extends Activity {

	// Controles
	private TextView textoTitulo;
	private ListView listaConversacion;
	private EditText cajaTexto;
	private Button botonEnviar;

	// ArrayAdapter para el hilo de la conversacion
	private ArrayAdapter<String> conversacionArrayAdapter;

	private BluetoothAdapter bluetoothAdapter = null;

	private ServicioChat servicioChat = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Configurar el diseño de la ventana
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.titulo_personalizado);

		// Configurar el titulo personalizado
		textoTitulo = (TextView) findViewById(R.id.texto_titulo_izquierda);
		textoTitulo.setText(R.string.app_name);
		textoTitulo = (TextView) findViewById(R.id.texto_titulo_derecha);

		// Obtener el local BluetoothAdapter
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// Si el adaptador es nulo, no es compatible el Bluetooth
		if (bluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth no soportado", Toast.LENGTH_LONG)
					.show();
			finish();
			return;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		// Si el dispositivo no esta habilitado, pedimos que se prenda.
		// configurarChat() sera llamado durante el onActivityResult
		if (!bluetoothAdapter.isEnabled()) {
			Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(intent, 2);
		} else {
			// En otro caso, configuramos una sesion de chat
			if (servicioChat == null) {
				configurarChat();
			}
		}
	}

	private void configurarChat() {
		// Inicializar el ArrayAdapter para el hilo de la conversacion
		conversacionArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.mensaje);
		listaConversacion = (ListView) findViewById(R.id.lista_conversacion);
		listaConversacion.setAdapter(conversacionArrayAdapter);

		// Inicializar la caja de escritura para detectar el teclado
		cajaTexto = (EditText) findViewById(R.id.caja_texto);
		cajaTexto.setOnEditorActionListener(null);

		// Inicializar el boton enviar los eventos
		botonEnviar = (Button) findViewById(R.id.boton_enviar);
		botonEnviar.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {
				// Enviar un mensaje
				TextView view = (TextView) findViewById(R.id.caja_texto);
				String mensaje = view.getText().toString();
				enviarMensaje(mensaje);
			}
		});

	}

	protected void enviarMensaje(String mensaje) {
		// TODO Auto-generated method stub

	}

	// El Handler (controlador) devuelve la informacion del ServicioBluetooth
	private final Handler controlador = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case 1:
				switch (msg.arg1) {
				case 1:

					break;

				default:
					break;
				}
				break;

			case 2:
				break;
			default:
				break;
			}
		}

	};

}