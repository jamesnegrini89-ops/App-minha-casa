package br.com.minhacasa.voz;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;

public class EscutaService extends Service implements RecognitionListener, TextToSpeech.OnInitListener {
    public static final String INICIAR = "br.com.minhacasa.INICIAR";
    public static final String PARAR = "br.com.minhacasa.PARAR";
    private static final String CANAL = "escuta_voz";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SpeechRecognizer reconhecedor;
    private TextToSpeech voz;
    private boolean ativo = false;
    private boolean ouvindo = false;
    private long aguardandoAte = 0;
    private final Runnable proximaEscuta = this::ouvir;

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(CANAL, "Escuta Minha Casa", NotificationManager.IMPORTANCE_LOW));
        }
        voz = new TextToSpeech(this, this);
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && PARAR.equals(intent.getAction())) {
            encerrar();
            return START_NOT_STICKY;
        }
        if (!ativo) {
            ativo = true;
            notificar("Escutando comandos de voz");
            atualizar("Escuta iniciada. Diga 'Ok, minha casa'.");
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                    atualizar("Sem serviço de reconhecimento de voz instalado no Android.");
                    encerrar();
                    return START_NOT_STICKY;
                }
                if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
                    reconhecedor = SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
                } else {
                    reconhecedor = SpeechRecognizer.createSpeechRecognizer(this);
                }
                reconhecedor.setRecognitionListener(this);
                agendar(300);
            } catch (Exception e) {
                atualizar("Falha ao iniciar reconhecimento: " + e.getClass().getSimpleName());
                encerrar();
            }
        }
        return START_NOT_STICKY;
    }
    private void notificar(String msg) {
        Intent parar = new Intent(this, EscutaService.class).setAction(PARAR);
        PendingIntent acao = PendingIntent.getService(this, 0, parar, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder nb = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CANAL) : new Notification.Builder(this);
        Notification notificacao = nb.setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Minha Casa Voz")
            .setContentText(msg)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Parar", acao)
            .build();
        startForeground(10, notificacao);
    }
    private void agendar(long atraso) {
        handler.removeCallbacks(proximaEscuta);
        if (ativo) handler.postDelayed(proximaEscuta, atraso);
    }
    private void ouvir() {
        if (!ativo || ouvindo || reconhecedor == null) return;
        try {
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
            i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
            ouvindo = true;
            reconhecedor.startListening(i);
        } catch (Exception e) {
            ouvindo = false;
            atualizar("Reconhecedor indisponível: " + e.getClass().getSimpleName());
            agendar(2500);
        }
    }
    private String normalizar(String s) {
        return Normalizer.normalize(s.toLowerCase(new Locale("pt", "BR")), Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }
    private void interpretar(String frase) {
        String s = normalizar(frase);
        atualizar("Ouvi: " + frase);
        boolean chamado = s.contains("minha casa") || s.contains("minhacasa");
        if (chamado) aguardandoAte = System.currentTimeMillis() + 12000;
        boolean acordado = System.currentTimeMillis() < aguardandoAte;
        if (!acordado) return;
        boolean alvo = s.contains("luz tv") || s.contains("tv leds") || s.contains("luz da tv") || s.contains("luzes da tv");
        boolean desligar = s.contains("deslig") || s.contains("apag");
        boolean ligar = !desligar && (s.contains("lig") || s.contains("acend"));
        if (alvo && (ligar || desligar)) {
            aguardandoAte = 0;
            String comando = desligar ? "DESLIGAR" : "LIGAR";
            atualizar("DEMONSTRAÇÃO: reconhecido " + comando + " Luz TV. A tomada REAL não foi acionada.");
            falar("Comando " + (desligar ? "desligar" : "ligar") + " reconhecido. Integração da tomada pendente.");
        } else if (chamado) {
            falar("Estou ouvindo.");
        }
    }
    private void falar(String frase) {
        if (voz != null) voz.speak(frase, TextToSpeech.QUEUE_FLUSH, null, "minhacasa-resposta");
    }
    private void atualizar(String texto) {
        getSharedPreferences("voz", MODE_PRIVATE).edit().putBoolean("ativo", ativo).putString("ultimo", texto).apply();
    }
    private void encerrar() {
        ativo = false;
        ouvindo = false;
        handler.removeCallbacks(proximaEscuta);
        if (reconhecedor != null) { reconhecedor.cancel(); reconhecedor.destroy(); reconhecedor = null; }
        atualizar("Escuta parada.");
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }
    @Override public void onDestroy() {
        ativo = false;
        handler.removeCallbacksAndMessages(null);
        if (reconhecedor != null) { reconhecedor.cancel(); reconhecedor.destroy(); reconhecedor = null; }
        if (voz != null) { voz.stop(); voz.shutdown(); voz = null; }
        atualizar("Aplicativo sem escuta ativa.");
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS && voz != null) voz.setLanguage(new Locale("pt", "BR"));
    }
    @Override public void onReadyForSpeech(Bundle params) {}
    @Override public void onBeginningOfSpeech() {}
    @Override public void onRmsChanged(float rmsdB) {}
    @Override public void onBufferReceived(byte[] buffer) {}
    @Override public void onEndOfSpeech() {}
    @Override public void onError(int error) {
        ouvindo = false;
        if (ativo) agendar(1800);
    }
    @Override public void onResults(Bundle results) {
        ouvindo = false;
        ArrayList<String> hipoteses = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (hipoteses != null && !hipoteses.isEmpty()) interpretar(hipoteses.get(0));
        if (ativo) agendar(1200);
    }
    @Override public void onPartialResults(Bundle partialResults) {}
    @Override public void onEvent(int eventType, Bundle params) {}
}
