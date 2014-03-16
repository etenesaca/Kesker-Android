package com.kemas.fragments;

import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar.OnNavigationListener;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kemas.R;
import com.kemas.datasources.DataSourceAttendance;
import com.kemas.item.adapters.AttendancesItemAdapter;

/*  Fragment para ver las asistencias */
@SuppressLint("NewApi")
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class AttendancesFragment extends Fragment {
	private DataSourceAttendance DataSource;
	private static final int PAGESIZE = 15;
	private View footerView;
	private boolean loading = false;
	private ListAdapter CurrentAdapter;

	private TextView textViewDisplaying;
	private ListView lvAttendance;

	String[] OptionsListNavigation = new String[] { "Todos", "A Tiempo", "Atrazos", "Inasistencias" };
	String[] AttendanceTypes = new String[] { "all", "just_time", "late", "absence" };

	public AttendancesFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.activity_attedances, container, false);

		// Lineas para habilitar el acceso a la red y poder conectarse al
		// servidor de OpenERP en el Hilo Principal
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		ArrayAdapter<String> ActionBarListAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_item, OptionsListNavigation);
		((ActionBarActivity) getActivity()).getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		((ActionBarActivity) getActivity()).getSupportActionBar().setListNavigationCallbacks(ActionBarListAdapter, new OnNavigationListener() {
			@Override
			public boolean onNavigationItemSelected(int arg0, long arg1) {
				// Toast.makeText(getActivity(), "Seleccionada opcion: " +
				// OptionsListNavigation[arg0], Toast.LENGTH_SHORT).show();
				new SearchRegisters(AttendanceTypes[arg0]).execute();
				return false;
			}
		});

		lvAttendance = (ListView) rootView.findViewById(R.id.lvAttendanceList);
		textViewDisplaying = (TextView) rootView.findViewById(R.id.displaying);

		footerView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.footer_list, null, false);
		lvAttendance.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView arg0, int arg1) {
				// nothing here
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				if (load(firstVisibleItem, visibleItemCount, totalItemCount)) {
					loading = true;
					lvAttendance.addFooterView(footerView, null, false);
					(new LoadNextPage()).execute("");
				}
			}
		});

		lvAttendance.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
				Toast.makeText(getActivity(), lvAttendance.getAdapter().getItem(position) + " " + getString(R.string.selected), Toast.LENGTH_SHORT).show();
			}
		});
		return rootView;
	}

	protected void updateDisplayingTextView() {
		String text = getString(R.string.display);
		text = String.format(text, lvAttendance.getCount(), DataSource.getSize());
		textViewDisplaying.setText(text);
	}

	protected boolean load(int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		boolean result = false;
		if (lvAttendance.getAdapter() != null) {
			int aux = firstVisibleItem + visibleItemCount;
			boolean lastItem = aux == totalItemCount && lvAttendance.getChildAt(visibleItemCount - 1) != null && lvAttendance.getChildAt(visibleItemCount - 1).getBottom() <= lvAttendance.getHeight();
			boolean moreRows = lvAttendance.getAdapter().getCount() < DataSource.getSize();
			result = moreRows && lastItem && !loading;
		}
		return result;
	}

	protected class SearchRegisters extends AsyncTask<String, Void, String> {
		ProgressDialog pDialog;
		String AttendancesType;

		public SearchRegisters(String AttendancesType) {
			this.AttendancesType = AttendancesType;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			pDialog = new ProgressDialog(getActivity());
			pDialog.setMessage("Cargando Datos");
			pDialog.setCancelable(true);
			pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pDialog.show();
			lvAttendance.addFooterView(footerView, null, false);
		}

		@Override
		protected String doInBackground(String... params) {
			DataSource = new DataSourceAttendance(getActivity(), this.AttendancesType);
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);

			CurrentAdapter = new AttendancesItemAdapter(getActivity(), DataSource.getData(0, PAGESIZE));
			lvAttendance.setAdapter(CurrentAdapter);
			lvAttendance.removeFooterView(footerView);
			updateDisplayingTextView();
			pDialog.dismiss();
		}

	}

	protected class LoadNextPage extends AsyncTask<String, Void, String> {
		private List<HashMap<String, Object>> newData = null;

		@Override
		protected String doInBackground(String... arg0) {
			newData = DataSource.getData(lvAttendance.getAdapter().getCount() - 1, PAGESIZE);
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			AttendancesItemAdapter adp = (AttendancesItemAdapter) CurrentAdapter;
			for (HashMap<String, Object> value : newData) {
				adp.add(value);
			}
			adp.notifyDataSetChanged();
			CurrentAdapter = (ListAdapter) adp;
			lvAttendance.removeFooterView(footerView);
			updateDisplayingTextView();
			loading = false;
		}
	}

}