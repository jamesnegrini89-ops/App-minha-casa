# Minha Casa Voz — Protótipo Android 0.1

Projeto experimental para Xiaomi 15T (Android 16 / HyperOS).

**Estado atual:** demonstração de reconhecimento de voz. **Não controla a tomada Smart Life/Tuya de verdade.** Não contém credenciais nem integração com a nuvem Tuya.

## Testes da primeira versão

- Iniciar a escuta tocando em **Ativar escuta**, com autorização do microfone.
- Manter um serviço de microfone em primeiro plano com notificação para parar.
- Reconhecer "Ok, minha casa" e "ligue/desligue Luz TV" e responder por síntese de voz.
- Testar o que acontece ao bloquear a tela — o HyperOS pode suspender a captura.

**Atenção:** usa `SpeechRecognizer` para uma prova de conceito. Essa API não foi projetada para escuta contínua. A detecção da frase de ativação precisa de um motor dedicado em versões futuras.

## Como gerar o APK gratuitamente

1. Abrir a aba **Actions** deste repositório.
2. Selecionar **Compilar APK de testes** e executar `Run workflow`, ou observar a execução iniciada pelo envio dos arquivos.
3. Após execução concluída com sucesso, abrir os artefatos e baixar `minha-casa-voz-debug`.
4. Extrair e instalar `app-debug.apk` no Xiaomi, conferindo a origem e as permissões.

A compilação no GitHub ainda precisa ser validada. Instalar aplicativos fora da Play Store requer cuidado.

## Próximas etapas

1. Testar o reconhecimento da frase e a tela bloqueada no Xiaomi.
2. Adotar detector local de palavra-chave em português e otimizar consumo de bateria.
3. Integrar a tomada por meio de API Tuya autorizada, **sem embutir segredos no app**.
4. Testar confiabilidade e compatibilidade com Android 16.

Não promete ativação sempre disponível com a tela apagada, nem substitui por completo o Google Assistente.
