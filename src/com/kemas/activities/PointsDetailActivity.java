package com.kemas.activities;

import java.util.HashMap;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.kemas.Configuration;
import com.kemas.OpenERP;
import com.kemas.R;
import com.kemas.hupernikao;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
@SuppressLint("NewApi")
public class PointsDetailActivity extends ActionBarActivity {
	private Configuration config;
	private long RecordID;
	Context Context = (Context) this;

	private TextView lblTitleUser;

	private TextView tvUser;
	private ImageView ivUser;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_points_detail);

		// Lineas para habilitar el acceso a la red y poder conectarse al
		// servidor de OpenERP en el Hilo Principal
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		// Activar el Boton Home
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Crear una instancia de la Clase de Configuraciones
		config = new Configuration(this);

		Bundle bundle = getIntent().getExtras();
		RecordID = bundle.getLong("ID");

		Typeface Roboto_bold = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Bold.ttf");
		lblTitleUser = (TextView) findViewById(R.id.lblTitleUser);
		lblTitleUser.setTypeface(Roboto_bold);

		ivUser = (ImageView) findViewById(R.id.ivUser);
		tvUser = (TextView) findViewById(R.id.tvUser);

		// Ejecutar la Carga de Datos
		((ActionBarActivity) PointsDetailActivity.this).getSupportActionBar().setTitle("Historial de Puntos");

		LoadInfo Task = new LoadInfo();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			Task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			Task.execute();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			// Reggresar al activity de registro de asistencias
			finish();
		}

		return true;
	}

	/** Clase Asincrona para recuparar los datos del registro de Puntaje **/
	protected class LoadInfo extends AsyncTask<String, Void, String> {
		ProgressDialog pDialog;
		HashMap<String, Object> PointsDetail = null;

		public LoadInfo() {
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			pDialog = new ProgressDialog(PointsDetailActivity.this);
			pDialog.setMessage("Cargando Datos");
			pDialog.setCancelable(false);
			pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pDialog.show();
		}

		@Override
		protected String doInBackground(String... params) {
			int Port = Integer.parseInt(config.getPort().toString());
			String Server = config.getServer().toString();

			boolean TestConnection = OpenERP.TestConnection(Server, Port);
			if (TestConnection) {
				OpenERP oerp = hupernikao.BuildOpenERPConnection(config);
				PointsDetail = oerp.read("kemas.history.points", RecordID, new String[] { "code", "date", "reg_uid", "attendance_id", "type", "description", "summary", "points" });

				Object[] reg_uid = (Object[]) PointsDetail.get("reg_uid");
				PointsDetail.put("NameUser", reg_uid[1].toString());
				PointsDetail.put("UserID", reg_uid[0].toString());
				PointsDetail.remove("reg_uid");
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);

			((ActionBarActivity) PointsDetailActivity.this).getSupportActionBar().setSubtitle(PointsDetail.get("code").toString());
			tvUser.setText(PointsDetail.get("NameUser").toString());

			pDialog.dismiss();
			LoadUserImage Task = new LoadUserImage(Long.parseLong(PointsDetail.get("UserID").toString()));
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				Task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				Task.execute();
			}
		}
	}

	/**
	 * Clase Asincrona para recuparar la foto del Usuario que realizo la
	 * modificacion de los puntos
	 **/
	protected class LoadUserImage extends AsyncTask<String, Void, String> {
		HashMap<String, Object> User = null;
		long UserID;

		public LoadUserImage(long UserID) {
			this.UserID = UserID;
		}

		@Override
		protected String doInBackground(String... params) {
			OpenERP oerp = hupernikao.BuildOpenERPConnection(config);
			User = oerp.read("res.users", this.UserID, new String[] { "image_small" });
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);

			// Cargar la imagen del usuario
			byte[] logo = Base64.decode(User.get("image_small").toString(), Base64.DEFAULT);
			Bitmap bmp = BitmapFactory.decodeByteArray(logo, 0, logo.length);
			ivUser.setImageBitmap(hupernikao.getRoundedCornerBitmapSimple(bmp));
		}
	}
}
