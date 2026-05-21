# WTC Connecta — App Android

Aplicativo Android nativo do projeto WTC Connecta, desenvolvido em Kotlin com Jetpack Compose.
Conecta clientes corporativos ao World Trade Center São Paulo, permitindo comunicação em tempo real,
gestão de atendimentos, campanhas e grupos.


---

## Tecnologias

| Camada             | Tecnologia                     |
|--------------------|--------------------------------|
| Linguagem          | Kotlin 2.0.21                  |
| UI                 | Jetpack Compose + Material 3   |
| Navegação          | Navigation Compose 2.8         |
| HTTP               | Retrofit 2.11 + OkHttp         |
| Serialização       | Gson                           |
| Push Notifications | Firebase Cloud Messaging (FCM) |
| Imagens            | Coil                           |
| Arquitetura        | MVVM (ViewModel + Repository)  |

---

## Pré-requisitos

- Android Studio Hedgehog ou superior
- JDK 11+
- Dispositivo ou emulador com Android 7.0+ (API 24+)
- Backend WTC Connecta rodando (ver repositório do backend)
- Arquivo `google-services.json` configurado no Firebase

---

## Configuração

### 1. Clonar o repositório

```bash
git clone https://github.com/mwrocha/wtcconnecta-app.git
cd wtcconnecta-app
```

### 2. Configurar a URL do backend

Abra o arquivo `app/src/main/java/br/com/fiap/wtcconnecta/data/remote/RetrofitClient.kt` e ajuste o
`BASE_URL` de acordo com o ambiente:

```kotlin
// Emulador Android (backend rodando no mesmo PC)
private const val BASE_URL = "http://10.0.2.2:8080/"

// Dispositivo físico (mesma rede Wi-Fi)
// Descubra o IP: cmd → ipconfig → "Endereço IPv4"
private const val BASE_URL = "http://192.168.X.X:8080/"
```

### 3. Configurar o Firebase

O arquivo `google-services.json` já está incluído em `app/`. Caso precise reconfigurar:

1. Acesse o [Firebase Console](https://console.firebase.google.com)
2. Selecione o projeto **WTC Connecta**
3. Baixe o `google-services.json`
4. Substitua o arquivo em `app/google-services.json`

---

## Como Executar

1. Abra o projeto no **Android Studio**
2. Aguarde a sincronização do Gradle
3. Certifique-se que o backend está rodando
4. Selecione um dispositivo ou emulador
5. Clique em **Run → Run 'app'** ou pressione `Shift + F10`

---

## Gerar APK

```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

O arquivo gerado estará em:

```
app/build/outputs/apk/debug/app-debug.apk
```

---

## Perfis de Usuário

O app opera com dois perfis distintos, cada um com fluxo e telas independentes:

| Perfil     | Acesso                                                                  |
|------------|-------------------------------------------------------------------------|
| `OPERATOR` | Dashboard, atendimentos, clientes, campanhas, grupos, kanban, auditoria |
| `CLIENT`   | Chat, campanhas recebidas, perfil, solicitação de troca de grupo        |

O perfil é determinado automaticamente pelo JWT retornado no login.

---

## Estrutura do Projeto

```
br.com.fiap.wtcconnecta
├── data
│   ├── model/          # Modelos de dados (User, Message, Campaign, Group, ...)
│   ├── remote/         # ApiService (Retrofit) e RetrofitClient
│   └── repository/     # Repositories (Auth, Client, Message, Campaign, ...)
├── service/
│   └── WTCFirebaseMessagingService.kt  # Push notifications via FCM
├── ui
│   ├── components/     # Componentes reutilizáveis (MessageBubble, Banner, ...)
│   ├── navigation/     # NavGraph — rotas e deeplinks
│   ├── screens/
│   │   ├── auth/       # LoginScreen, RegisterScreen
│   │   ├── client/     # HomeClientScreen, ChatScreen, CampaignExpressScreen, ...
│   │   └── operator/   # OperatorDashboardScreen, ClientDetailScreen, KanbanScreen, ...
│   └── theme/          # Cores, tipografia e tema do app
├── viewmodel/          # ViewModels de cada domínio
└── MainActivity.kt
```

---

## Telas Principais

### Perfil Operador

| Tela               | Descrição                                                  |
|--------------------|------------------------------------------------------------|
| Dashboard          | Métricas em tempo real, fila de atendimento, ações rápidas |
| ClientDetailScreen | Chat, anotações, perfil e campanhas do cliente em 4 abas   |
| GroupListScreen    | Grupos e divisões com disparo de mensagens                 |
| CampaignScreen     | Criação, edição e disparo de campanhas segmentadas         |
| KanbanScreen       | Tarefas organizadas em A Fazer / Em Progresso / Concluído  |
| AuditScreen        | Trilha de auditoria de operações críticas                  |

### Perfil Cliente

| Tela                   | Descrição                                  |
|------------------------|--------------------------------------------|
| HomeClientScreen       | Acesso rápido ao chat e campanhas          |
| ConversationListScreen | Lista de conversas com operadores          |
| ChatScreen             | Chat 1:1 com suporte a imagem e PDF        |
| CampaignExpressScreen  | Campanhas recebidas com botões de ação     |
| ProfileScreen          | Perfil com divisão, grupo e dados pessoais |

---

## Deeplinks Suportados

Usados nos botões de ação das campanhas para navegação interna:

| Deeplink                  | Destino           |
|---------------------------|-------------------|
| `wtcconnecta://chat`      | Tela de conversas |
| `wtcconnecta://campaigns` | Campanhas Express |
| `wtcconnecta://profile`   | Perfil do usuário |
| `wtcconnecta://kanban`    | Painel Kanban     |

---

## Push Notifications (FCM)

O app recebe notificações em tempo real para os seguintes eventos:

- Nova mensagem 1:1 recebida
- Campanha disparada pelo operador
- Atendimento encerrado (cliente)
- Novo cliente na fila de espera (operador)
- Solicitação de troca de grupo (operador)

O token FCM é registrado automaticamente no backend após o login e atualizado a cada nova sessão.

---

## Segurança

- Autenticação via **JWT** armazenado em memória na sessão
- **Sessão única** — logar em outro dispositivo invalida o token anterior automaticamente
- Dois perfis com fluxos completamente separados (`OPERATOR` / `CLIENT`)

---

> **Nota:** o backend deve estar rodando e acessível antes de iniciar o app. Consulte o README do
> repositório do backend para instruções de configuração.
