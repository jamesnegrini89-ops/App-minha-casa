package br.com.minhacasa.voz;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int REQ_MICROFONE = 101;
    private TextView status;
    private final Handler atualizador = new Handler(Looper.getMainLooper());
    private boolean visivel;
    private final Runnable atualizar = new Runnable() {
        @Override public void run() {
            if (!visivel) return;
            boolean ativo = getSharedPreferences("voz", MODE_PRIVATE).getBoolean("ativo", false);
            String ultimo = getSharedPreferences("voz", MODE_PRIVATE).getString("ultimo", "Aguardando ativação.");
            status.setText((ativo ? "ESCUTA ATIVA" : "ESCUTA DESLIGADA") + "\n\n" + ultimo);
            atualizador.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout coluna = new LinearLayout(this);
        coluna.setOrientation(LinearLayout.VERTICAL);
        coluna.setPadding(40, 65, 40, 40);
        scroll.addView(coluna);
        setContentView(scroll);

        TextView titulo = texto("Minha Casa Voz", 28, Color.rgb(20, 48, 87));
        titulo.setGravity(Gravity.CENTER_HORIZONTAL);
        coluna.addView(titulo);
        TextView explicacao = texto("Protótipo 0.1 • Xiaomi 15T\nAtive a escuta com o aplicativo aberto. Depois tente bloquear a tela e falar: 'Ok, minha casa'.", 16, Color.DKGRAY);
        explicacao.setPadding(0, 30, 0, 30);
        coluna.addView(explicacao);

        status = texto("Aguardando ativação.", 18, Color.BLACK);
        status.setPadding(20, 25, 20, 25);
        status.setBackgroundColor(Color.rgb(237, 245, 255));
        coluna.addView(status);

        Button ativar = new Button(this);
        ativar.setText("Ativar escuta de voz");
        ativar.setOnClickListener(v -> ativarEscuta());
        coluna.addView(ativar);

        Button desativar = new Button(this);
        desativar.setText("Parar escuta");
        desativar.setOnClickListener(v -> {
            Intent i = new Intent(this, EscutaService.class).setAction(EscutaService.PARAR);
            startService(i);
        });
        coluna.addView(desativar);

        TextView ajuda = texto("COMANDOS DE TESTE\n\n• Ok, minha casa\n• Ligue a Luz TV\n• Desligue a Luz TV\n• Ok, minha casa, desligue a Luz TV\n\nATENÇÃO: esta versão reconhece comandos, mas NÃO controla a tomada real. A integração com Smart Life/Tuya ainda não foi implementada.\n\nA escuta usa o reconhecedor de fala disponível no Android. A operação contínua com a tela bloqueada não é garantida; o serviço pode ser suspenso pelo HyperOS. Não use para comandos de segurança.", 15, Color.DKGRAY);
        ajuda.setPadding(0, 30, 0, 0);
        coluna.addView(ajuda);
    }

    private TextView texto(String s, int tamanho, int cor) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(tamanho);
        v.setTextColor(cor);
        return v;
    }

    private void ativarEscuta() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MICROFONE);
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 102);
        }
        Intent i = new Intent(this, EscutaService.class).setAction(EscutaService.INICIAR);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MICROFONE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ativarEscuta();
        }
    }
    @Override protected void onResume() {
        super.onResume(); visivel = true; atualizador.removeCallbacks(atualizar); atualizador.post(atualizar);
    }
    @Override protected void onPause() {
        visivel = false; atualizador.removeCallbacks(atualizar); super.onPause();
    }
}
