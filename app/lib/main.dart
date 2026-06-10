import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/api/api_client.dart';
import 'core/push/push_notification_service.dart';
import 'core/router/app_router.dart';
import 'core/theme/app_theme.dart';
import 'firebase_options.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  try {
    await Firebase.initializeApp(
      options: DefaultFirebaseOptions.currentPlatform,
    );
    // 백그라운드 메시지 핸들러는 Firebase 초기화 직후에 등록해야 한다.
    FirebaseMessaging.onBackgroundMessage(firebaseMessagingBackgroundHandler);
  } catch (_) {
    // firebase_options.dart가 플레이스홀더이거나 네트워크 문제 시 FCM 없이 기동
  }

  runApp(const ProviderScope(child: KraftLottoApp()));
}

class KraftLottoApp extends ConsumerStatefulWidget {
  const KraftLottoApp({super.key});

  @override
  ConsumerState<KraftLottoApp> createState() => _KraftLottoAppState();
}

class _KraftLottoAppState extends ConsumerState<KraftLottoApp> {
  bool _fcmInitialized = false;

  @override
  void initState() {
    super.initState();
    // build 이후 첫 프레임에 FCM 초기화 — ref.read 사용
    WidgetsBinding.instance.addPostFrameCallback((_) => _initFcm());
  }

  Future<void> _initFcm() async {
    if (_fcmInitialized) return;
    _fcmInitialized = true;
    try {
      final client = ref.read(kraftApiClientProvider);
      await PushNotificationService.initialize(client);
    } catch (_) {
      // FCM 초기화 실패는 앱 동작에 영향을 주지 않는다
    }
  }

  @override
  Widget build(BuildContext context) {
    final router = ref.watch(appRouterProvider);
    return MaterialApp.router(
      title: 'KRAFT Lotto',
      theme: AppTheme.light,
      darkTheme: AppTheme.dark,
      routerConfig: router,
      debugShowCheckedModeBanner: false,
    );
  }
}
