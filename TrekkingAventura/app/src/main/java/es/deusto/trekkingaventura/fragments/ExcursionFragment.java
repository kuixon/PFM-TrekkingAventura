package es.deusto.trekkingaventura.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import es.deusto.trekkingaventura.R;
import es.deusto.trekkingaventura.activities.MainActivity;
import es.deusto.trekkingaventura.entities.Excursion;
import es.deusto.trekkingaventura.entities.InternetAlarm;
import es.deusto.trekkingaventura.entities.OpinionExtendida;
import es.deusto.trekkingaventura.entities.Weather;
import es.deusto.trekkingaventura.imagesAPI.CloudinaryClient;
import es.deusto.trekkingaventura.imagesAPI.PicassoClient;
import es.deusto.trekkingaventura.restDatabaseAPI.RestClientManager;
import es.deusto.trekkingaventura.restDatabaseAPI.RestJSONParserManager;
import es.deusto.trekkingaventura.utilities.InternetAlarmManager;
import es.deusto.trekkingaventura.weatherAPI.JSONWeatherParser;
import es.deusto.trekkingaventura.weatherAPI.WeatherHttpClient;

/**
 * Created by salgu on 15/02/2017.
 */

public class ExcursionFragment extends Fragment {

    public static final String EXCURSION_KEY = "excursion_key";
    public static final String ARG_EXCURSIONES = "excursiones";
    public static final String ARG_OPINIONES_EXTENDIDAS = "opiniones_extendidas";

    private ArrayList<Excursion> arrExcursiones;
    private ArrayList<OpinionExtendida> arrOpinionesExtendidas;
    private Excursion excursion;

    private ImageView imgExc;
    private TextView txtName;
    private TextView txtDescription;
    private TextView txtLocation;
    private TextView txtDistance;
    private ImageView imgLevel;
    private TextView txtLatitude;
    private TextView txtLongitude;

    // Parámetros situación meteorológica
    private TextView cityText;
    private TextView condDescr;
    private TextView temp;
    private TextView press;
    private TextView windSpeed;
    private TextView windDeg;
    private TextView hum;
    private ImageView imgWeather;
    private LinearLayout panelTiempo;
    private LinearLayout panelTiempoNoDisponible;

    private SharedPreferences sharedPref;

    public ExcursionFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Obtenemos el nombre del elemento de la lista seleccionado.
        View rootView = inflater.inflate(R.layout.fragment_excursion, container, false);

        arrOpinionesExtendidas = (ArrayList<OpinionExtendida>) getArguments().getSerializable(ARG_OPINIONES_EXTENDIDAS);
        arrExcursiones = (ArrayList<Excursion>) getArguments().getSerializable(ARG_EXCURSIONES);
        excursion = (Excursion) getArguments().getSerializable(EXCURSION_KEY);

        // Le cambiamos el título a la actividad (al cambiar el título, estaremos llamando
        // a un método de la actividad llamado setTitle.
        getActivity().setTitle(excursion.getName());

        // Ponemos esta opción a true para poder inflar el menu en la Toolbar.
        setHasOptionsMenu(true);

        imgExc = (ImageView) rootView.findViewById(R.id.excImg);
        txtName = (TextView) rootView.findViewById(R.id.excName);
        txtDescription = (TextView) rootView.findViewById(R.id.excDescription);
        txtLocation = (TextView) rootView.findViewById(R.id.excLocation);
        txtDistance = (TextView) rootView.findViewById(R.id.excDistance);
        imgLevel = (ImageView) rootView.findViewById(R.id.excLevel);
        txtLatitude = (TextView) rootView.findViewById(R.id.excLatitude);
        txtLongitude = (TextView) rootView.findViewById(R.id.excLongitude);

        // Parámetros meteorológicos
        cityText = (TextView) rootView.findViewById(R.id.cityText);
        condDescr = (TextView) rootView.findViewById(R.id.condDescr);
        temp = (TextView) rootView.findViewById(R.id.temp);
        hum = (TextView) rootView.findViewById(R.id.hum);
        press = (TextView) rootView.findViewById(R.id.press);
        windSpeed = (TextView) rootView.findViewById(R.id.windSpeed);
        windDeg = (TextView) rootView.findViewById(R.id.windDeg);
        imgWeather = (ImageView) rootView.findViewById(R.id.condIcon);
        panelTiempo = (LinearLayout) rootView.findViewById(R.id.panelTiempo);
        panelTiempoNoDisponible = (LinearLayout) rootView.findViewById(R.id.panelTiempoNoDisponible);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if(excursion.getImgPath() == null || excursion.getImgPath().isEmpty()) {
            imgExc.setImageResource(R.drawable.imgnotavailable);
        } else {
            PicassoClient.downloadImage(getContext(), "http://res.cloudinary.com/trekkingaventura/image/upload/" + excursion.getImgPath(), imgExc);
        }

        txtName.setText(excursion.getName());
        txtDescription.setText(excursion.getOpinion());
        txtLocation.setText(excursion.getLocation());
        txtDistance.setText(Double.toString(excursion.getTravelDistance()) + " km");
        switch (excursion.getLevel()) {
            case "Facil":
                imgLevel.setImageResource(R.drawable.facil);
                break;
            case "Medio":
                imgLevel.setImageResource(R.drawable.medio);
                break;
            case "Dificil":
                imgLevel.setImageResource(R.drawable.dificil);
                break;
        }
        txtLatitude.setText(Float.toString(excursion.getLatitude()));
        txtLongitude.setText(Float.toString(excursion.getLongitude()));

        // Se realiza la petición a la API del tiempo.
        String city = excursion.getLocation();
        String cityWithoutSpaces = city.replace(" ", "%20");
        JSONWeatherTask task = new JSONWeatherTask();
        if (cityWithoutSpaces.contains("%20")) {
            task.execute(new String[]{cityWithoutSpaces});
        } else {
            task.execute(new String[]{city});
        }

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.excursion, menu);

        MenuItem mnuShare = menu.findItem(R.id.mnu_share);

        String shareText = "*" + excursion.getName() + "*\n" + excursion.getOpinion();

        ShareActionProvider shareProv = (ShareActionProvider) MenuItemCompat.getActionProvider(mnuShare);
        shareProv.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareProv.setShareIntent(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.mnu_edit_exc) {
            Fragment fragment = new FormExcursionesFragment();
            Bundle args = new Bundle();
            args.putString(FormExcursionesFragment.ARG_FORM_EXCURSIONES_TITLE, "Formulario");
            args.putString(FormExcursionesFragment.ARG_FORM_EXCURSIONES_SOURCE, "Excursion");
            args.putSerializable(FormExcursionesFragment.FORM_EXCURSION_KEY, excursion);
            args.putSerializable(FormExcursionesFragment.FORM_OPINIONES_EXTENDIDAS, arrOpinionesExtendidas);
            args.putSerializable(FormExcursionesFragment.FORM_EXCURSIONES, arrExcursiones);
            fragment.setArguments(args);
            getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commit();
            return true;
        } else if (id == R.id.mnu_delete_exc) {
            String idExcursionNotification = sharedPref.getString("excursiones", "");
            if (Integer.parseInt(idExcursionNotification) == excursion.getIdExcursion()) {
                showMessageDialog("No puedes eliminar esta excursión porque la tienes seleccionada en el panel de ajustes (Notificaciones)");
            } else {
                EliminarOpinionTask task = new EliminarOpinionTask();
                task.execute(new String[] {Integer.toString(excursion.getIdOpinion())});
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void showMessageDialog(String str) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(str);
        builder.setCancelable(false);
        builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private class JSONWeatherTask extends AsyncTask<String, Void, Weather> {

        @Override
        protected Weather doInBackground(String... params) {
            Weather weather = new Weather();
            String data = ((new WeatherHttpClient()).getWeatherData(params[0]));
            if (data != null) {
                try {
                    weather = JSONWeatherParser.getWeather(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.i("Error", "la API no ha devuelto ningun valor");
                weather = null;
            }

            return weather;
        }

        @Override
        protected void onPostExecute(Weather weather) {
            super.onPostExecute(weather);

            if (weather != null) {
                if (weather.currentCondition.getIcon() != null) {
                    switch (weather.currentCondition.getIcon()) {
                        case "01d":
                            imgWeather.setImageResource(R.drawable.img01d);
                            break;
                        case "01n":
                            imgWeather.setImageResource(R.drawable.img01n);
                            break;
                        case "02d":
                            imgWeather.setImageResource(R.drawable.img02d);
                            break;
                        case "02n":
                            imgWeather.setImageResource(R.drawable.img02n);
                            break;
                        case "03d":
                            imgWeather.setImageResource(R.drawable.img03d);
                            break;
                        case "03n":
                            imgWeather.setImageResource(R.drawable.img03n);
                            break;
                        case "04d":
                            imgWeather.setImageResource(R.drawable.img04d);
                            break;
                        case "04n":
                            imgWeather.setImageResource(R.drawable.img04n);
                            break;
                        case "09d":
                            imgWeather.setImageResource(R.drawable.img09d);
                            break;
                        case "09n":
                            imgWeather.setImageResource(R.drawable.img09n);
                            break;
                        case "10d":
                            imgWeather.setImageResource(R.drawable.img10d);
                            break;
                        case "10n":
                            imgWeather.setImageResource(R.drawable.img10n);
                            break;
                        case "11d":
                            imgWeather.setImageResource(R.drawable.img11d);
                            break;
                        case "11n":
                            imgWeather.setImageResource(R.drawable.img11n);
                            break;
                        case "13d":
                            imgWeather.setImageResource(R.drawable.img13d);
                            break;
                        case "13n":
                            imgWeather.setImageResource(R.drawable.img13n);
                            break;
                        case "50d":
                            imgWeather.setImageResource(R.drawable.img50d);
                            break;
                        case "50n":
                            imgWeather.setImageResource(R.drawable.img50n);
                            break;
                        default:
                            imgWeather.setImageResource(R.drawable.imgnotavailable);
                            break;
                    }
                } else {
                    imgWeather.setImageResource(R.drawable.imgnotavailable);
                }

                cityText.setText(weather.location.getCity() + "," + weather.location.getCountry());
                condDescr.setText(weather.currentCondition.getCondition() + "(" + weather.currentCondition.getDescr() + ")");
                temp.setText("" + Math.round((weather.temperature.getTemp() - 273.15)) + "ºC");
                hum.setText("" + weather.currentCondition.getHumidity() + "%");
                press.setText("" + weather.currentCondition.getPressure() + " hPa");
                windSpeed.setText("" + weather.wind.getSpeed() + " mps");
                if (weather.wind.getDeg() != -1111) {
                    windDeg.setText("" + weather.wind.getDeg() + "º");
                } else {
                    windDeg.setText("");
                }

                panelTiempo.setVisibility(View.VISIBLE);
            } else {
                panelTiempoNoDisponible.setVisibility(View.VISIBLE);
            }
        }
    }

    private class EliminarOpinionTask extends AsyncTask<String, Void, Void> {
        ProgressDialog progressDialog = new ProgressDialog(getActivity());

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog.setTitle("Eliminando excursión...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(String... params) {
            String data = (new RestClientManager()).eliminarOpinion(Integer.parseInt(params[0]));
            if (data != null) {
                // Se ha eliminado correctamente
                Log.i("ELIMINAR_OPINIÓN", "Se ha eliminado la opinión correctamente");
            } else {
                // No se ha eliminado la opinión
                Log.i("ELIMINAR_OPINIÓN", "NO se ha podido eliminar la opinión");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            InicializarExcursionesTask task = new InicializarExcursionesTask();
            task.execute(new String[] {MainActivity.usuario.getIdUsuario()});

            progressDialog.dismiss();
        }
    }

    private class InicializarExcursionesTask extends AsyncTask<String, Void, ArrayList<OpinionExtendida>> {
        ProgressDialog progressDialog = new ProgressDialog(getActivity());

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog.setTitle("Cargando excursiones del usuario...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected ArrayList<OpinionExtendida> doInBackground(String... params) {
            ArrayList<OpinionExtendida> aloe = null;

            String data = ((new RestClientManager()).obtenerOpinionesUsuario(params[0]));
            if (data != null) {
                try {
                    aloe = RestJSONParserManager.getOpinionesExtendidas(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            return aloe;
        }

        @Override
        protected void onPostExecute(ArrayList<OpinionExtendida> aloe) {
            super.onPostExecute(aloe);
            if (aloe != null) {
                // El usuario tiene excursiones
                arrExcursiones = new ArrayList<Excursion>();
                for (OpinionExtendida oe : aloe) {
                    arrExcursiones.add(new Excursion(oe.getIdOpinion(), oe.getExcursion().getIdExcursion(),
                            oe.getExcursion().getNombre(), oe.getOpinion(), oe.getExcursion().getNivel(),
                            oe.getExcursion().getDistancia(), oe.getExcursion().getLugar(), oe.getExcursion().getLatitud(),
                            oe.getExcursion().getLongitud(), oe.getImgPath()));
                }
                arrOpinionesExtendidas = aloe;
            } else {
                // El usuario no tiene excursiones
                arrExcursiones = new ArrayList<Excursion>();
                arrOpinionesExtendidas = new ArrayList<OpinionExtendida>();
            }

            MainActivity.arrExcursiones = arrExcursiones;
            MainActivity.arrOpinionesExtendidas = arrOpinionesExtendidas;

            Fragment fragment = new MisExcursionesFragment();
            Bundle args = new Bundle();
            args.putInt(MisExcursionesFragment.ARG_MIS_EXCURSIONES_NUMBER, 0);
            args.putSerializable(MisExcursionesFragment.ARG_MIS_OPINIONES_EXTENDIDAS, arrOpinionesExtendidas);
            args.putSerializable(MisExcursionesFragment.ARG_MIS_EXCURSIONES, arrExcursiones);
            fragment.setArguments(args);
            getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commit();

            progressDialog.dismiss();
        }
    }
}
