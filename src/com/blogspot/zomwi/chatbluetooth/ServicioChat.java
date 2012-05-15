package com.blogspot.zomwi.chatbluetooth;

import java.io.IOException;
import java.util.UUID;

import com.blogspot.zomwi.chatbluetooth.hilos.HiloAceptar;
import com.blogspot.zomwi.chatbluetooth.hilos.HiloConectado;
import com.blogspot.zomwi.chatbluetooth.hilos.HiloConectar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class ServicioChat {

	// Nombre para el registro SDP cuando creamos el socket servidor
	private static final String NOMBRE = "BluetoothChat";

	// Unico UUID para esta aplicacion
	private static final UUID MI_UUID = UUID
			.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

	private BluetoothAdapter bluetoothAdapter;
	private Handler controlador;
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

	private synchronized void setEstado(int estado) {
		this.estado = estado;

		// Dar el nuevo estado
		controlador.obtainMessage(2, estado, -1).sendToTarget(); // **
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
		// Cancelar el hilo aceptar, porque lo unico que queremos es conectar a un dispositivo
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
		// Cancelar cualquier subproceso actualmente en ejecucion de una conexion
		if (hiloConectado != null) {
			hiloConectado.cancelar();
			hiloConectado = null;
		}
		// Iniciar el hilo para conectarse con el dispositivo dado
		hiloConectar = new HiloConectar(dispositivo);
		hiloConectar.start();
		setEstado(ESTADO_CONECTANDO);
	}
	
	/**
	 * Iniciar el ContectandoThread para comenzar a gestionar una conexion Bluetooth
	 * @param socket
	 * @param dispositivo
	 */
	public synchronized void conectando(BluetoothSocket socket, BluetoothDevice dispositivo) {
		// Cancelar el hilo que conecta la conexion
		if (hiloConectar != null ) {
			hiloConectar.cancelar();
			hiloConectar = null;
		}
		// Cancelar cualquier subproceso actualmente en ejecucion de una conexion
		if (hiloConectado != null) {
			hiloConectado.cancelar();
			hiloConectado = null;
		}
		// Cancelar el hilo de aceptar, porque lo unico que queremos es conectar el dispositivo
		if (hiloAceptar != null) {
			hiloAceptar.cancelar();
			hiloAceptar = null;
		}
		// Iniciar el hilo para administrar la conexion y realizar las transmisiones
		hiloConectado = new HiloConectado(socket);
		hiloConectado.start();
		
		// Enviar el nombre del dispositivo conectado de regreso a la Actividad de interfaz de usuario
		Message mensaje = controlador.obtainMessage(2);
		Bundle bundle = new Bundle();
		bundle.putString("nombre", dispositivo.getName());
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

	private class ConectarThread extends Thread {
		
		private BluetoothSocket socket;
		private BluetoothDevice dispositivo;
		
		public ConectarThread(BluetoothDevice dispositivo) {
			this.dispositivo = dispositivo;
			BluetoothSocket tmp = null;
			// Obtener 
			try {
				tmp = dispositivo.createRfcommSocketToServiceRecord(MI_UUID);
			} catch (IOException ex) {
				
			}
			socket = tmp;
		}

		public void run() {
			byte[] buffer = new byte[1024];
			int bytes;
			
			while (true) {
				
			}
		}
		
		public void cancelar() {
			try {
				socket.close();
			} catch (IOException ex) {
				
			}
		}
	}
	
	private class ConectadoThread extends Thread {
		
		public ConectadoThread(BluetoothSocket socket) {
			
		}
		
		public void cancelar() {
			
		}
	}
	
	private class AceptarThread extends Thread {
		public void cancelar() {
			
		}
	}

}
