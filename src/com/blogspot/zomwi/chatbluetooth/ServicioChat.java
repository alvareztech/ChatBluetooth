package com.blogspot.zomwi.chatbluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * Esta clase hace todo el trabajo para la creación y gestión de las conexiones
 * Bluetooth con otros dispositivos. Tiene un hilo que escucha las conexiones
 * entrantes, un hilo para conectar con un dispositivo y un hilo para realizar
 * las transmisiones de datos cuando se conecta.
 */
public class ServicioChat {

	// Nombre para el registro SDP cuando creamos el socket servidor
	private static final String NOMBRE = "ChatBluetooth";

	// Unico UUID para esta aplicacion
	private static final UUID MI_UUID = UUID
			.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

	// Campos miembros
	private final BluetoothAdapter bluetoothAdapter;
	private final Handler controlador;
	private HiloConectar hiloConectar;
	private HiloConectado hiloConectado;
	private HiloAceptar hiloAceptar;
	private int estado;

	// Constantes que indican el estado de la conexion actual
	public static final int ESTADO_NINGUNO = 0; // no estamos haciendo nada
	public static final int ESTADO_ESCUCHANDO = 1; // atentos a las conexiones
													// entrantes
	public static final int ESTADO_CONECTANDO = 2; // ahora se abre una conexion
													// saliente
	public static final int ESTADO_CONECTADO = 3; // ahora esta conectado a un
													// dispositivo remoto

	/**
	 * Constructor. Prepara una nueva sesion BluetoothChat
	 * 
	 * @param context
	 *            El contexto de la Activity de la interfaz de usuario
	 * @param controlador
	 *            Un controlador para enviar mensajes de regreso a la actividad
	 *            de la interfaz de usuario
	 */
	public ServicioChat(Context context, Handler controlador) {
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		estado = ESTADO_NINGUNO;
		this.controlador = controlador;
	}

	/**
	 * Establecer el estado actual de la conexion de chat
	 * 
	 * @param estado
	 *            Un entero que define el estado de la conexion actual
	 */
	private synchronized void setEstado(int estado) {
		this.estado = estado;
		// Dar el nuevo estado a la PrincipalActivity para actualizar
		controlador.obtainMessage(PrincipalActivity.MENSAJE_CAMBIO_ESTADO,
				estado, -1).sendToTarget();
	}

	/**
	 * @return estado de la conexion actual
	 */
	public synchronized int getEstado() {
		return estado;
	}

	/**
	 * Inicia el servicio de chat. En concreto empezara AcceptThread para
	 * iniciar una sesion en modo de escucha. Llamada por el onResume de la
	 * actividad.
	 */
	public synchronized void iniciar() {
		// Cancelando cualquier hilo tratando de hacer conexion
		if (hiloConectar != null) {
			hiloConectar.cancelar();
			hiloConectar = null;
		}
		// Cancelar cualquier subproceso actualmente en ejecucion
		if (hiloConectado != null) {
			hiloConectado.cancelar();
			hiloConectado = null;
		}
		// Iniciar el hilo para escuchar en un BluetoothServerSocket
		if (hiloAceptar != null) {
			hiloAceptar = new HiloAceptar();
			hiloAceptar.start();
		}
		setEstado(ESTADO_ESCUCHANDO);
	}

	public synchronized void conectar(BluetoothDevice dispositivo) {
		// Cancelar cualquier hilo tratando de hacer una conexion
		if (estado == ESTADO_CONECTANDO) {
			if (hiloConectar != null) {
				hiloConectar.cancelar();
				hiloConectar = null;
			}
		}
		// Cancelar cualquier subproceso actualmente en ejecucion de una
		// conexion
		if (hiloConectado != null) {
			hiloConectado.cancelar();
			hiloConectado = null;
		}
		// Iniciar el hilo para conectarse con el dispositivo dado
		hiloConectar = new HiloConectar(dispositivo); // ---------------
		hiloConectar.start();
		setEstado(ESTADO_CONECTANDO);
	}

	/**
	 * Iniciar el ContectandoThread para comenzar a gestionar una conexion
	 * Bluetooth
	 * 
	 * @param socket
	 * @param dispositivo
	 */
	public synchronized void conectado(BluetoothSocket socket,
			BluetoothDevice dispositivo) {
		// Cancelar el hilo que conecta la conexion
		if (hiloConectar != null) {
			hiloConectar.cancelar();
			hiloConectar = null;
		}
		// Cancelar cualquier subproceso actualmente en ejecucion de una
		// conexion
		if (hiloConectado != null) {
			hiloConectado.cancelar();
			hiloConectado = null;
		}
		// Cancelar el hilo de aceptar, porque lo unico que queremos es conectar
		// el dispositivo
		if (hiloAceptar != null) {
			hiloAceptar.cancelar();
			hiloAceptar = null;
		}
		// Iniciar el hilo para administrar la conexion y realizar las
		// transmisiones
		hiloConectado = new HiloConectado(socket); // ------------------
		hiloConectado.start();

		// Enviar el nombre del dispositivo conectado de regreso a la Actividad
		// de interfaz de usuario
		Message mensaje = controlador
				.obtainMessage(PrincipalActivity.MENSAJE_NOMBRE_DISPOSITIVO);
		Bundle bundle = new Bundle();
		bundle.putString(PrincipalActivity.NOMBRE_DISPOSITIVO,
				dispositivo.getName());
		mensaje.setData(bundle);
		controlador.sendMessage(mensaje);
		setEstado(ESTADO_CONECTADO);
	}

	/**
	 * Detener todos los hilos
	 */
	public synchronized void detener() {
		if (hiloConectar != null) {
			hiloConectar.cancelar();
			hiloConectar = null;
		}
		if (hiloConectado != null) {
			hiloConectado.cancelar();
			hiloConectado = null;
		}
		if (hiloAceptar != null) {
			hiloAceptar.cancelar();
			hiloAceptar = null;
		}
		setEstado(ESTADO_NINGUNO);
	}

	/**
	 * Escribir al HiloConectado de una sincronizada
	 * 
	 * @param out
	 *            Los bytes para escribir
	 */
	public void escribir(byte[] out) {
		// Creando un objeto temporal
		HiloConectado r;
		// Sincronizar una copia de HiloConectado
		synchronized (this) {
			if (estado != ESTADO_CONECTADO)
				return;
			r = hiloConectado;
		}
		// Realizar una escritura sincronizada
		r.escribir(out);
	}

	/**
	 * Indica que el intento de conexion fallo y lo notofica a la
	 * PrincipalActivity
	 */
	private void conexionErronea() {
		setEstado(ESTADO_ESCUCHANDO);
		// Enviar mensaje de fallo a la PrincipalActivity
		Message mensaje = controlador
				.obtainMessage(PrincipalActivity.MENSAJE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(PrincipalActivity.TOAST,
				"La conexion al dispositivo fallo");
		mensaje.setData(bundle);
		controlador.sendMessage(mensaje);
	}

	private void conexionPerdida() {
		setEstado(ESTADO_ESCUCHANDO);
		// Enviar mensaje de fallo a la PrincipalActivity
		Message mensaje = controlador
				.obtainMessage(PrincipalActivity.MENSAJE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(PrincipalActivity.TOAST,
				"La conexion al dispositivo ha sido perdida");
		mensaje.setData(bundle);
		controlador.sendMessage(mensaje);
	}

	// ***********************************************************************************
	/**
	 * Este hilo se ejecuta mientras la escucha de las conexiones entrantes. Se
	 * comporta como un cliente al lado del servidor. Se ejecuta hasta que la
	 * conexion es aceptada (o hasta que se cancele).
	 */
	private class HiloAceptar extends Thread {

		private final BluetoothServerSocket serverSocket;

		public HiloAceptar() {
			BluetoothServerSocket temporal = null;
			// Crear un nuevo SocketServer que escucha
			try {
				temporal = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
						NOMBRE, MI_UUID);
			} catch (IOException e) {
			}
			serverSocket = temporal;
		}

		public void run() {
			setName("HiloAceptar");
			BluetoothSocket socket = null;

			// Escucha el socket servidor si no estamos conectamos
			while (estado != ESTADO_CONECTADO) {
				try {
					// Se trata de un bloqueo de llamadas y solo devolvera un
					// conexion con exito o una excepcion.
					socket = serverSocket.accept();
				} catch (IOException e) {
					break;
				}

				// Si la conexion fue aceptada
				if (socket != null) {
					synchronized (ServicioChat.this) {
						switch (estado) {
						case ESTADO_ESCUCHANDO:
						case ESTADO_CONECTANDO:
							// Situacion normal. Iniciar el hilo conectado.
							conectado(socket, socket.getRemoteDevice());
							break;
						case ESTADO_NINGUNO:
						case ESTADO_CONECTADO:
							// No esta listo o ya esta conectado. Terminar nuevo
							// socket.
							try {
								socket.close();
							} catch (IOException e) {
							}
							break;
						}
					}
				}
			}
		}

		public void cancelar() {
			try {
				serverSocket.close();
			} catch (IOException e) {
			}
		}
	}

	// **********************************************************************************
	/**
	 * Este hilo se ejecuta al intentar realizar una conexion de salida con un
	 * dispositivo. Se ejecuta directamente a traves de: la conexion se realiza
	 * correctamente bien o no.
	 */
	private class HiloConectar extends Thread {

		private BluetoothSocket socket;
		private BluetoothDevice dispositivo;

		public HiloConectar(BluetoothDevice dispositivo) {
			this.dispositivo = dispositivo;
			BluetoothSocket temporal = null;
			// Obtener un BluetoothSocket para una conexion con el
			// BluetoothDevice dado.
			try {
				temporal = dispositivo
						.createRfcommSocketToServiceRecord(MI_UUID);
			} catch (IOException ex) {
			}
			socket = temporal;
		}

		public void run() {
			setName("HiloConectar");
			// Siempre cancelar el descubirmiento, ya que se ralentizará la
			// conexión.
			bluetoothAdapter.cancelDiscovery();

			// Establezca una conexion a la BluetoothSocket.
			try {
				// Este es un bloque de llamadas y solo devolvera una conexion
				// con exito o una excepcion
				socket.connect();
			} catch (IOException ex) {
				conexionErronea();
				// Cerrar socket
				try {
					socket.close();
				} catch (IOException ex2) {

				}
				ServicioChat.this.iniciar();
				return;
			}
			// Restablecer el HiloConectar, ya hemos terminado
			synchronized (ServicioChat.this) {
				hiloConectar = null;
			}
			// Iniciar el hilo conectado
			conectado(socket, dispositivo);
		}

		public void cancelar() {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

	// ************************************************************************

	/**
	 * Este hilo se ejecuta durante la conexión con un dispositivo remoto. Se
	 * ocupa de todas las transmisiones entrantes y salientes.
	 */
	private class HiloConectado extends Thread {

		private BluetoothSocket socket;
		private InputStream flujoEntrada;
		private OutputStream flujoSalida;

		public HiloConectado(BluetoothSocket socket) {
			this.socket = socket;
			InputStream flujoEntradaTemporal = null;
			OutputStream flujoSalidaTemporal = null;
			// Obtener los flujos de entrada y salida del BluetoothSocket
			try {
				flujoEntradaTemporal = socket.getInputStream();
				flujoSalidaTemporal = socket.getOutputStream();
			} catch (IOException ex) {

			}
			flujoEntrada = flujoEntradaTemporal;
			flujoSalida = flujoSalidaTemporal;
		}

		public void run() {
			byte[] buffer = new byte[1024];
			int bytes;
			// Seguimos escuchando el InputStream mientras esta conectado
			while (true) {
				try {
					// Leer desde el InputStream
					bytes = flujoEntrada.read(buffer);
					// Enviar los bytes obtenidos a la actividad de interfaz
					// grafica
					controlador.obtainMessage(PrincipalActivity.MENSAJE_LEER, bytes, -1, buffer)
							.sendToTarget();
				} catch (IOException ex) {
					conexionPerdida();
					break;
				}
			}
		}

		/**
		 * Escribir al flujo de salida conectado
		 * @param buffer
		 *            Los bytes para escribir
		 */
		public void escribir(byte[] buffer) {
			try {
				flujoSalida.write(buffer);
				// Compartir el mensaje enviado por la PrincipalActivity
				controlador.obtainMessage(PrincipalActivity.MENSAJE_ESCRIBIR, -1, -1, buffer).sendToTarget();
			} catch (IOException ex) {

			}
		}

		public void cancelar() {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}
}
