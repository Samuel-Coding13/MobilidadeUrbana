🚍 Mobilidade Urbana
Aplicativo Android de rastreamento em tempo real para mobilidade urbana, desenvolvido com Jetpack Compose e Firebase.

📱 Funcionalidades
✅ Autenticação completa com Firebase (Login, Cadastro, Recuperação de senha)
✅ Rastreamento em tempo real com GPS
✅ Mapas interativos usando OpenStreetMap (OSMDroid)
✅ Verificação de email obrigatória
✅ Splash screen com verificação de internet
✅ Perfil de usuário com informações
✅ Design moderno com cores azuis e Material Design 3
✅ Navegação drawer lateral
🎨 Design
O aplicativo utiliza uma paleta de cores azuis moderna:

Azul Principal: 
#0066FF
Azul Claro: 
#00D4FF
Azul Escuro: 
#003366
Fundo: 
#F0F7FF
🛠️ Tecnologias
Kotlin - Linguagem principal
Jetpack Compose - UI moderna e declarativa
Firebase Auth - Autenticação de usuários
Firebase Firestore - Banco de dados NoSQL
OSMDroid - Mapas OpenStreetMap
Google Play Services Location - Serviços de localização
Navigation Compose - Navegação entre telas
Material Design 3 - Componentes modernos
📂 Estrutura do Projeto
app/
├── src/main/
│   ├── java/com/example/mobilidadeurbana/
│   │   ├── MainActivity.kt              # Activity principal
│   │   ├── MyApp.kt                     # Application class
│   │   ├── Navigation.kt                # Configuração de rotas
│   │   ├── Utils.kt                     # Funções utilitárias
│   │   ├── view/
│   │   │   ├── TelaDeCadastro.kt       # Tela de cadastro
│   │   │   ├── TelaDeLogin.kt          # Tela de login
│   │   │   ├── TelaHome.kt             # Tela principal com mapa
│   │   │   └── TelaPerfil.kt           # Tela de perfil
│   │   ├── viewmodel/
│   │   │   └── AuthViewModel.kt        # ViewModel de autenticação
│   │   └── util/
│   │       └── BlinkText.kt            # Componente de texto piscante
│   ├── res/
│   │   ├── drawable/
│   │   │   ├── outline_bus_alert_24.xml    # Ícone de ônibus
│   │   │   └── outline_pin_drop_24.xml     # Ícone de pin
│   │   ├── values/
│   │   │   ├── colors.xml              # Paleta de cores
│   │   │   ├── strings.xml             # Strings do app
│   │   │   └── themes.xml              # Temas
│   │   └── xml/
│   │       ├── backup_rules.xml
│   │       └── data_extraction_rules.xml
│   └── AndroidManifest.xml             # Configurações e permissões
└── build.gradle.kts                     # Dependências
🚀 Como Executar
Pré-requisitos
Android Studio Hedgehog ou superior
JDK 8 ou superior
Conta Firebase configurada
Passos
Clone o repositório
bash
   git clone https://github.com/Samuel-Coding13/MobilidadeUrbana.git
   cd MobilidadeUrbana
Configure o Firebase
Crie um projeto no Firebase Console
Adicione um app Android com o package name com.example.mobilidadeurbana
Baixe o arquivo google-services.json
Coloque o arquivo na pasta app/
Habilite Authentication (Email/Password)
Habilite Firestore Database
Configure as permissões
As permissões já estão configuradas no AndroidManifest.xml
Certifique-se de aceitar as permissões de localização no dispositivo
Execute o aplicativo
Abra o projeto no Android Studio
Sincronize o Gradle
Execute em um dispositivo físico ou emulador (API 24+)
📱 Telas
1. Splash Screen
Verifica conexão com internet
Mostra animação de loading
Redireciona para Login ou Home
2. Login
Campo de email e senha
Validação de formato de email
Opção "Esqueci a senha"
Link para cadastro
3. Cadastro
Campos: nome, email, senha, confirmar senha
Validação de senhas
Envio de email de verificação
Armazena dados no Firestore
4. Home (Mapa)
Mapa interativo com OSMDroid
Botão de play/stop para rastreamento
Marcador de localização do usuário
Menu drawer lateral
TopBar com título
5. Perfil
Exibe nome, email e UID
Status de verificação de email
Botão para voltar
🔒 Segurança
Senhas armazenadas com hash no Firebase Auth
Email de verificação obrigatório
Validação de formulários
Proteção contra SQL injection (Firestore)
📝 Permissões Necessárias
INTERNET - Para acessar Firebase e mapas
ACCESS_NETWORK_STATE - Verificar conexão
ACCESS_FINE_LOCATION - GPS preciso
ACCESS_COARSE_LOCATION - Localização aproximada
WRITE_EXTERNAL_STORAGE - Cache de mapas (Android < 13)
🐛 Troubleshooting
Problema: Mapa não carrega
Solução: Verifique se tem internet e se as permissões de localização foram concedidas.

Problema: Login não funciona
Solução: Certifique-se de que o Firebase Auth está habilitado e o email foi verificado.

Problema: App crasha ao abrir
Solução: Verifique se o arquivo google-services.json está na pasta app/.

🤝 Contribuindo
Fork o projeto
Crie uma branch para sua feature (git checkout -b feature/MinhaFeature)
Commit suas mudanças (git commit -m 'Adiciona MinhaFeature')
Push para a branch (git push origin feature/MinhaFeature)
Abra um Pull Request
📄 Licença
Este projeto está sob a licença MIT. Veja o arquivo LICENSE para mais detalhes.

👥 Autor
Samuel - GitHub

🎓 Projeto Acadêmico
Este é um projeto de TCC (Trabalho de Conclusão de Curso) desenvolvido com fins educacionais.

⭐ Se este projeto te ajudou, considere dar uma estrela no GitHub!

