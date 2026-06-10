import 'dart:io';

import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../api/api_client.dart';

/// 백그라운드 메시지 핸들러 — 반드시 최상위 함수여야 한다.
@pragma('vm:entry-point')
Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  // 백그라운드/종료 상태에서는 OS가 알림을 자동 표시한다.
  // 추가 처리가 필요하면 여기서 수행.
}

/// FCM 알림 채널 (Android 8+)
const _drawResultChannel = AndroidNotificationChannel(
  'draw_result',
  '추첨 결과 알림',
  description: '로또 당첨번호 발표 시 알림을 받습니다.',
  importance: Importance.high,
);

class PushNotificationService {
  PushNotificationService._();

  static const _tokenKey = 'fcm_token_v1';
  static final _localNotif = FlutterLocalNotificationsPlugin();

  /// 앱 기동 시 한 번만 호출한다.
  /// Firebase 초기화가 성공한 상태에서만 의미 있다.
  static Future<void> initialize(KraftApiClient apiClient) async {
    await _setupLocalNotifications();

    final messaging = FirebaseMessaging.instance;

    // iOS/Android 알림 권한 요청
    await messaging.requestPermission(
      alert: true,
      badge: true,
      sound: true,
    );

    // Android 13+ 포그라운드 알림 권한
    if (Platform.isAndroid) {
      await _localNotif
          .resolvePlatformSpecificImplementation<
              AndroidFlutterLocalNotificationsPlugin>()
          ?.requestNotificationsPermission();
    }

    // 현재 FCM 토큰 등록
    final token = await messaging.getToken();
    if (token != null) await _upsertToken(token, apiClient);

    // 토큰 갱신 감지 — 앱 실행 중에 토큰이 바뀌면 자동 재등록
    messaging.onTokenRefresh.listen((newToken) async {
      await _upsertToken(newToken, apiClient);
    });

    // 포그라운드 메시지 → flutter_local_notifications로 직접 표시
    FirebaseMessaging.onMessage.listen(_showForegroundNotification);
  }

  // ── 내부 헬퍼 ────────────────────────────────────────────────────────────

  static Future<void> _setupLocalNotifications() async {
    const androidInit = AndroidInitializationSettings('@mipmap/ic_launcher');
    const iosInit = DarwinInitializationSettings();
    await _localNotif.initialize(
      const InitializationSettings(android: androidInit, iOS: iosInit),
    );

    if (Platform.isAndroid) {
      await _localNotif
          .resolvePlatformSpecificImplementation<
              AndroidFlutterLocalNotificationsPlugin>()
          ?.createNotificationChannel(_drawResultChannel);
    }
  }

  /// 이전에 등록한 토큰과 다를 때만 백엔드에 등록 요청을 보낸다.
  static Future<void> _upsertToken(
      String token, KraftApiClient apiClient) async {
    const storage = FlutterSecureStorage();
    final prev = await storage.read(key: _tokenKey);
    if (prev == token) return;

    try {
      final platform = Platform.isAndroid ? 'ANDROID' : 'IOS';
      await apiClient.registerPushToken(token, platform);
      await storage.write(key: _tokenKey, value: token);
    } catch (_) {
      // 네트워크 오류 시 무시 — 다음 실행 때 재시도
    }
  }

  static Future<void> _showForegroundNotification(
      RemoteMessage message) async {
    final notif = message.notification;
    if (notif == null) return;

    await _localNotif.show(
      notif.hashCode,
      notif.title,
      notif.body,
      NotificationDetails(
        android: AndroidNotificationDetails(
          _drawResultChannel.id,
          _drawResultChannel.name,
          channelDescription: _drawResultChannel.description,
          importance: _drawResultChannel.importance,
          icon: '@mipmap/ic_launcher',
        ),
        iOS: const DarwinNotificationDetails(),
      ),
    );
  }
}
