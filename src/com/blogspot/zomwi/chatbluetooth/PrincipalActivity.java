package com.blogspot.zomwi.chatbluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PrincipalActivity extends Activity {

	// Tipos de mensajes enviados desde el controlador ServicioChat
	public static final int MENSAJE_CAMBIO_ESTADO = 1;
	public static final int MENSAJE_LEER = 2;
	public static final int MENSAJE_ESCRIBIR = 3;
	public static final int MENSAJE_NOMBRE_DISPOSITIVO = 4;
	public static final int MENSAJE_TOAST = 5;

	// Nombres de teclas recibidos del controlador ServicioChat
	public static final String NOMBRE_DISPOSITIVO = "nombre_dispositivo";
	public static final String TOAST = "toast";

	// Codigos de peticion del Intent
	private static final int PETICION_CONECTAR_DISPOSITIVO = 1;
	private static final int PETICION_HABILITAR_BLUETOOTH = 2;

	// Controles
	private TextView textoTitulo;
	private ListView listaConversacion;
	private EditText cajaTexto;
	private Button botonEnviar;

	// Nombre del dispositivo conectado
	private String nombreDispositivoConectado = null;
	// ArrayAdapter para el hilo de la conversacion
	private ArrayAdapter<String> conversacionArrayAdapter;
	// StringBuffer para los mensajes salientes
	private StringBuffer stringBuffer;
	// BluetoothAdapter local
	private BluetoothAdapter bluetoothAdapter = null;
	// Objeto ServivioChat
	private ServicioChat servicioChat = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Configurar el diseño de la ventana
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE); // crear un titulo
															// personalizado
		setContentView(R.layout.main); // asignamos un diseño
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.titulo_personalizado); // colocamos el titulo
												// personalizado

		// Configurar el titulo personalizado
		textoTitulo = (TextView) findViewById(R.id.texto_titulo_izquierda);
		textoTitulo.setText(R.string.app_name);
		textoTitulo = (TextView) findViewById(R.id.texto_titulo_derecha);

		// Obtenemos el BluetoothAdapter local
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
		// Si el dispositivo no esta activado, pedimos que se active.
		// configurarChat() sera llamado durante el onActivityResult
		if (!bluetoothAdapter.isEnabled()) {
			Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(intent, PETICION_HABILITAR_BLUETOOTH);
		} else {
			// En otro caso, configuramos una sesion de chat
			if (servicioChat == null) {
				configurarChat();
			}
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		// La razon de esta verificacion en onResume() se debe al caso en
		// el que el Bluetooth no se activa durante el onStart(), porque se hizo
		// una pausa, para permitir que onResume() llame de nuevo al
		// ACTION_REQUEST_ENABLE
		if (servicioChat != null) {
			// Solo si el estado ESTADO_NINGUNO, sabemos que no hemos empezado
			if (servicioChat.getEstado() == ServicioChat.ESTADO_NINGUNO) {
				// Iniciamos el ServicioChat
				servicioChat.iniciar();
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
		cajaTexto.setOnEditorActionListener(oyenteTextView);

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
		// Inicializar el ServicioChat para realizar conexiones Bluetooth
		servicioChat = new ServicioChat(this, controlador);
		// Inicializar el buffer para los mensajes salientes
		stringBuffer = new StringBuffer("");
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Detener el servicio de Bluetooth
		if (servicioChat != null) {
			servicioChat.detener();
		}
	}

	private void asegurarDescubrimiento() {
		if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent intent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(intent);
		}
	}

	/**
	 * Enviar un mensaje
	 * 
	 * @param mensaje
	 *            Una cadena de texto para enviar
	 */
	private void enviarMensaje(String mensaje) {
		// Comprobamos que en realidad estamos conectados, antes de hacer
		// cualquier cosa
		if (servicioChat.getEstado() != ServicioChat.ESTADO_CONECTADO) {
			Toast.makeText(this, R.string.no_conectado_dispositivo,
					Toast.LENGTH_LONG).show();
			return;
		}
		// Comprobamos que hay algo para enviar
		if (mensaje.length() > 0) {
			// Obtenemos los bytes del mensaje y le decimos al ServicioChat que
			// escriba
			byte[] enviar = mensaje.getBytes();
			servicioChat.escribir(enviar);
			// Restablecer el StringBuffer a cero y despejar la caja de texto
			stringBuffer.setLength(0);
			cajaTexto.setText(stringBuffer);
		}
	}

	// El oyente de la accion para el EditText, para escuchar la tecla enter
	private TextView.OnEditorActionListener oyenteTextView = new TextView.OnEditorActionListener() {
		public boolean onEditorAction(TextView view, int actionId,
				KeyEvent event) {
			// Si la accion es una una enter, enviamos el mensaje
			if (actionId == EditorInfo.IME_NULL
					&& event.getAction() == KeyEvent.ACTION_UP) {
				String mensaje = view.getText().toString();
				enviarMensaje(mensaje);
			}
			return true;
		}
	};

	// El Handler (controlador) devuelve la informacion del ServicioBluetooth
	private final Handler controlador = new Handler() {

		@Override
		public void handleMessage(Message mensaje) {
			super.handleMessage(mensaje);
			switch (mensaje.what) {

			case MENSAJE_CAMBIO_ESTADO:
				switch (mensaje.arg1) {
				case ServicioChat.ESTADO_CONECTADO:
					textoTitulo.setText(R.string.conectado);
					textoTitulo.append(nombreDispositivoConectado);
					conversacionArrayAdapter.clear();
					break;

				case ServicioChat.ESTADO_CONECTANDO:
					textoTitulo.setText(R.string.conectando);
					break;
				case ServicioChat.ESTADO_ESCUCHANDO:
				case ServicioChat.ESTADO_NINGUNO:
					textoTitulo.setText(R.string.no_conectado);
					break;
				}
				break;

			case MENSAJE_ESCRIBIR:
				byte[] escribirBuffer = (byte[]) mensaje.obj;
				// Construir una cadena para el buffer
				String mensajeEscribir = new String(escribirBuffer);
				conversacionArrayAdapter.add("Yo:  " + mensajeEscribir);
				break;

			case MENSAJE_LEER:
				byte[] leerBuffer = (byte[]) mensaje.obj;
				// Contruir una cadena desde bytes validos
				String mensajeLeer = new String(leerBuffer, 0, mensaje.arg1);
				conversacionArrayAdapter.add(nombreDispositivoConectado + ":  "
						+ mensajeLeer);
				break;

			case MENSAJE_NOMBRE_DISPOSITIVO:
				// guardar el nombre del dispositivo conectado
				nombreDispositivoConectado = mensaje.getData().getString(
						NOMBRE_DISPOSITIVO);
				Toast.makeText(getApplicationContext(),
						"Conectado a " + nombreDispositivoConectado,
						Toast.LENGTH_LONG).show();
				break;
			case MENSAJE_TOAST:
				Toast.makeText(getApplicationContext(),
						mensaje.getData().getString(TOAST), Toast.LENGTH_LONG)
						.show();
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case PETICION_CONECTAR_DISPOSITIVO:
			// Cuando ListaDispositivosActivity regresa con un dispositivo para
			// conectarse
			if (resultCode == Activity.RESULT_OK) {
				// Obtenemos la direccion MAC del dispositivo
				String direccion = data.getExtras().getString(
						ListaDispositivosActivity.EXTRA_DIRECCION_DISPOSITIVO);
				// Obtenemos el objeto BluetoothDevice
				BluetoothDevice dispositivo = bluetoothAdapter
						.getRemoteDevice(direccion);
				// Intentar conectar al dispositivo
				servicioChat.conectar(dispositivo);
			}
			break;
		case PETICION_HABILITAR_BLUETOOTH:
			// Cuando la peticion para habilitar el Bluetooth retorna
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth esta habilitado, por lo que hay que establecer una
				// sesion chat
				configurarChat();
			} else {
				// El usuario no ha habilitado el Bluetooth, o ha ocurrido un
				// error
				Toast.makeText(this, R.string.bt_no_activado_cerrando,
						Toast.LENGTH_SHORT).show();
				finish();
			}

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.buscar:
			// Inicie la ListaDispositivosActivity para ver los dispositivos y
			// para descubrir
			Intent serverIntent = new Intent(this,
					ListaDispositivosActivity.class);
			startActivityForResult(serverIntent, PETICION_CONECTAR_DISPOSITIVO);
			return true;
		case R.id.descubrible:
			// Asegurando de que el dispositivo sea reconocible para los demas
			asegurarDescubrimiento();
			return true;
		}
		return false;
	}

}