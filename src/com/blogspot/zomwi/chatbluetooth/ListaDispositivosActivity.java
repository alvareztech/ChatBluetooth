package com.blogspot.zomwi.chatbluetooth;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Esta Activity se presenta como un cuadro de dialogo. En ella se muestan todos
 * los dispositivos vinculados y los dispositivos detectados en area despues del
 * descubrimiento. Cuando un dispositivo es elegido por el usuario, la direccion
 * MAC del dispositivo se envia de vuelta a la PrincipalActivity
 */
public class ListaDispositivosActivity extends Activity {

	// Retorna un extra del Intent
	public static String EXTRA_DIRECCION_DISPOSITIVO = "direccion_dispositivo";

	// Campos miembro
	private BluetoothAdapter bluetoothAdapter;
	private ArrayAdapter<String> dispositivosVinculadosArrayAdapter;
	private ArrayAdapter<String> nuevosDispositivosArrayAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Configurar la ventana
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS); // barra de
																		// progreso
		setContentView(R.layout.lista_dispositivos);

		// Colocar resultado CANCELADO en caso de que el usuario presione back
		setResult(Activity.RESULT_CANCELED);

		// Iniciar el boton para la busqueda de dispositivos
		Button botonBuscar = (Button) findViewById(R.id.boton_buscar);
		botonBuscar.setOnClickListener(new OnClickListener() {

			public void onClick(View view) {
				descubrirDispositivos();
				view.setVisibility(View.GONE); // oculta el boton
			}
		});

		// Iniciar los ArrayAdapter's de las ListView's. Uno para los
		// dispositivos vinculados y otro para los dispositivos descubiertos.
		dispositivosVinculadosArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.nombre_dispositivo);
		nuevosDispositivosArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.nombre_dispositivo);

		// Buscar y configurar el ListView de dispositivos vinculados
		ListView listaDispositivosVinculados = (ListView) findViewById(R.id.lista_dispositivos_vinculados);
		listaDispositivosVinculados
				.setAdapter(dispositivosVinculadosArrayAdapter);
		listaDispositivosVinculados
				.setOnItemClickListener(dispositivosClickListener);

		// Buscar y configurar el ListView de los nuevos dispositivos
		ListView listaNuevosDispositivos = (ListView) findViewById(R.id.lista_nuevos_dispositivos);
		listaNuevosDispositivos.setAdapter(nuevosDispositivosArrayAdapter);
		listaNuevosDispositivos
				.setOnItemClickListener(dispositivosClickListener);

		// Registrar transmiciones cuando un dispositivo se encuentra
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(receptor, filter);

		// Registra transmiciones cuando el descubrimiento ha terminado
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(receptor, filter);

		// Obtener el local BluetoothAdapter
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// Obtener un conjunto de dispositivos vinculados en ese momento
		Set<BluetoothDevice> dispositivosVinculados = bluetoothAdapter
				.getBondedDevices();

		// Si hay dispositivos vinculados, adicionamos cada uno al ArrayAdapter
		if (dispositivosVinculados.size() > 0) {
			findViewById(R.id.texto_dispositivos_vinculados).setVisibility(
					View.VISIBLE);
			for (BluetoothDevice dispositivo : dispositivosVinculados) {
				dispositivosVinculadosArrayAdapter.add(dispositivo.getName()
						+ "\n" + dispositivo.getAddress());
			}
		} else {
			String noDispositivos = getResources().getText(
					R.string.no_dispositivos).toString();
			dispositivosVinculadosArrayAdapter.add(noDispositivos);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Asegurese de que nosotros estamos haciendo un descubrimiento mas
		if (bluetoothAdapter != null) {
			bluetoothAdapter.cancelDiscovery();
		}
		// Cancelar el registro de los oyentes de las transmisiones
		this.unregisterReceiver(receptor);
	}

	/**
	 * Iniciar un dispositivo descubierto con el BluetoothAdapter
	 */
	private void descubrirDispositivos() {
		// Indicar la exploracion en el titulo
		setProgressBarIndeterminateVisibility(true);
		setTitle(R.string.buscando);

		// Mostrar el titulo para nuevos dispositivos
		findViewById(R.id.texto_nuevos_dispositivos)
				.setVisibility(View.VISIBLE);

		// Si ya estamos descubriendo, lo detenemos
		if (bluetoothAdapter.isDiscovering()) {
			bluetoothAdapter.cancelDiscovery();
		}

		// Empezar el descubrimiento del BluetoothAdapter
		bluetoothAdapter.startDiscovery();
	}

	// El oyente de todos los eventos clic sobre una ListView
	private OnItemClickListener dispositivosClickListener = new OnItemClickListener() {

		public void onItemClick(AdapterView<?> av, View view, int arg2,
				long arg3) {
			// Cancelar el descubrimiento porque es costoso y ademas estamos a
			// punto de conectar
			bluetoothAdapter.cancelDiscovery();

			// Obtenemos la direccion MAC del dispositivo, el cual es lso
			// ultimos 17 caracteres en la View
			String informacion = ((TextView) view).getText().toString();
			String direccion = informacion.substring(informacion.length() - 17);

			// Crear un Intent resultado y incluir la direccion MAC
			Intent intent = new Intent();
			intent.putExtra(EXTRA_DIRECCION_DISPOSITIVO, direccion);

			// Colocar el resultado y finalizar esta Activity
			setResult(Activity.RESULT_OK, intent);
			finish();
		}
	};

	// El BroadcastReceiver que detecta los dispositivos descubiertos y cambia
	// el titulo cuando haya finalizado el descubrimiento
	private final BroadcastReceiver receptor = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String accion = intent.getAction();

			// Cuando se encuentra un dispositivo
			if (BluetoothDevice.ACTION_FOUND.equals(accion)) {
				// Obtener el objeto BluetoothDevice del Intent
				BluetoothDevice dispositivo = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// Si ya esta vinculado, saltar, porque ya se ha listado
				if (dispositivo.getBondState() != BluetoothDevice.BOND_BONDED) {
					nuevosDispositivosArrayAdapter.add(dispositivo.getName()
							+ "\n" + dispositivo.getAddress());
				}
			} else {
				// Cuando el descubrimiento se halla terminado, cambiar el
				// titulo de la Activity
				if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(accion)) {
					setProgressBarIndeterminateVisibility(false);
					setTitle(R.string.seleccione_dispositivo);
					if (nuevosDispositivosArrayAdapter.getCount() == 0) {
						String noDispositivos = getResources().getText(
								R.string.no_dispositivos).toString();
						dispositivosVinculadosArrayAdapter.add(noDispositivos);
					}
				}
			}
		}
	};

}
